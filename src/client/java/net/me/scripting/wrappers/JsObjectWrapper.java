package net.me.scripting.wrappers;

import net.me.scripting.utils.ScriptUtils;
import net.me.scripting.wrappers.support.FieldLookup;
import net.me.scripting.wrappers.support.MethodLookup;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class JsObjectWrapper implements ProxyObject {
    private final Object javaInstance;
    private final Class<?> instanceClass;
    private final MethodLookup methods;
    private final FieldLookup fields;

    public JsObjectWrapper(Object instance,
                           Class<?> cls,
                           Map<String, List<String>> methodMap,
                           Map<String, String> fieldMap) {
        if (instance == null) {
            throw new NullPointerException("Java instance cannot be null");
        }
        this.javaInstance = instance;
        this.instanceClass = (cls != null) ? cls : instance.getClass();
        this.methods = new MethodLookup(methodMap);
        this.fields = new FieldLookup(fieldMap);
    }

    @Override
    public Object getMember(String key) {
        if ("_self".equals(key)) return this.getJavaInstance();
        if (key.endsWith("$")) return handleField(key.substring(0, key.length() - 1));
        Object mapped = handleMappedMethod(key);
        if (mapped != null) return mapped;
        Object direct = handleDirectMethod(key);
        if (direct != null) return direct;
        return handleField(key);
    }

    @Override
    public boolean hasMember(String key) {
        if ("_self".equals(key)) return true;
        if (key.endsWith("$")) return fields.hasField(instanceClass, key.substring(0, key.length() - 1));
        return methods.hasMapped(key) || MethodLookup.hasDirect(instanceClass, key) || fields.hasField(instanceClass, key);
    }

    @Override
    public Object getMemberKeys() {
        Set<String> keys = new HashSet<>(methods.methodKeys());
        keys.addAll(fields.fieldKeys());
        for (Method method : instanceClass.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) keys.add(method.getName());
        }
        for (Field field : instanceClass.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) keys.add(field.getName());
        }
        keys.add("_self");
        return keys.toArray(new String[0]);
    }

    @Override
    public void putMember(String key, Value value) {
        String fieldName = key;
        boolean isExplicitFieldAccess = false;
        if (key.endsWith("$")) {
            fieldName = key.substring(0, key.length() - 1);
            isExplicitFieldAccess = true;
        }
        if (fields.hasField(instanceClass, fieldName)) {
            boolean methodConflict = methods.hasMapped(fieldName) || MethodLookup.hasDirect(instanceClass, fieldName);
            if (methodConflict && !isExplicitFieldAccess) {
                throw new UnsupportedOperationException(
                        "Ambiguous write to '" + fieldName + "'. A method with this name exists. " +
                                "Use the '$' suffix to write to the field directly: " + fieldName + "$"
                );
            }
            writeField(fieldName, value);
            return;
        }
        throw new UnsupportedOperationException("No writable member: " + key);
    }

    private Object handleMappedMethod(String key) {
        List<Method> candidates = methods.findMethods(instanceClass, key);
        if (!candidates.isEmpty()) return (ProxyExecutable) args -> invokeMethods(candidates, args,key);
        return null;
    }

    private Object handleDirectMethod(String key) {
        List<Method> direct = MethodLookup.findDirect(instanceClass, key);
        if (!direct.isEmpty()) return (ProxyExecutable) args -> invokeMethods(direct, args,key);
        return null;
    }

    private Object handleField(String key) {
        try {
            Field f = fields.accessField(instanceClass, key);
            if (Modifier.isStatic(f.getModifiers())) return null;
            return ScriptUtils.wrapReturn(f.get(javaInstance));
        } catch (NoSuchFieldException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Field access failed: " + key, e);
        }
    }

    private Object invokeMethods(List<Method> methods, Value[] args, String yarnName) {
        for (Method m : methods) {
            if (m.getParameterCount() == args.length) {
                try {
                    Object[] javaArgs = ScriptUtils.unwrapArgs(args, m.getParameterTypes()); // works

                    Object result = m.invoke(this.javaInstance, javaArgs);
                    return ScriptUtils.wrapReturn(result);
                } catch (Exception e) {
                    if (e.getCause() != null) {
                        throw new RuntimeException("Method '" + yarnName + "' threw an exception: " + e.getCause().getMessage(), e.getCause());
                    }
                    throw new RuntimeException("Method invocation failed for '" + yarnName + "'. See logs for details.", e);
                }
            }
        }
        throw new RuntimeException("No overload for method '" + yarnName + "' with " + args.length + " args");
    }

    private void writeField(String key, Value value) {
        try {
            Field f = fields.accessField(instanceClass, key);
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers()))
                throw new UnsupportedOperationException("Cannot modify field: " + key);
            Object javaVal = ScriptUtils.unwrapArgs(new Value[]{value}, new Class[]{f.getType()})[0];
            f.set(javaInstance, javaVal);
        } catch (Exception e) {
            throw new RuntimeException("Field write failed: " + key, e);
        }
    }

    public Object getJavaInstance() {
        return this.javaInstance;
    }
}
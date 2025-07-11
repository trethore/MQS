package net.me.scripting.wrappers;

import net.me.scripting.ScriptManager;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.utils.FastAccessorUtils;
import net.me.scripting.utils.ScriptUtils;
import net.me.scripting.wrappers.support.FieldLookup;
import net.me.scripting.wrappers.support.MethodLookup;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JsObjectWrapper implements ProxyObject {
    private static final Map<Class<?>, String[]> MEMBER_KEYS_CACHE = new ConcurrentHashMap<>();
    private final Object javaInstance;
    private final Class<?> instanceClass;
    private final MethodLookup methods;
    private final FieldLookup fields;
    private final String[] memberKeys;

    // Dependencies
    private final MappingsManager mappingsManager;
    private final ScriptManager scriptManager;

    public JsObjectWrapper(Object instance,
                           Class<?> cls,
                           Map<String, List<String>> methodMap,
                           Map<String, String> fieldMap,
                           MappingsManager mappingsManager,
                           ScriptManager scriptManager) {
        if (instance == null) {
            throw new NullPointerException("Java instance cannot be null");
        }
        this.javaInstance = instance;
        this.instanceClass = (cls != null) ? cls : instance.getClass();
        this.methods = new MethodLookup(methodMap);
        this.fields = new FieldLookup(fieldMap);
        this.mappingsManager = mappingsManager;
        this.scriptManager = scriptManager;

        this.memberKeys = MEMBER_KEYS_CACHE.computeIfAbsent(this.instanceClass, c -> {
            Set<String> keys = new HashSet<>(methods.methodKeys());
            keys.addAll(fields.fieldKeys());

            for (Method method : c.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    keys.add(method.getName());
                }
            }
            for (Field field : c.getFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    keys.add(field.getName());
                }
            }
            keys.add("_self");
            keys.add("equals");
            return keys.toArray(new String[0]);
        });
    }

    @Override
    public Object getMember(String key) {
        if ("equals".equals(key)) {
            return (ProxyExecutable) (Value... args) -> {
                if (args.length != 1) return false;
                Object otherRaw = ScriptUtils.unwrapReceiver(args[0]);
                return this.javaInstance.equals(otherRaw);
            };
        }

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
        if ("_self".equals(key) || "equals".equals(key)) return true;
        if (key.endsWith("$")) return fields.hasField(instanceClass, key.substring(0, key.length() - 1));
        return methods.hasMapped(key) || MethodLookup.hasDirect(instanceClass, key) || fields.hasField(instanceClass, key);
    }

    @Override
    public Object getMemberKeys() {
        return this.memberKeys;
    }

    @Override
    public void putMember(String key, Value value) {
        if ("equals".equals(key)) {
            throw new UnsupportedOperationException("Cannot override the built-in 'equals' method.");
        }
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
        if (!candidates.isEmpty()) return (ProxyExecutable) args -> invokeMethods(candidates, args, key);
        return null;
    }

    private Object handleDirectMethod(String key) {
        List<Method> direct = MethodLookup.findDirect(instanceClass, key);
        if (!direct.isEmpty()) return (ProxyExecutable) args -> invokeMethods(direct, args, key);
        return null;
    }

    private Object handleField(String key) {
        try {
            Field f = fields.accessField(instanceClass, key);
            if (Modifier.isStatic(f.getModifiers())) return null;
            MethodHandle getter = FastAccessorUtils.getFieldGetter(f);
            Object result = getter.bindTo(javaInstance).invoke();
            return ScriptUtils.wrapReturn(result, this.mappingsManager, this.scriptManager);
        } catch (NoSuchFieldException e) {
            return null;
        } catch (Throwable e) {
            throw new RuntimeException("Field access failed: " + key, e);
        }
    }

    private Object invokeMethods(List<Method> methods, Value[] args, String yarnName) {
        for (Method m : methods) {
            if (m.getParameterCount() == args.length) {
                try {
                    Object[] javaArgs = ScriptUtils.unwrapArgs(args, m.getParameterTypes());
                    MethodHandle handle = FastAccessorUtils.getMethodHandle(m);
                    Object result = handle.bindTo(this.javaInstance).invokeWithArguments(javaArgs);
                    return ScriptUtils.wrapReturn(result, this.mappingsManager, this.scriptManager);
                } catch (Throwable e) {
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
            MethodHandle setter = FastAccessorUtils.getFieldSetter(f);
            setter.bindTo(javaInstance).invoke(javaVal);
        } catch (Throwable e) {
            throw new RuntimeException("Field write failed: " + key, e);
        }
    }

    public Object getJavaInstance() {
        return this.javaInstance;
    }

    @Override
    public String toString() {
        return String.format("[MQS Wrapper: %s]", this.instanceClass.getName());
    }
}
package net.me.scripting;

import net.me.Main;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsExtendedObjectWrapper implements ProxyObject {
    private final Object javaInstance;
    private final Class<?> instanceClass;
    private final SuperAccessWrapper superAccessor;
    private final Map<String, List<String>> methodMap;
    private final Map<String, String> fieldMap;
    private final Value jsOverrides;

    public JsExtendedObjectWrapper(Object javaInstance,
                                   Class<?> instanceClass,
                                   Map<String, List<String>> methods,
                                   Map<String, String> fields,
                                   Value jsOverrides) {
        if (javaInstance == null) {
            throw new NullPointerException("Java instance cannot be null");
        }
        this.javaInstance = javaInstance;
        this.instanceClass = instanceClass;
        this.methodMap = Collections.unmodifiableMap(methods);
        this.fieldMap = Collections.unmodifiableMap(fields);
        this.jsOverrides = jsOverrides;
        this.superAccessor = new SuperAccessWrapper(javaInstance, instanceClass, methodMap, fieldMap);
    }

    @Override
    public Object getMember(String key) {
        if ("_super".equals(key)) {
            return superAccessor;
        }
        Object override = handleJsOverride(key);
        if (override != null) return override;
        Object methodProxy = handleMappedMethod(key);
        if (methodProxy != null) return methodProxy;
        return handleField(key);
    }

    @Override
    public boolean hasMember(String key) {
        if ("_super".equals(key)) return true;
        if (jsOverrides != null && jsOverrides.hasMember(key)) return true;
        return methodMap.containsKey(key) || fieldMap.containsKey(key);
    }

    @Override
    public Object getMemberKeys() {
        Set<String> keys = new HashSet<>();
        keys.add("_super");
        if (jsOverrides != null) {
            keys.addAll(jsOverrides.getMemberKeys());
        }
        keys.addAll(methodMap.keySet());
        keys.addAll(fieldMap.keySet());
        return keys.toArray(new String[0]);
    }

    @Override
    public void putMember(String key, Value value) {
        if (fieldMap.containsKey(key)) {
            writeField(key, value);
            return;
        }
        throw new UnsupportedOperationException("Cannot set member: " + key);
    }

    private Object handleJsOverride(String key) {
        if (jsOverrides != null && jsOverrides.hasMember(key)) {
            Value jsVal = jsOverrides.getMember(key);
            if (jsVal.canExecute()) {
                return (ProxyExecutable) args -> invokeJsFunction(jsVal, args);
            }
            return ScriptUtils.wrapReturn(jsVal);
        }
        return null;
    }

    private Object invokeJsFunction(Value fn, Value[] args) {
        try {
            Value[] callArgs = new Value[args.length + 1];
            callArgs[0] = Value.asValue(this);
            System.arraycopy(args, 0, callArgs, 1, args.length);
            Value result = fn.invokeMember("call", (Object[]) callArgs);
            return ScriptUtils.wrapReturn(result);
        } catch (PolyglotException e) {
            Main.LOGGER.error("JavaScript error in function call: {}", e.getMessage());
            throw e;
        }
    }

    private Object handleMappedMethod(String key) {
        List<String> runtimeNames = methodMap.get(key);
        if (runtimeNames != null) {
            List<Method> methods = ScriptUtils.findMethods(instanceClass, runtimeNames, false);
            return (ProxyExecutable) args -> invokeMethods(methods, args);
        }
        return null;
    }

    private Object handleField(String key) {
        String runtime = fieldMap.get(key);
        if (runtime == null) return null;
        try {
            Field f = ScriptUtils.findField(instanceClass, runtime);
            if (Modifier.isStatic(f.getModifiers())) return null;
            return ScriptUtils.wrapReturn(f.get(javaInstance));
        } catch (Exception e) {
            throw new RuntimeException("Field access failed: " + key, e);
        }
    }

    private Object invokeMethods(List<Method> methods, Value[] args) {
        int count = args.length;
        for (Method m : methods) {
            if (m.getParameterCount() == count) {
                try {
                    Object[] javaArgs = ScriptUtils.unwrapArgs(args, m.getParameterTypes());
                    Object res = m.invoke(javaInstance, javaArgs);
                    return ScriptUtils.wrapReturn(res);
                } catch (Exception e) {
                    throw new RuntimeException("Method invocation failed: " + m.getName(), e);
                }
            }
        }
        throw new RuntimeException("No overload for method with " + count + " args");
    }

    private void writeField(String key, Value value) {
        String runtime = fieldMap.get(key);
        try {
            Field f = ScriptUtils.findField(instanceClass, runtime);
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) {
                throw new UnsupportedOperationException("Cannot modify field: " + key);
            }
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

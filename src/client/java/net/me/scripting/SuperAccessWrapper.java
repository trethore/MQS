package net.me.scripting;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SuperAccessWrapper implements ProxyObject {
    private final Object javaInstance;
    private final Class<?> instanceClass;
    private final Map<String, List<String>> methodMap;
    private final Map<String, String> fieldMap;

    public SuperAccessWrapper(Object javaInstance,
                              Class<?> instanceClass,
                              Map<String, List<String>> methods,
                              Map<String, String> fields) {
        this.javaInstance = javaInstance;
        this.instanceClass = instanceClass;
        this.methodMap = methods;
        this.fieldMap = fields;
    }

    @Override
    public Object getMember(String key) {
        if (methodMap.containsKey(key)) {
            List<Method> methods = ScriptUtils.findMethods(instanceClass, methodMap.get(key), false);
            return (ProxyExecutable) args -> invokeMethods(methods, args);
        }
        if (fieldMap.containsKey(key)) {
            String runtimeName = fieldMap.get(key);
            try {
                Field f = ScriptUtils.findField(instanceClass, runtimeName);
                if (Modifier.isStatic(f.getModifiers())) {
                    throw new RuntimeException("Cannot access static field via super: " + key);
                }
                Object val = f.get(javaInstance);
                return ScriptUtils.wrapReturn(val);
            } catch (Exception e) {
                throw new RuntimeException("Super field access failed: " + key, e);
            }
        }
        return null;
    }

    @Override
    public boolean hasMember(String key) {
        return methodMap.containsKey(key) || fieldMap.containsKey(key);
    }

    @Override
    public Object getMemberKeys() {
        return Stream.concat(
                methodMap.keySet().stream(),
                fieldMap.keySet().stream()
        ).toArray(String[]::new);
    }

    @Override
    public void putMember(String key, Value value) {
        if (fieldMap.containsKey(key)) {
            String runtimeName = fieldMap.get(key);
            try {
                Field f = ScriptUtils.findField(instanceClass, runtimeName);
                if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) {
                    throw new UnsupportedOperationException("Cannot set field via super: " + key);
                }
                Object javaVal = ScriptUtils.unwrapArgs(new Value[]{value}, new Class[]{f.getType()})[0];
                f.set(javaInstance, javaVal);
                return;
            } catch (Exception e) {
                throw new RuntimeException("Super field write failed: " + key, e);
            }
        }
        throw new UnsupportedOperationException("No writable super member: " + key);
    }

    private Object invokeMethods(List<Method> methods, Value[] args) {
        for (Method m : methods) {
            if (m.getParameterCount() == args.length) {
                try {
                    Object[] javaArgs = ScriptUtils.unwrapArgs(args, m.getParameterTypes());
                    Object result = m.invoke(javaInstance, javaArgs);
                    return ScriptUtils.wrapReturn(result);
                } catch (Exception e) {
                    throw new RuntimeException("Super method invocation failed: " + m.getName(), e);
                }
            }
        }
        throw new RuntimeException("No matching super-method overload for args count: " + args.length);
    }
}

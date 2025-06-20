package net.me.scripting.wrappers;

import net.me.Main;
import net.me.scripting.utils.ReflectionUtils;
import net.me.scripting.utils.ScriptUtils;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyInstantiable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JsClassWrapper implements ProxyObject, ProxyInstantiable {
    private final Class<?> targetClass;
    private final String targetClassName;
    private final Map<String, List<String>> yarnToRuntimeMethods;
    private final Map<String, String> yarnToRuntimeFields;
    private final List<Constructor<?>> constructors;

    public JsClassWrapper(String runtimeFqcn,
                          Map<String, List<String>> methodLookup,
                          Map<String, String> fieldLookup
    ) throws ClassNotFoundException {
        Main.LOGGER.debug("Creating JsClassWrapper for: {}", runtimeFqcn);
        this.targetClass = Class.forName(runtimeFqcn);
        this.targetClassName = targetClass.getName();
        this.yarnToRuntimeMethods = Map.copyOf(methodLookup);
        this.yarnToRuntimeFields = Map.copyOf(fieldLookup);
        this.constructors = List.of(targetClass.getConstructors());
        this.constructors.forEach(c -> c.setAccessible(true));
    }

    @Override
    public Object newInstance(Value... args) {
        return invokeConstructor(args);
    }

    @Override
    public Object getMember(String key) {
        if ("_class".equals(key)) {
            return targetClass;
        }

        if (key.endsWith("$")) {
            String fieldName = key.substring(0, key.length() - 1);
            if (yarnToRuntimeFields.containsKey(fieldName)) {
                return readStaticField(fieldName);
            }
            return null;
        }

        if (yarnToRuntimeMethods.containsKey(key)) {
            return createStaticMethodProxy(key);
        }
        if (yarnToRuntimeFields.containsKey(key)) {
            return readStaticField(key);
        }

        return null;
    }

    @Override
    public boolean hasMember(String key) {
        if ("_class".equals(key)) return true;
        if (key.endsWith("$")) {
            return yarnToRuntimeFields.containsKey(key.substring(0, key.length() - 1));
        }
        return yarnToRuntimeMethods.containsKey(key) || yarnToRuntimeFields.containsKey(key);
    }

    @Override
    public Object getMemberKeys() {
        Set<String> keys = new LinkedHashSet<>();
        keys.add("_class");
        keys.addAll(yarnToRuntimeMethods.keySet());
        keys.addAll(yarnToRuntimeFields.keySet());
        yarnToRuntimeFields.keySet().forEach(field -> keys.add(field + "$"));
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

        if (yarnToRuntimeFields.containsKey(fieldName)) {
            boolean methodConflict = yarnToRuntimeMethods.containsKey(fieldName);
            if (methodConflict && !isExplicitFieldAccess) {
                throw new UnsupportedOperationException(
                        "Ambiguous write to static member '" + fieldName + "'. A static method with this name exists. " +
                                "Use the '$' suffix to write to the field directly: " + fieldName + "$"
                );
            }
            writeStaticField(fieldName, value);
            return;
        }
        throw new UnsupportedOperationException("No writable static member: " + key);
    }

    private Object invokeConstructor(Value[] polyglotArgs) {
        int argCount = polyglotArgs.length;
        for (Constructor<?> ctor : constructors) {
            if (ctor.getParameterCount() == argCount) {
                try {
                    Object[] javaArgs = ScriptUtils.unwrapArgs(polyglotArgs, ctor.getParameterTypes());
                    Object instance = ctor.newInstance(javaArgs);
                    return ScriptUtils.wrapReturn(instance);
                } catch (Exception e) {
                    throw new RuntimeException(
                            String.format("Failed to instantiate %s: %s", targetClassName, e.getMessage()), e);
                }
            }
        }
        String available = constructors.stream()
                .map(c -> c.getParameterCount() + " args")
                .distinct()
                .collect(Collectors.joining(", "));
        throw new RuntimeException(
                String.format("No constructor for %s with %d args. Available: [%s]",
                        targetClassName, argCount, available));
    }

    private ProxyExecutable createStaticMethodProxy(String yarnKey) {
        List<String> runtimeNames = yarnToRuntimeMethods.get(yarnKey);
        List<Method> methods = ReflectionUtils.findMethods(targetClass, runtimeNames, true);
        return polyglotArgs -> {
            int argCount = polyglotArgs.length;
            for (Method m : methods) {
                if (m.getParameterCount() == argCount) {
                    try {
                        Object[] javaArgs = ScriptUtils.unwrapArgs(polyglotArgs, m.getParameterTypes());
                        Object result = m.invoke(null, javaArgs);
                        return ScriptUtils.wrapReturn(result);
                    } catch (Exception e) {
                        throw new RuntimeException(
                                String.format("Failed to instantiate %s: %s", targetClassName, e.getMessage()), e);
                    }
                }
            }
            throw new RuntimeException(
                    String.format("No static overload for %s.%s with %d args", targetClassName, yarnKey, argCount));
        };
    }

    private Object readStaticField(String yarnKey) {
        String runtimeName = yarnToRuntimeFields.get(yarnKey);
        try {
            Field f = ReflectionUtils.findField(targetClass, runtimeName);
            if (!Modifier.isStatic(f.getModifiers())) {
                throw new RuntimeException(yarnKey + " is not a static field.");
            }
            return ScriptUtils.wrapReturn(f.get(null));
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Error accessing static field %s.%s: %s", targetClassName, yarnKey, e.getMessage()), e);
        }
    }

    private void writeStaticField(String yarnKey, Value value) {
        String runtimeName = yarnToRuntimeFields.get(yarnKey);
        try {
            Field f = ReflectionUtils.findField(targetClass, runtimeName);
            if (!Modifier.isStatic(f.getModifiers())) {
                throw new UnsupportedOperationException("Cannot write to non-static field '" + yarnKey + "' via class proxy.");
            }
            if (Modifier.isFinal(f.getModifiers())) {
                throw new UnsupportedOperationException("Cannot modify final static field '" + yarnKey + "'.");
            }
            Object javaVal = ScriptUtils.unwrapArgs(new Value[]{value}, new Class[]{f.getType()})[0];
            f.set(null, javaVal);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Error setting static field %s.%s: %s", targetClassName, yarnKey, e.getMessage()), e);
        }
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public Map<String, List<String>> getMethodMappings() {
        return yarnToRuntimeMethods;
    }

    public Map<String, String> getFieldMappings() {
        return yarnToRuntimeFields;
    }

}
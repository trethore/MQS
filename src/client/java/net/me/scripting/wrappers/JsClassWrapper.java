/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.me.scripting.wrappers;

import net.me.Main;
import net.me.scripting.ScriptManager;
import net.me.scripting.WrapperConstants;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.utils.FastAccessorUtils;
import net.me.scripting.utils.ReflectionUtils;
import net.me.scripting.utils.ScriptUtils;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyInstantiable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
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
    private final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private final MappingsManager mappingsManager;
    private final ScriptManager scriptManager;

    public JsClassWrapper(String runtimeFqcn,
                          Map<String, List<String>> methodLookup,
                          Map<String, String> fieldLookup,
                          MappingsManager mappingsManager,
                          ScriptManager scriptManager
    ) throws ClassNotFoundException {
        Main.LOGGER.debug("Creating JsClassWrapper for: {}", runtimeFqcn);
        this.targetClass = Class.forName(runtimeFqcn);
        this.targetClassName = targetClass.getName();
        this.yarnToRuntimeMethods = Map.copyOf(methodLookup);
        this.yarnToRuntimeFields = Map.copyOf(fieldLookup);
        this.constructors = List.of(targetClass.getConstructors());
        this.constructors.forEach(c -> c.setAccessible(true));

        this.mappingsManager = mappingsManager;
        this.scriptManager = scriptManager;
    }

    @Override
    public Object newInstance(Value... args) {
        return invokeConstructor(args);
    }

    @Override
    public Object getMember(String key) {
        if (WrapperConstants.CLASS.equals(key)) {
            return targetClass;
        }

        if (key.endsWith(WrapperConstants.FIELD_SUFFIX)) {
            String fieldName = key.substring(0, key.length() - 1);
            if (yarnToRuntimeFields.containsKey(fieldName)) {
                return readStaticField(fieldName);
            }
        }

        if (yarnToRuntimeMethods.containsKey(key)) {
            return createStaticMethodProxyFromYarnKey(key);
        }

        List<Method> directMethods = ReflectionUtils.findMethods(targetClass, List.of(key), true);
        if (!directMethods.isEmpty()) {
            return createStaticMethodProxyFromMethods(directMethods, key);
        }

        if (yarnToRuntimeFields.containsKey(key)) {
            return readStaticField(key);
        }

        return null;
    }

    @Override
    public boolean hasMember(String key) {
        if (WrapperConstants.CLASS.equals(key)) return true;
        if (key.endsWith(WrapperConstants.FIELD_SUFFIX)) {
            return yarnToRuntimeFields.containsKey(key.substring(0, key.length() - 1));
        }
        return yarnToRuntimeMethods.containsKey(key)
                || yarnToRuntimeFields.containsKey(key)
                || !ReflectionUtils.findMethods(targetClass, List.of(key), true).isEmpty();
    }

    @Override
    public Object getMemberKeys() {
        Set<String> keys = new LinkedHashSet<>();
        keys.add(WrapperConstants.CLASS);
        keys.addAll(yarnToRuntimeMethods.keySet());
        keys.addAll(yarnToRuntimeFields.keySet());
        yarnToRuntimeFields.keySet().forEach(field -> keys.add(field + WrapperConstants.FIELD_SUFFIX));
        for (Method m : targetClass.getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                keys.add(m.getName());
            }
        }
        return keys.toArray(new String[0]);
    }

    @Override
    public void putMember(String key, Value value) {
        String fieldName = key;
        boolean isExplicitFieldAccess = false;
        if (key.endsWith(WrapperConstants.FIELD_SUFFIX)) {
            fieldName = key.substring(0, key.length() - 1);
            isExplicitFieldAccess = true;
        }

        if (yarnToRuntimeFields.containsKey(fieldName)) {
            boolean methodConflict = yarnToRuntimeMethods.containsKey(fieldName);
            if (methodConflict && !isExplicitFieldAccess) {
                throw new UnsupportedOperationException(
                        "Ambiguous write to static member '" + fieldName + "'. A static method with this name exists. " +
                                "Use the '$' suffix to write to the field directly: " + fieldName + WrapperConstants.FIELD_SUFFIX
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
                    MethodHandle handle = lookup.unreflectConstructor(ctor);
                    Object instance = handle.invokeWithArguments(javaArgs);
                    return ScriptUtils.wrapReturn(instance, this.mappingsManager, this.scriptManager);
                } catch (Throwable e) {
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

    private ProxyExecutable createStaticMethodProxyFromMethods(List<Method> methods, String methodNameForErrors) {
        return polyglotArgs -> {
            int argCount = polyglotArgs.length;
            for (Method m : methods) {
                if (m.getParameterCount() == argCount) {
                    try {
                        Object[] javaArgs = ScriptUtils.unwrapArgs(polyglotArgs, m.getParameterTypes());
                        MethodHandle handle = FastAccessorUtils.getMethodHandle(m);
                        Object result = handle.invokeWithArguments(javaArgs);
                        return ScriptUtils.wrapReturn(result, this.mappingsManager, this.scriptManager);
                    } catch (Throwable e) {
                        if (e.getCause() != null) {
                            throw new RuntimeException(String.format("Static method %s.%s threw an exception: %s", targetClassName, methodNameForErrors, e.getCause().getMessage()), e.getCause());
                        }
                        throw new RuntimeException(String.format("Method invocation failed for static method %s.%s. See logs for details.", targetClassName, methodNameForErrors), e);
                    }
                }
            }
            throw new RuntimeException(
                    String.format("No static overload for %s.%s with %d args", targetClassName, methodNameForErrors, argCount));
        };
    }

    private ProxyExecutable createStaticMethodProxyFromYarnKey(String yarnKey) {
        List<String> runtimeNames = yarnToRuntimeMethods.get(yarnKey);
        List<Method> methods = ReflectionUtils.findMethods(targetClass, runtimeNames, true);
        return createStaticMethodProxyFromMethods(methods, yarnKey);
    }

    private Object readStaticField(String yarnKey) {
        String runtimeName = yarnToRuntimeFields.get(yarnKey);
        try {
            Field f = ReflectionUtils.findField(targetClass, runtimeName);
            if (!Modifier.isStatic(f.getModifiers())) {
                throw new RuntimeException(yarnKey + " is not a static field.");
            }
            MethodHandle getter = FastAccessorUtils.getFieldGetter(f);
            return ScriptUtils.wrapReturn(getter.invoke(), this.mappingsManager, this.scriptManager);
        } catch (Throwable e) {
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
            MethodHandle setter = FastAccessorUtils.getFieldSetter(f);
            setter.invoke(javaVal);
        } catch (Throwable e) {
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
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

import lombok.Getter;
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
    @Getter
    private final Class<?> targetClass;
    private final String targetClassName;
    private final Map<String, List<String>> namedToRuntimeMethods;
    private final Map<String, String> namedToRuntimeFields;
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
        this.namedToRuntimeMethods = Map.copyOf(methodLookup);
        this.namedToRuntimeFields = Map.copyOf(fieldLookup);
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
            return getExplicitFieldMember(key);
        }

        if (namedToRuntimeMethods.containsKey(key)) {
            return createStaticMethodProxyFromNamedKey(key);
        }

        List<Method> directMethods = ReflectionUtils.findMethods(targetClass, List.of(key), true);
        if (!directMethods.isEmpty()) {
            return createStaticMethodProxyFromMethods(directMethods, key);
        }

        if (namedToRuntimeFields.containsKey(key)) {
            return readStaticField(key);
        }

        return null;
    }

    private Object getExplicitFieldMember(String key) {
        String fieldName = key.substring(0, key.length() - 1);
        if (namedToRuntimeFields.containsKey(fieldName)) {
            return readStaticField(fieldName);
        }
        return null;
    }

    @Override
    public boolean hasMember(String key) {
        if (WrapperConstants.CLASS.equals(key)) {
            return true;
        }
        if (key.endsWith(WrapperConstants.FIELD_SUFFIX)) {
            return namedToRuntimeFields.containsKey(key.substring(0, key.length() - 1));
        }
        if (namedToRuntimeMethods.containsKey(key) || namedToRuntimeFields.containsKey(key)) {
            return true;
        }
        return !ReflectionUtils.findMethods(targetClass, List.of(key), true).isEmpty();
    }

    @Override
    public Object getMemberKeys() {
        Set<String> keys = new LinkedHashSet<>();
        keys.add(WrapperConstants.CLASS);
        keys.addAll(namedToRuntimeMethods.keySet());
        keys.addAll(namedToRuntimeFields.keySet());
        namedToRuntimeFields.keySet().forEach(field -> keys.add(field + WrapperConstants.FIELD_SUFFIX));
        collectStaticMethodNames(keys);
        return keys.toArray(String[]::new);
    }

    private void collectStaticMethodNames(Set<String> keys) {
        for (Method m : targetClass.getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                keys.add(m.getName());
            }
        }
    }

    @Override
    public void putMember(String key, Value value) {
        boolean isExplicitFieldAccess = key.endsWith(WrapperConstants.FIELD_SUFFIX);
        String fieldName = isExplicitFieldAccess ? key.substring(0, key.length() - 1) : key;

        if (!namedToRuntimeFields.containsKey(fieldName)) {
            throw new UnsupportedOperationException("No writable static member: " + key);
        }

        if (!isExplicitFieldAccess && namedToRuntimeMethods.containsKey(fieldName)) {
            throw new UnsupportedOperationException(
                    "Ambiguous write to static member '" + fieldName + "'. A static method with this name exists. " +
                            "Use the '$' suffix to write to the field directly: " + fieldName + WrapperConstants.FIELD_SUFFIX
            );
        }

        writeStaticField(fieldName, value);
    }

    private Object invokeConstructor(Value[] polyglotArgs) {
        Constructor<?> matchingCtor = findConstructorByArgCount(polyglotArgs.length);
        if (matchingCtor == null) {
            throw noMatchingConstructorException(polyglotArgs.length);
        }
        return invokeConstructorWithArgs(matchingCtor, polyglotArgs);
    }

    private Constructor<?> findConstructorByArgCount(int argCount) {
        for (Constructor<?> ctor : constructors) {
            if (ctor.getParameterCount() == argCount) {
                return ctor;
            }
        }
        return null;
    }

    private Object invokeConstructorWithArgs(Constructor<?> ctor, Value[] polyglotArgs) {
        try {
            Object[] javaArgs = ScriptUtils.unwrapArgs(polyglotArgs, ctor.getParameterTypes());
            MethodHandle handle = lookup.unreflectConstructor(ctor);
            Object instance = handle.invokeWithArguments(javaArgs);
            return ScriptUtils.wrapReturn(instance, mappingsManager, scriptManager);
        } catch (Throwable e) {
            throw new IllegalStateException(
                    String.format("Failed to instantiate %s: %s", targetClassName, e.getMessage()), e);
        }
    }

    private IllegalArgumentException noMatchingConstructorException(int argCount) {
        String available = constructors.stream()
                .map(c -> c.getParameterCount() + " args")
                .distinct()
                .collect(Collectors.joining(", "));
        return new IllegalArgumentException(
                String.format("No constructor for %s with %d args. Available: [%s]", targetClassName, argCount, available));
    }

    private ProxyExecutable createStaticMethodProxyFromMethods(List<Method> methods, String methodNameForErrors) {
        return polyglotArgs -> invokeStaticMethod(methods, polyglotArgs, methodNameForErrors);
    }

    private Object invokeStaticMethod(List<Method> methods, Value[] polyglotArgs, String methodName) {
        Method matchingMethod = findMethodByArgCount(methods, polyglotArgs.length);
        if (matchingMethod == null) {
            throw new IllegalArgumentException(
                    String.format("No static overload for %s.%s with %d args", targetClassName, methodName, polyglotArgs.length));
        }
        return invokeMethodWithArgs(matchingMethod, polyglotArgs, methodName);
    }

    private Method findMethodByArgCount(List<Method> methods, int argCount) {
        for (Method m : methods) {
            if (m.getParameterCount() == argCount) {
                return m;
            }
        }
        return null;
    }

    private Object invokeMethodWithArgs(Method method, Value[] polyglotArgs, String methodName) {
        try {
            Object[] javaArgs = ScriptUtils.unwrapArgs(polyglotArgs, method.getParameterTypes());
            MethodHandle handle = FastAccessorUtils.getMethodHandle(method);
            Object result = handle.invokeWithArguments(javaArgs);
            return ScriptUtils.wrapReturn(result, mappingsManager, scriptManager);
        } catch (Throwable e) {
            throw createMethodInvocationException(methodName, e);
        }
    }

    private IllegalStateException createMethodInvocationException(String methodName, Throwable e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        String message = cause != e
                ? String.format("Static method %s.%s threw an exception: %s", targetClassName, methodName, cause.getMessage())
                : String.format("Method invocation failed for static method %s.%s", targetClassName, methodName);
        return new IllegalStateException(message, cause);
    }

    private ProxyExecutable createStaticMethodProxyFromNamedKey(String namedKey) {
        List<String> runtimeNames = namedToRuntimeMethods.get(namedKey);
        List<Method> methods = ReflectionUtils.findMethods(targetClass, runtimeNames, true);
        return createStaticMethodProxyFromMethods(methods, namedKey);
    }

    private Object readStaticField(String namedKey) {
        String runtimeName = namedToRuntimeFields.get(namedKey);
        Field field;
        try {
            field = ReflectionUtils.findField(targetClass, runtimeName);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(
                    String.format("Static field %s.%s not found", targetClassName, namedKey), e);
        }

        validateStaticField(field, namedKey);
        return getFieldValue(field, namedKey);
    }

    private void validateStaticField(Field field, String namedKey) {
        if (!Modifier.isStatic(field.getModifiers())) {
            throw new IllegalStateException(namedKey + " is not a static field.");
        }
    }

    private Object getFieldValue(Field field, String namedKey) {
        try {
            MethodHandle getter = FastAccessorUtils.getFieldGetter(field);
            return ScriptUtils.wrapReturn(getter.invoke(), mappingsManager, scriptManager);
        } catch (Throwable e) {
            throw new IllegalStateException(
                    String.format("Error accessing static field %s.%s: %s", targetClassName, namedKey, e.getMessage()), e);
        }
    }

    private void writeStaticField(String namedKey, Value value) {
        String runtimeName = namedToRuntimeFields.get(namedKey);
        Field field;
        try {
            field = ReflectionUtils.findField(targetClass, runtimeName);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(
                    String.format("Static field %s.%s not found", targetClassName, namedKey), e);
        }

        validateWritableStaticField(field, namedKey);
        setFieldValue(field, value, namedKey);
    }

    private void validateWritableStaticField(Field field, String namedKey) {
        if (!Modifier.isStatic(field.getModifiers())) {
            throw new UnsupportedOperationException("Cannot write to non-static field '" + namedKey + "' via class proxy.");
        }
        if (Modifier.isFinal(field.getModifiers())) {
            throw new UnsupportedOperationException("Cannot modify final static field '" + namedKey + "'.");
        }
    }

    private void setFieldValue(Field field, Value value, String namedKey) {
        try {
            Object javaVal = ScriptUtils.unwrapArgs(new Value[]{value}, new Class[]{field.getType()})[0];
            MethodHandle setter = FastAccessorUtils.getFieldSetter(field);
            setter.invoke(javaVal);
        } catch (Throwable e) {
            throw new IllegalStateException(
                    String.format("Error setting static field %s.%s: %s", targetClassName, namedKey, e.getMessage()), e);
        }
    }

    public Map<String, List<String>> getMethodMappings() {
        return namedToRuntimeMethods;
    }

    public Map<String, String> getFieldMappings() {
        return namedToRuntimeFields;
    }
}

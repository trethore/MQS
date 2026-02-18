/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Réthoré
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

    @Getter
    private final Object javaInstance;
    private final Class<?> instanceClass;
    private final String instanceClassName;
    private final MethodLookup methods;
    private final FieldLookup fields;
    private final String[] memberKeys;

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
        this.instanceClassName = instanceClass.getName();
        this.methods = new MethodLookup(methodMap);
        this.fields = new FieldLookup(fieldMap);
        this.mappingsManager = mappingsManager;
        this.scriptManager = scriptManager;
        this.memberKeys = buildMemberKeys();
    }

    private String[] buildMemberKeys() {
        return MEMBER_KEYS_CACHE.computeIfAbsent(this.instanceClass, clazz -> {
            Set<String> keys = new HashSet<>(methods.methodKeys());
            keys.addAll(fields.fieldKeys());
            collectInstanceMethodNames(clazz, keys);
            collectInstanceFieldNames(clazz, keys);
            keys.add(WrapperConstants.SELF);
            keys.add(WrapperConstants.EQUALS);
            keys.add(WrapperConstants.INSTANCE_OF);
            return keys.toArray(new String[0]);
        });
    }

    private void collectInstanceMethodNames(Class<?> clazz, Set<String> keys) {
        for (Method method : clazz.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                keys.add(method.getName());
            }
        }
    }

    private void collectInstanceFieldNames(Class<?> clazz, Set<String> keys) {
        for (Field field : clazz.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                keys.add(field.getName());
            }
        }
    }

    @Override
    public Object getMember(String key) {
        if (WrapperConstants.INSTANCE_OF.equals(key)) {
            return createInstanceOfProxy();
        }
        if (WrapperConstants.EQUALS.equals(key)) {
            return createEqualsProxy();
        }
        if (WrapperConstants.SELF.equals(key)) {
            return this.javaInstance;
        }
        if (key.endsWith(WrapperConstants.FIELD_SUFFIX)) {
            return handleField(key.substring(0, key.length() - 1));
        }

        Object mapped = handleMappedMethod(key);
        if (mapped != null) {
            return mapped;
        }

        Object direct = handleDirectMethod(key);
        if (direct != null) {
            return direct;
        }

        return handleField(key);
    }

    private ProxyExecutable createInstanceOfProxy() {
        return args -> {
            validateInstanceOfArgs(args);
            Class<?> targetClass = extractClassFromValue(args[0]);
            return targetClass.isInstance(this.javaInstance);
        };
    }

    private void validateInstanceOfArgs(Value[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("_instanceof(class) requires exactly one argument.");
        }
        if (args[0] == null || args[0].isNull()) {
            throw new IllegalArgumentException("The argument to _instanceof cannot be null.");
        }
    }

    private Class<?> extractClassFromValue(Value classValue) {
        Object proxy = classValue.isProxyObject() ? classValue.asProxyObject() : null;

        if (proxy instanceof LazyJsClassHolder holder) {
            return holder.getWrapper().getTargetClass();
        }
        if (proxy instanceof JsClassWrapper wrapper) {
            return wrapper.getTargetClass();
        }

        Object unwrapped = ScriptUtils.unwrapReceiver(classValue);
        if (unwrapped instanceof Class<?> clazz) {
            return clazz;
        }

        throw new IllegalArgumentException("The argument to _instanceof must be a class.");
    }

    private ProxyExecutable createEqualsProxy() {
        return args -> {
            if (args.length != 1) {
                return false;
            }
            Object otherRaw = ScriptUtils.unwrapReceiver(args[0]);
            return this.javaInstance.equals(otherRaw);
        };
    }

    @Override
    public boolean hasMember(String key) {
        if (WrapperConstants.SELF.equals(key)
                || WrapperConstants.EQUALS.equals(key)
                || WrapperConstants.INSTANCE_OF.equals(key)) {
            return true;
        }
        if (key.endsWith(WrapperConstants.FIELD_SUFFIX)) {
            return fields.hasField(instanceClass, key.substring(0, key.length() - 1));
        }
        return methods.hasMapped(key)
                || MethodLookup.hasDirect(instanceClass, key)
                || fields.hasField(instanceClass, key);
    }

    @Override
    public Object getMemberKeys() {
        return this.memberKeys;
    }

    @Override
    public void putMember(String key, Value value) {
        if (WrapperConstants.EQUALS.equals(key)) {
            throw new UnsupportedOperationException("Cannot override the built-in 'equals' method.");
        }

        boolean isExplicitFieldAccess = key.endsWith(WrapperConstants.FIELD_SUFFIX);
        String fieldName = isExplicitFieldAccess ? key.substring(0, key.length() - 1) : key;

        if (!fields.hasField(instanceClass, fieldName)) {
            throw new UnsupportedOperationException("No writable member: " + key);
        }

        validateNoAmbiguousWrite(fieldName, isExplicitFieldAccess);
        writeField(fieldName, value);
    }

    private void validateNoAmbiguousWrite(String fieldName, boolean isExplicitFieldAccess) {
        if (isExplicitFieldAccess) {
            return;
        }
        boolean methodConflict = methods.hasMapped(fieldName) || MethodLookup.hasDirect(instanceClass, fieldName);
        if (methodConflict) {
            throw new UnsupportedOperationException(
                    "Ambiguous write to '" + fieldName + "'. A method with this name exists. " +
                            "Use the '$' suffix to write to the field directly: " + fieldName + WrapperConstants.FIELD_SUFFIX
            );
        }
    }

    private Object handleMappedMethod(String key) {
        List<Method> candidates = methods.findMethods(instanceClass, key);
        if (candidates.isEmpty()) {
            return null;
        }
        return (ProxyExecutable) args -> invokeMethods(candidates, args, key);
    }

    private Object handleDirectMethod(String key) {
        List<Method> direct = MethodLookup.findDirect(instanceClass, key);
        if (direct.isEmpty()) {
            return null;
        }
        return (ProxyExecutable) args -> invokeMethods(direct, args, key);
    }

    private Object handleField(String key) {
        Field field = findFieldOrNull(key);
        if (field == null) {
            return null;
        }
        if (Modifier.isStatic(field.getModifiers())) {
            return null;
        }
        return readFieldValue(field, key);
    }

    private Field findFieldOrNull(String key) {
        try {
            return fields.accessField(instanceClass, key);
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private Object readFieldValue(Field field, String fieldName) {
        try {
            MethodHandle getter = FastAccessorUtils.getFieldGetter(field);
            Object result = getter.bindTo(javaInstance).invoke();
            return ScriptUtils.wrapReturn(result, this.mappingsManager, this.scriptManager);
        } catch (Throwable e) {
            throw new IllegalStateException(
                    String.format("Field access failed for %s.%s: %s", instanceClassName, fieldName, e.getMessage()), e);
        }
    }

    private Object invokeMethods(List<Method> methods, Value[] args, String methodName) {
        Main.LOGGER.debug("Attempting to invoke method '{}' on {} with {} args.",
                methodName, instanceClassName, args.length);

        Method matchingMethod = findMethodByArgCount(methods, args.length, methodName);
        if (matchingMethod == null) {
            throw new IllegalArgumentException(
                    String.format("No overload for method '%s' on %s with %d args", methodName, instanceClassName, args.length));
        }

        return invokeMethodWithArgs(matchingMethod, args, methodName);
    }

    @SuppressWarnings("java:S2629") // SonarQube is drunk
    private Method findMethodByArgCount(List<Method> methods, int argCount, String methodName) {
        boolean debugEnabled = Main.LOGGER.isDebugEnabled();
        for (Method method : methods) {
            if (debugEnabled) {
                Main.LOGGER.debug("  - Considering candidate method: {}", method.toGenericString());
            }
            if (method.getParameterCount() == argCount) {
                if (debugEnabled) {
                    Main.LOGGER.debug("    - Argument count matches. Proceeding to invoke.");
                }
                return method;
            }
            if (debugEnabled) {
                Main.LOGGER.debug("    - Argument count mismatch. Expected: {}, Got: {}. Skipping.",
                        method.getParameterCount(), argCount);
            }
        }
        Main.LOGGER.warn("No suitable overload found for method '{}' on {} with {} arguments.",
                methodName, instanceClassName, argCount);
        return null;
    }

    private Object invokeMethodWithArgs(Method method, Value[] args, String methodName) {
        try {
            Object[] javaArgs = ScriptUtils.unwrapArgs(args, method.getParameterTypes());
            Main.LOGGER.debug("    - Unwrapped arguments successfully.");
            MethodHandle handle = FastAccessorUtils.getMethodHandle(method);
            Object result = handle.bindTo(this.javaInstance).invokeWithArguments(javaArgs);
            Main.LOGGER.debug("    - Invocation successful. Result: {}", result);
            return ScriptUtils.wrapReturn(result, this.mappingsManager, this.scriptManager);
        } catch (Throwable e) {
            throw createMethodInvocationException(methodName, e);
        }
    }

    private IllegalStateException createMethodInvocationException(String methodName, Throwable e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        String message = cause != e
                ? String.format("Method '%s' on %s threw an exception: %s", methodName, instanceClassName, cause.getMessage())
                : String.format("Method invocation failed for '%s' on %s", methodName, instanceClassName);
        return new IllegalStateException(message, cause);
    }

    private void writeField(String key, Value value) {
        Field field = findFieldForWrite(key);
        validateWritableField(field, key);
        setFieldValue(field, value, key);
    }

    private Field findFieldForWrite(String key) {
        try {
            return fields.accessField(instanceClass, key);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(
                    String.format("Field %s.%s not found", instanceClassName, key), e);
        }
    }

    private void validateWritableField(Field field, String fieldName) {
        if (Modifier.isStatic(field.getModifiers())) {
            throw new UnsupportedOperationException(
                    "Cannot modify static field '" + fieldName + "' via instance proxy.");
        }
        if (Modifier.isFinal(field.getModifiers())) {
            throw new UnsupportedOperationException(
                    "Cannot modify final field '" + fieldName + "'.");
        }
    }

    private void setFieldValue(Field field, Value value, String fieldName) {
        try {
            Object javaVal = ScriptUtils.unwrapArgs(new Value[]{value}, new Class[]{field.getType()})[0];
            MethodHandle setter = FastAccessorUtils.getFieldSetter(field);
            setter.bindTo(javaInstance).invoke(javaVal);
        } catch (Throwable e) {
            throw new IllegalStateException(
                    String.format("Field write failed for %s.%s: %s", instanceClassName, fieldName, e.getMessage()), e);
        }
    }

    @Override
    public String toString() {
        return String.format("[MQS Wrapper: %s]", instanceClassName);
    }
}

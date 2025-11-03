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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JsObjectWrapper implements ProxyObject {
    private static final Map<Class<?>, String[]> MEMBER_KEYS_CACHE = new ConcurrentHashMap<>();
    @Getter
    private final Object javaInstance;
    private final Class<?> instanceClass;
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
            keys.add(WrapperConstants.SELF);
            keys.add(WrapperConstants.EQUALS);
            keys.add(WrapperConstants.INSTANCE_OF);
            return keys.toArray(new String[0]);
        });
    }

    @Override
    public Object getMember(String key) {
        if (WrapperConstants.INSTANCE_OF.equals(key)) {
            return (ProxyExecutable) (Value... args) -> {
                if (args.length != 1) {
                    throw new IllegalArgumentException("_instanceof(class) requires exactly one argument.");
                }

                Value classValue = args[0];
                Class<?> rawClass;

                if (classValue == null || classValue.isNull()) {
                    throw new IllegalArgumentException("The argument to _instanceof cannot be null.");
                }
                Object proxy = classValue.isProxyObject() ? classValue.asProxyObject() : null;
                if (proxy instanceof net.me.scripting.wrappers.LazyJsClassHolder holder) {
                    rawClass = holder.getWrapper().getTargetClass();
                } else if (proxy instanceof net.me.scripting.wrappers.JsClassWrapper wrapper) {
                    rawClass = wrapper.getTargetClass();
                } else {
                    Object unwrapped = ScriptUtils.unwrapReceiver(classValue);
                    if (unwrapped instanceof Class) {
                        rawClass = (Class<?>) unwrapped;
                    } else {
                        throw new IllegalArgumentException("The argument to _instanceof must be a class.");
                    }
                }

                return rawClass.isInstance(this.javaInstance);
            };
        }
        if (WrapperConstants.EQUALS.equals(key)) {
            return (ProxyExecutable) (Value... args) -> {
                if (args.length != 1) return false;
                Object otherRaw = ScriptUtils.unwrapReceiver(args[0]);
                return this.javaInstance.equals(otherRaw);
            };
        }

        if (WrapperConstants.SELF.equals(key)) return this.getJavaInstance();
        if (key.endsWith(WrapperConstants.FIELD_SUFFIX))
            return handleField(key.substring(0, key.length() - 1));
        Object mapped = handleMappedMethod(key);
        if (mapped != null) return mapped;
        Object direct = handleDirectMethod(key);
        if (direct != null) return direct;
        return handleField(key);
    }

    @Override
    public boolean hasMember(String key) {
        if (WrapperConstants.SELF.equals(key) || WrapperConstants.EQUALS.equals(key) || WrapperConstants.INSTANCE_OF.equals(key))
            return true;
        if (key.endsWith(WrapperConstants.FIELD_SUFFIX))
            return fields.hasField(instanceClass, key.substring(0, key.length() - 1));
        return methods.hasMapped(key) || MethodLookup.hasDirect(instanceClass, key) || fields.hasField(instanceClass, key);
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
        String fieldName = key;
        boolean isExplicitFieldAccess = false;
        if (key.endsWith(WrapperConstants.FIELD_SUFFIX)) {
            fieldName = key.substring(0, key.length() - 1);
            isExplicitFieldAccess = true;
        }
        if (fields.hasField(instanceClass, fieldName)) {
            boolean methodConflict = methods.hasMapped(fieldName) || MethodLookup.hasDirect(instanceClass, fieldName);
            if (methodConflict && !isExplicitFieldAccess) {
                throw new UnsupportedOperationException(
                        "Ambiguous write to '" + fieldName + "'. A method with this name exists. " +
                                "Use the '$' suffix to write to the field directly: " + fieldName + WrapperConstants.FIELD_SUFFIX
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
        Main.LOGGER.debug("Attempting to invoke method '{}' on instance of {} with {} args.", yarnName, this.instanceClass.getSimpleName(), args.length);
        for (Method m : methods) {
            Main.LOGGER.debug("  - Considering candidate method: {}", m.toGenericString());
            if (m.getParameterCount() == args.length) {
                Main.LOGGER.debug("    - Argument count matches. Proceeding to unwrap and invoke.");
                try {
                    Object[] javaArgs = ScriptUtils.unwrapArgs(args, m.getParameterTypes());
                    ScriptUtils.coerceArgumentTypes(this.javaInstance, m, javaArgs);
                    Main.LOGGER.debug("    - Unwrapped arguments: {}", Arrays.toString(javaArgs));
                    MethodHandle handle = FastAccessorUtils.getMethodHandle(m);
                    Object result = handle.bindTo(this.javaInstance).invokeWithArguments(javaArgs);
                    Main.LOGGER.debug("    - Invocation successful. Result: {}", result);
                    return ScriptUtils.wrapReturn(result, this.mappingsManager, this.scriptManager);
                } catch (Throwable e) {
                    Main.LOGGER.error("    - !! Invocation FAILED for method: {}", m.toGenericString(), e);
                    if (e.getCause() != null) {
                        throw new RuntimeException("Method '" + yarnName + "' threw an exception: " + e.getCause().getMessage(), e.getCause());
                    }
                    throw new RuntimeException("Method invocation failed for '" + yarnName + "'. See logs for details.", e);
                }
            } else {
                Main.LOGGER.debug("    - Argument count mismatch. Expected: {}, Got: {}. Skipping.", m.getParameterCount(), args.length);
            }
        }
        Main.LOGGER.warn("No suitable overload found for method '{}' on {} with {} arguments.", yarnName, this.instanceClass.getSimpleName(), args.length);
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

    @Override
    public String toString() {
        return String.format("[MQS Wrapper: %s]", this.instanceClass.getName());
    }
}

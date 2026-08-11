/*
 * My QOL Packages - Client-side Minecraft modding at runtime
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
package io.github.trethore.myqolpackages.internal.runtime.graal.api.java.interop;

import io.github.trethore.myqolpackages.internal.runtime.graal.api.java.interop.mapping.MappingIndex;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.Proxy;

final class JavaInteropService {
    private final JavaExecutableResolver executableResolver = new JavaExecutableResolver();
    private final Map<MemberCacheKey, JavaInteropMembers> memberCache = new HashMap<>();
    private final MappingIndex mappings;

    JavaInteropService(MappingIndex mappings) {
        this.mappings = mappings;
    }

    JavaInteropMembers getMembers(Class<?> targetClass, String preferredNamedClassName) {
        MemberCacheKey cacheKey = new MemberCacheKey(targetClass, preferredNamedClassName);
        return memberCache.computeIfAbsent(
                cacheKey, ignored -> new JavaInteropMembers(targetClass, preferredNamedClassName, mappings));
    }

    Object instantiate(
            Class<?> targetClass,
            String preferredNamedClassName,
            List<Constructor<?>> constructors,
            Value[] arguments) {
        JavaExecutableResolver.ResolvedExecutable<Constructor<?>> resolvedConstructor =
                executableResolver.resolveConstructor(targetClass, constructors, arguments);
        Constructor<?> constructor = resolvedConstructor.executable();
        try {
            if (!constructor.trySetAccessible() && !constructor.canAccess(null)) {
                throw inaccessibleMember(constructor.toGenericString());
            }
            Object instance = constructor.newInstance(resolvedConstructor.arguments());
            return wrapObject(instance, preferredNamedClassName);
        } catch (ReflectiveOperationException exception) {
            throw invocationFailure("constructor " + constructor.toGenericString(), exception);
        }
    }

    Object invokeMethod(
            Object receiver,
            String preferredNamedClassName,
            String exposedName,
            List<Method> methods,
            Value[] arguments) {
        JavaExecutableResolver.ResolvedExecutable<Method> resolvedMethod =
                executableResolver.resolveMethod(exposedName, methods, arguments);
        Method method = resolvedMethod.executable();
        try {
            if (!method.trySetAccessible() && !method.canAccess(receiver)) {
                throw inaccessibleMember(method.toGenericString());
            }
            Object result = method.invoke(receiver, resolvedMethod.arguments());
            return wrapReturn(result, preferredNamedClassName);
        } catch (IllegalAccessException exception) {
            throw inaccessibleMember(method.toGenericString(), exception);
        } catch (InvocationTargetException exception) {
            throw invocationFailure("method " + exposedName, exception);
        }
    }

    Object readField(Object receiver, String preferredNamedClassName, Field field) {
        try {
            if (!field.trySetAccessible() && !field.canAccess(receiver)) {
                throw inaccessibleMember(field.toGenericString());
            }
            return wrapReturn(field.get(receiver), preferredNamedClassName);
        } catch (IllegalAccessException exception) {
            throw inaccessibleMember(field.toGenericString(), exception);
        }
    }

    @SuppressWarnings("java:S3011")
    void writeField(Object receiver, Field field, String exposedName, Value value) {
        if (field == null) {
            throw new UnsupportedOperationException("No writable field " + exposedName);
        }
        if (Modifier.isFinal(field.getModifiers())) {
            throw new UnsupportedOperationException("Cannot modify final field " + exposedName);
        }
        Object convertedValue = executableResolver.convertValue(value, field.getType());
        try {
            if (!field.trySetAccessible() && !field.canAccess(receiver)) {
                throw inaccessibleMember(field.toGenericString());
            }
            field.set(receiver, convertedValue);
        } catch (IllegalAccessException exception) {
            throw inaccessibleMember(field.toGenericString(), exception);
        }
    }

    Object wrapObject(Object value, String preferredNamedClassName) {
        return new JavaObjectProxy(value, preferredNamedClassName, this);
    }

    Object wrap(Value value) {
        if (value.isNull()) {
            return null;
        }
        if (value.isProxyObject()) {
            Object proxy = value.asProxyObject();
            if (proxy instanceof JavaObjectProxy) {
                return proxy;
            }
        }
        if (!value.isHostObject()) {
            throw new IllegalArgumentException("wrap requires a Java object");
        }
        return wrapReturn(value.asHostObject(), null);
    }

    boolean objectsEqual(Object instance, Value other) {
        return instance.equals(executableResolver.convertValue(other, Object.class));
    }

    boolean isInstance(Object instance, Value type) {
        Object convertedType = executableResolver.convertValue(type, Class.class);
        if (!(convertedType instanceof Class<?> targetClass)) {
            throw new IllegalArgumentException(JavaInteropMembers.INSTANCEOF_MEMBER + " requires a Java class");
        }
        return targetClass.isInstance(instance);
    }

    private static IllegalStateException invocationFailure(String description, ReflectiveOperationException exception) {
        Throwable cause = exception instanceof InvocationTargetException invocationTargetException
                ? invocationTargetException.getTargetException()
                : exception;
        return new IllegalStateException("Failed to invoke " + description + ": " + cause, cause);
    }

    private Object wrapReturn(Object value, String preferredNamedClassName) {
        if (value == null
                || value instanceof String
                || value instanceof Character
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Value
                || value instanceof Proxy) {
            return value;
        }
        return wrapObject(value, resolveNamedClassName(value.getClass(), preferredNamedClassName));
    }

    private String resolveNamedClassName(Class<?> runtimeClass, String preferredNamedClassName) {
        if (preferredNamedClassName != null) {
            MappingIndex.ClassMapping preferredMapping = mappings.getClassMapping(preferredNamedClassName);
            if (preferredMapping != null
                    && (preferredMapping.runtimeClassName().equals(runtimeClass.getName())
                            || preferredMapping.namedClassName().equals(runtimeClass.getName()))) {
                return preferredNamedClassName;
            }
        }
        MappingIndex.ClassMapping classMapping = mappings.findClassMapping(runtimeClass);
        return classMapping == null ? runtimeClass.getName() : classMapping.namedClassName();
    }

    private static IllegalStateException inaccessibleMember(String member) {
        return inaccessibleMember(member, null);
    }

    private static IllegalStateException inaccessibleMember(String member, Throwable cause) {
        return new IllegalStateException(
                "Cannot access " + member + ". Its module may need to open the declaring package", cause);
    }

    private record MemberCacheKey(Class<?> targetClass, String preferredNamedClassName) {}
}

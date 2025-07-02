package net.me.scripting.utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FastAccessorUtils {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Map<Method, MethodHandle> METHOD_HANDLE_CACHE = new ConcurrentHashMap<>();
    private static final Map<Field, MethodHandle> FIELD_GETTER_CACHE = new ConcurrentHashMap<>();
    private static final Map<Field, MethodHandle> FIELD_SETTER_CACHE = new ConcurrentHashMap<>();

    private FastAccessorUtils() {
    }

    public static MethodHandle getMethodHandle(Method method) {
        return METHOD_HANDLE_CACHE.computeIfAbsent(method, m -> {
            try {
                m.setAccessible(true);
                return LOOKUP.unreflect(m);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to create MethodHandle for: " + m, e);
            }
        });
    }

    public static MethodHandle getFieldGetter(Field field) {
        return FIELD_GETTER_CACHE.computeIfAbsent(field, f -> {
            try {
                f.setAccessible(true);
                return LOOKUP.unreflectGetter(f);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to create getter MethodHandle for: " + f, e);
            }
        });
    }

    public static MethodHandle getFieldSetter(Field field) {
        return FIELD_SETTER_CACHE.computeIfAbsent(field, f -> {
            try {
                f.setAccessible(true);
                return LOOKUP.unreflectSetter(f);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to create setter MethodHandle for: " + f, e);
            }
        });
    }
}
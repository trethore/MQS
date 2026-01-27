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

package net.me.scripting.utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("java:S3011") // Accessibility bypass is intentional for this reflection utility
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
                throw new ReflectionAccessException("Failed to create MethodHandle for: " + m, e);
            }
        });
    }

    public static MethodHandle getFieldGetter(Field field) {
        return FIELD_GETTER_CACHE.computeIfAbsent(field, f -> {
            try {
                f.setAccessible(true);
                return LOOKUP.unreflectGetter(f);
            } catch (IllegalAccessException e) {
                throw new ReflectionAccessException("Failed to create getter MethodHandle for: " + f, e);
            }
        });
    }

    public static MethodHandle getFieldSetter(Field field) {
        return FIELD_SETTER_CACHE.computeIfAbsent(field, f -> {
            try {
                f.setAccessible(true);
                return LOOKUP.unreflectSetter(f);
            } catch (IllegalAccessException e) {
                throw new ReflectionAccessException("Failed to create setter MethodHandle for: " + f, e);
            }
        });
    }

    public static class ReflectionAccessException extends RuntimeException {
        public ReflectionAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
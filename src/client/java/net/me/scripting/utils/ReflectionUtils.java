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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

@SuppressWarnings("java:S3011") // Accessibility bypass is intentional for this reflection utility
public final class ReflectionUtils {

    private ReflectionUtils() {
    }

    public static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException _) {
                // Continue searching in superclass
            }
        }
        throw new NoSuchFieldException("Field '" + name + "' not found in class " + cls + " or its superclasses.");
    }

    public static List<Method> findMethods(Class<?> cls, List<String> names, boolean isStatic) {
        List<Method> result = new ArrayList<>();
        Set<String> foundSignatures = new HashSet<>();
        Set<Class<?>> visited = new HashSet<>();
        Queue<Class<?>> toSearch = new LinkedList<>();

        if (cls != null) {
            toSearch.add(cls);
        }

        while (!toSearch.isEmpty()) {
            Class<?> current = toSearch.poll();
            if (current == null || !visited.add(current)) {
                continue;
            }

            collectMatchingMethods(current, names, isStatic, foundSignatures, result);
            enqueueRelatedClasses(current, toSearch);
        }
        return result;
    }

    private static void collectMatchingMethods(Class<?> cls, List<String> names, boolean isStatic,
                                               Set<String> foundSignatures, List<Method> result) {
        for (Method method : cls.getDeclaredMethods()) {
            if (!isMethodMatch(method, names, isStatic)) {
                continue;
            }
            String signature = method.getName() + Arrays.toString(method.getParameterTypes());
            if (foundSignatures.add(signature)) {
                method.setAccessible(true);
                result.add(method);
            }
        }
    }

    private static boolean isMethodMatch(Method method, List<String> names, boolean isStatic) {
        return names.contains(method.getName()) && Modifier.isStatic(method.getModifiers()) == isStatic;
    }

    private static void enqueueRelatedClasses(Class<?> cls, Queue<Class<?>> toSearch) {
        if (cls.getSuperclass() != null) {
            toSearch.add(cls.getSuperclass());
        }
        Collections.addAll(toSearch, cls.getInterfaces());
    }
}
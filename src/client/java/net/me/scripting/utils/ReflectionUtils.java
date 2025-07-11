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

public final class ReflectionUtils {

    private ReflectionUtils() {
    }

    public static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException("Field '" + name + "' not found in class " + cls + " or its superclasses.");
    }

    public static List<Method> findMethods(Class<?> cls, List<String> names, boolean isStatic) {
        List<Method> list = new ArrayList<>();
        Set<String> foundSignatures = new HashSet<>();
        Queue<Class<?>> toSearch = new LinkedList<>();
        Set<Class<?>> visited = new HashSet<>();

        if (cls != null) {
            toSearch.add(cls);
        }

        while (!toSearch.isEmpty()) {
            Class<?> current = toSearch.poll();
            if (current == null || !visited.add(current)) {
                continue;
            }

            for (Method m : current.getDeclaredMethods()) {
                if (names.contains(m.getName()) && Modifier.isStatic(m.getModifiers()) == isStatic) {
                    String signature = m.getName() + Arrays.toString(m.getParameterTypes());
                    if (foundSignatures.add(signature)) {
                        m.setAccessible(true);
                        list.add(m);
                    }
                }
            }

            if (current.getSuperclass() != null) {
                toSearch.add(current.getSuperclass());
            }
            toSearch.addAll(Arrays.asList(current.getInterfaces()));
        }
        return list;
    }
}
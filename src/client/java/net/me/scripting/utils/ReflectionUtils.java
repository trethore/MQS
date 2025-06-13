package net.me.scripting.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class ReflectionUtils {

    private ReflectionUtils() {}

    public static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
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
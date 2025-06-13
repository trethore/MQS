package net.me.scripting.wrappers.support;

import net.me.scripting.utils.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MethodLookup {
    private final Map<String, List<String>> map;

    public MethodLookup(Map<String, List<String>> map) {
        this.map = map != null ? map : Collections.emptyMap();
    }

    public boolean hasMapped(String key) { return map.containsKey(key); }
    public Set<String> methodKeys() { return map.keySet(); }

    public List<Method> findMethods(Class<?> cls, String key) {
        List<String> names = map.getOrDefault(key, List.of());
        return ReflectionUtils.findMethods(cls, names, false);
    }

    public static List<Method> findDirect(Class<?> cls, String key) {
        return ReflectionUtils.findMethods(cls, List.of(key), false);
    }

    public static boolean hasDirect(Class<?> cls, String key) {
        return !findDirect(cls, key).isEmpty();
    }
}
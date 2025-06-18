package net.me.scripting.wrappers.support;

import net.me.scripting.utils.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MethodLookup {
    private final Map<String, List<String>> map;
    private static final Map<Class<?>, Map<String, List<Method>>> methodCache = new ConcurrentHashMap<>();

    public MethodLookup(Map<String, List<String>> map) {
        this.map = map != null ? map : Collections.emptyMap();
    }

    private static List<Method> findAndCache(Class<?> cls, String cacheKey, List<String> namesToSearch) {
        Map<String, List<Method>> classCache = methodCache.computeIfAbsent(cls, k -> new ConcurrentHashMap<>());
        return classCache.computeIfAbsent(cacheKey, k -> ReflectionUtils.findMethods(cls, namesToSearch, false));
    }

    public boolean hasMapped(String key) {
        return map.containsKey(key);
    }

    public Set<String> methodKeys() {
        return map.keySet();
    }

    public List<Method> findMethods(Class<?> cls, String key) {
        List<String> runtimeNames = map.getOrDefault(key, Collections.emptyList());
        if (runtimeNames.isEmpty()) {
            return Collections.emptyList();
        }
        return findAndCache(cls, key, runtimeNames);
    }

    public static List<Method> findDirect(Class<?> cls, String key) {
        return findAndCache(cls, key, List.of(key));
    }

    public static boolean hasDirect(Class<?> cls, String key) {
        return !findDirect(cls, key).isEmpty();
    }
}
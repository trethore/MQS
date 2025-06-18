package net.me.scripting.wrappers.support;

import net.me.scripting.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FieldLookup {
    private final Map<String, String> map;
    private final Map<Class<?>, Map<String, Field>> fieldCache = new ConcurrentHashMap<>();

    public FieldLookup(Map<String, String> map) {
        this.map = map != null ? map : Collections.emptyMap();
    }

    public boolean hasField(Class<?> cls, String key) {
        try {
            accessField(cls, key);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    public Set<String> fieldKeys() {
        return map.keySet();
    }

    public Field accessField(Class<?> cls, String key) throws NoSuchFieldException {
        Map<String, Field> classCache = fieldCache.computeIfAbsent(cls, k -> new ConcurrentHashMap<>());
        Field cachedField = classCache.get(key);
        if (cachedField != null) {
            return cachedField;
        }

        Field foundField;
        String runtimeName = map.get(key);
        if (runtimeName != null) {
            try {
                foundField = ReflectionUtils.findField(cls, runtimeName);
            } catch (NoSuchFieldException ignored) {
                foundField = ReflectionUtils.findField(cls, key);
            }
        } else {
            foundField = ReflectionUtils.findField(cls, key);
        }

        classCache.put(key, foundField);
        return foundField;
    }
}
package net.me.scripting.wrappers.support;

import net.me.scripting.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class FieldLookup {
    private final Map<String, String> map;

    public FieldLookup(Map<String, String> map) {
        this.map = map != null ? map : Collections.emptyMap();
    }

    public boolean hasField(Class<?> cls, String key) {
        if (map.containsKey(key)) {
            return true;
        }
        try {
            ReflectionUtils.findField(cls, key);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    public Set<String> fieldKeys() {
        return map.keySet();
    }

    public Field accessField(Class<?> cls, String key) throws NoSuchFieldException {
        String runtimeName = map.get(key);
        if (runtimeName != null) {
            try {
                Field f = ReflectionUtils.findField(cls, runtimeName);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }

        Field f = ReflectionUtils.findField(cls, key);
        f.setAccessible(true);
        return f;
    }
}
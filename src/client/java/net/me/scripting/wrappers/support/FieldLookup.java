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
        } catch (NoSuchFieldException _) {
            return false;
        }
    }

    public Set<String> fieldKeys() {
        return map.keySet();
    }

    public Field accessField(Class<?> cls, String key) throws NoSuchFieldException {
        Map<String, Field> classCache = fieldCache.computeIfAbsent(cls, _ -> new ConcurrentHashMap<>());
        Field cachedField = classCache.get(key);
        if (cachedField != null) {
            return cachedField;
        }

        Field foundField;
        String runtimeName = map.get(key);
        if (runtimeName != null) {
            try {
                foundField = ReflectionUtils.findField(cls, runtimeName);
            } catch (NoSuchFieldException _) {
                foundField = ReflectionUtils.findField(cls, key);
            }
        } else {
            foundField = ReflectionUtils.findField(cls, key);
        }

        classCache.put(key, foundField);
        return foundField;
    }
}
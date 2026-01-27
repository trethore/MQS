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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MappingUtils {

    private static final Map<Class<?>, ClassMappings> MAPPINGS_CACHE = new ConcurrentHashMap<>();

    private MappingUtils() {
    }

    public static ClassMappings combineMappings(Class<?> cls,
                                                Map<String, String> runtimeToYarn,
                                                Map<String, Map<String, List<String>>> methodsMap,
                                                Map<String, Map<String, String>> fieldsMap) {
        return MAPPINGS_CACHE.computeIfAbsent(cls, c -> {
            Map<String, List<String>> methods = new LinkedHashMap<>();
            Map<String, String> fields = new LinkedHashMap<>();
            combineMappingsIterative(c, runtimeToYarn, methodsMap, fieldsMap, methods, fields);
            return new ClassMappings(methods, fields);
        });
    }

    private static void combineMappingsIterative(Class<?> startCls,
                                                 Map<String, String> runtimeToYarn,
                                                 Map<String, Map<String, List<String>>> methodsMap,
                                                 Map<String, Map<String, String>> fieldsMap,
                                                 Map<String, List<String>> accMethods,
                                                 Map<String, String> accFields) {
        if (startCls == null) {
            return;
        }

        Queue<Class<?>> toSearch = new LinkedList<>();
        Set<Class<?>> seen = new HashSet<>();
        toSearch.add(startCls);

        while (!toSearch.isEmpty()) {
            Class<?> current = toSearch.poll();
            if (current == null || !seen.add(current)) {
                continue;
            }

            collectMappingsForClass(current, runtimeToYarn, methodsMap, fieldsMap, accMethods, accFields);
            enqueueParentTypes(current, toSearch);
        }
    }

    private static void collectMappingsForClass(Class<?> cls,
                                                Map<String, String> runtimeToYarn,
                                                Map<String, Map<String, List<String>>> methodsMap,
                                                Map<String, Map<String, String>> fieldsMap,
                                                Map<String, List<String>> accMethods,
                                                Map<String, String> accFields) {
        String yarnName = runtimeToYarn.get(cls.getName());
        if (yarnName == null) {
            return;
        }

        Map<String, List<String>> methods = methodsMap.get(yarnName);
        if (methods != null) {
            methods.forEach(accMethods::putIfAbsent);
        }

        Map<String, String> fields = fieldsMap.get(yarnName);
        if (fields != null) {
            fields.forEach(accFields::putIfAbsent);
        }
    }

    private static void enqueueParentTypes(Class<?> cls, Queue<Class<?>> queue) {
        Class<?> superclass = cls.getSuperclass();
        if (superclass != null) {
            queue.add(superclass);
        }
        Collections.addAll(queue, cls.getInterfaces());
    }

    public record ClassMappings(
            Map<String, List<String>> methods,
            Map<String, String> fields
    ) {
    }
}
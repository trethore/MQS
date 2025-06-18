package net.me.scripting.utils;

import java.util.*;

public final class MappingUtils {

    private MappingUtils() {
    }

    public record ClassMappings(
            Map<String, List<String>> methods,
            Map<String, String> fields
    ) {
    }

    public static ClassMappings combineMappings(Class<?> cls,
                                                Map<String, String> runtimeToYarn,
                                                Map<String, Map<String, List<String>>> methodsMap,
                                                Map<String, Map<String, String>> fieldsMap) {
        Map<String, List<String>> methods = new LinkedHashMap<>();
        Map<String, String> fields = new LinkedHashMap<>();
        combineMappingsIterative(cls, runtimeToYarn, methodsMap, fieldsMap, methods, fields);
        return new ClassMappings(methods, fields);
    }

    private static void combineMappingsIterative(Class<?> startCls,
                                                 Map<String, String> r2y,
                                                 Map<String, Map<String, List<String>>> mMap,
                                                 Map<String, Map<String, String>> fMap,
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

            String yarn = r2y.get(current.getName());
            if (yarn != null) {
                Map<String, List<String>> mm = mMap.get(yarn);
                if (mm != null) {
                    mm.forEach(accMethods::putIfAbsent);
                }

                Map<String, String> fm = fMap.get(yarn);
                if (fm != null) {
                    fm.forEach(accFields::putIfAbsent);
                }
            }

            if (current.getSuperclass() != null) {
                toSearch.add(current.getSuperclass());
            }
            toSearch.addAll(Arrays.asList(current.getInterfaces()));
        }
    }
}
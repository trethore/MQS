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
        combineRecursive(cls, runtimeToYarn, methodsMap, fieldsMap, methods, fields, new HashSet<>());
        return new ClassMappings(methods, fields);
    }

    private static void combineRecursive(Class<?> cls,
                                         Map<String, String> r2y,
                                         Map<String, Map<String, List<String>>> mMap,
                                         Map<String, Map<String, String>> fMap,
                                         Map<String, List<String>> accMethods,
                                         Map<String, String> accFields,
                                         Set<Class<?>> seen) {
        if (cls == null || !seen.add(cls)) return;

        String yarn = r2y.get(cls.getName());
        if (yarn != null) {
            Map<String, List<String>> mm = mMap.get(yarn);
            if (mm != null) mm.forEach(accMethods::putIfAbsent);

            Map<String, String> fm = fMap.get(yarn);
            if (fm != null) fm.forEach(accFields::putIfAbsent);
        }

        for (Class<?> iface : cls.getInterfaces()) {
            combineRecursive(iface, r2y, mMap, fMap, accMethods, accFields, seen);
        }
        combineRecursive(cls.getSuperclass(), r2y, mMap, fMap, accMethods, accFields, seen);
    }
}
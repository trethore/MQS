package net.me.scripting;

import net.me.mappings.MappingsManager;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class ScriptUtils {

    public record ClassMappings(
            Map<String, List<String>> methods,
            Map<String, String> fields
    ) {}

    public static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException("Field '" + name + "' not found in " + cls.getName());
    }

    public static List<Method> findMethods(Class<?> cls, List<String> names, boolean isStatic) {
        List<Method> list = new ArrayList<>();
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (names.contains(m.getName()) && Modifier.isStatic(m.getModifiers()) == isStatic) {
                    m.setAccessible(true);
                    list.add(m);
                }
            }
        }
        return list;
    }

    public static Object[] unwrapArgs(Value[] args, Class<?>[] types) {
        if (args == null) return new Object[0];
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            out[i] = convertValue(args[i], types != null && i < types.length ? types[i] : null);
        }
        return out;
    }

    private static Object convertValue(Value v, Class<?> expected) {
        if (v == null || v.isNull()) return null;
        if (v.isBoolean()) return v.asBoolean();
        if (v.isNumber()) return convertNumber(v, expected);
        if (v.isString()) return v.asString();
        if (v.isHostObject()) return v.asHostObject();
        if (v.isProxyObject()) return extractProxy(v, expected);
        if (v.canExecute() && expected != null && expected.isAnnotationPresent(FunctionalInterface.class)) {
            try { return v.as(expected); }
            catch (Exception e) { /* fallback */ }
        }
        return v;
    }

    private static Object convertNumber(Value v, Class<?> expected) {
        try {
            if (expected == int.class || expected == Integer.class) return v.asInt();
            if (expected == long.class || expected == Long.class) return v.asLong();
            if (expected == float.class || expected == Float.class) return v.asFloat();
            if (expected == double.class || expected == Double.class) return v.asDouble();
        } catch (Exception ignored) {}
        return v.asDouble();
    }

    private static Object extractProxy(Value v, Class<?> expected) {
        ProxyObject proxy = v.asProxyObject();
        if (proxy instanceof JsObjectWrapper) return ((JsObjectWrapper) proxy).getJavaInstance();
        if (proxy instanceof JsExtendedObjectWrapper) return ((JsExtendedObjectWrapper) proxy).getJavaInstance();
        if (proxy instanceof LazyJsClassHolder && expected == Class.class) {
            return ((LazyJsClassHolder) proxy).getWrapper().getMember("_class");
        }
        return proxy;
    }

    public static Object wrapReturn(Object o) {
        if (o == null || o instanceof String || o instanceof Number || o instanceof Boolean) return o;
        Class<?> c = o.getClass();
        Map<String, String> runtimeToYarn = MappingsManager.getInstance().getRuntimeToYarnClassMap();
        if (runtimeToYarn.containsKey(c.getName()) || c.isArray()) {
            ClassMappings cm = combineMappings(c, runtimeToYarn,
                    MappingsManager.getInstance().getMethodMap(),
                    MappingsManager.getInstance().getFieldMap());
            return new JsObjectWrapper(o, c, cm.methods, cm.fields);
        }
        return Value.asValue(o);
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
        for (Class<?> iface : cls.getInterfaces()) combineRecursive(iface, r2y, mMap, fMap, accMethods, accFields, seen);
        combineRecursive(cls.getSuperclass(), r2y, mMap, fMap, accMethods, accFields, seen);
    }
    public static void insertIntoPackageHierarchy(JsPackage root,
                                                  String fullYarnName,
                                                  LazyJsClassHolder holder) {
        String[] parts = fullYarnName.split("\\.");
        JsPackage current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String pkg = parts[i];
            Object existing = current.getMember(pkg);
            if (existing instanceof JsPackage p) {
                current = p;
            } else if (existing == null) {
                JsPackage next = new JsPackage();
                current.put(pkg, next);
                current = next;
            } else {
                return;
            }
        }
        String className = parts[parts.length - 1];
        current.put(className, holder);
    }

}

package net.me.scripting;

import net.me.Main;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsObjectWrapper implements ProxyObject {
    private final Object javaInstance;
    private final Class<?> instanceClass;
    private final MethodLookup methods;
    private final FieldLookup fields;

    public JsObjectWrapper(Object instance,
                           Class<?> cls,
                           Map<String, List<String>> methodMap,
                           Map<String, String> fieldMap) {
        if (instance == null) {
            throw new NullPointerException("Java instance cannot be null");
        }
        this.javaInstance = instance;
        this.instanceClass = (cls != null) ? cls : instance.getClass();
        this.methods = new MethodLookup(methodMap);
        this.fields = new FieldLookup(fieldMap);
    }

    @Override
    public Object getMember(String key) {
        if ("_self".equals(key)) {
            return this.getJavaInstance();
        }

        // Convention : si le nom se termine par '$', on force l'accès au champ
        if (key.endsWith("$")) {
            String fieldName = key.substring(0, key.length() - 1);
            return handleField(fieldName);
        }

        // Priorité normale : méthodes d'abord, puis champs
        Object mapped = handleMappedMethod(key);
        if (mapped != null) return mapped;
        Object direct = handleDirectMethod(key);
        if (direct != null) return direct;
        return handleField(key);
    }

    @Override
    public boolean hasMember(String key) {
        if ("_self".equals(key)) {
            return true;
        }

        // Gestion de la convention '$' pour forcer l'accès aux champs
        if (key.endsWith("$")) {
            String fieldName = key.substring(0, key.length() - 1);
            return fields.hasField(instanceClass, fieldName);
        }

        return methods.hasMapped(key)
                || MethodLookup.hasDirect(instanceClass, key)
                || fields.hasField(instanceClass,key);
    }

    @Override
    public Object getMemberKeys() {
        Set<String> keys = new HashSet<>(methods.methodKeys());
        keys.addAll(fields.fieldKeys());
        keys.add("_self");
        return keys.toArray(new String[0]);
    }

    @Override
    public void putMember(String key, Value value) {
        if (fields.hasField(instanceClass,key)) {
            writeField(key, value);
            return;
        }
        throw new UnsupportedOperationException("No writable member: " + key);
    }

    private Object handleMappedMethod(String key) {
        List<Method> candidates = methods.findMethods(instanceClass, key);
        if (!candidates.isEmpty()) {
            return (ProxyExecutable) args -> invokeMethods(candidates, args);
        }
        return null;
    }

    private Object handleDirectMethod(String key) {
        List<Method> direct = MethodLookup.findDirect(instanceClass, key);
        if (!direct.isEmpty()) {
            return (ProxyExecutable) args -> invokeMethods(direct, args);
        }
        return null;
    }

    private Object handleField(String key) {
        try {
            Field f = fields.accessField(instanceClass, key);
            if (Modifier.isStatic(f.getModifiers())) return null;
            return ScriptUtils.wrapReturn(f.get(javaInstance));
        } catch (NoSuchFieldException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Field access failed: " + key, e);
        }
    }

    private Object invokeMethods(List<Method> methods, Value[] args) {
        int count = args.length;
        for (Method m : methods) {
            if (m.getParameterCount() == count) {
                Object target = null;
                Object[] javaArgs = null;
                try {
                    target = ScriptUtils.unwrapReceiver(javaInstance);
                    javaArgs = ScriptUtils.unwrapArgs(args, m.getParameterTypes());
                    Object res = m.invoke(target, javaArgs);
                    return ScriptUtils.wrapReturn(res);
                } catch (Exception e) {
                    Main.LOGGER.error("Error invoking method {}.{} with {} args (actual exception below):", instanceClass.getName(), m.getName(), count);
                    Main.LOGGER.error("  -> Target: {}", target != null ? target.getClass().getName() : "null");
                    if (javaArgs != null) {
                        for (int i = 0; i < javaArgs.length; i++) {
                            Main.LOGGER.error("  -> Arg {}: {} (Type: {})", i, javaArgs[i], javaArgs[i] != null ? javaArgs[i].getClass().getName() : "null");
                        }
                    }
                    // Log the full stack trace of the actual exception 'e'
                    Main.LOGGER.error("Original Exception:", e);
                    throw new RuntimeException("Method invocation failed: " + m.getName(), e);
                }
            }
        }
        throw new RuntimeException("No overload for method with " + count + " args");
    }

    private void writeField(String key, Value value) {
        try {
            Field f = fields.accessField(instanceClass, key);
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers()))
                throw new UnsupportedOperationException("Cannot modify field: " + key);
            Object javaVal = ScriptUtils.unwrapArgs(new Value[]{value}, new Class[]{f.getType()})[0];
            f.set(javaInstance, javaVal);
        } catch (Exception e) {
            throw new RuntimeException("Field write failed: " + key, e);
        }
    }

    public static class MethodLookup {
        private final Map<String, List<String>> map;
        public MethodLookup(Map<String, List<String>> map) {
            this.map = map != null ? map : Collections.emptyMap();
        }
        public boolean hasMapped(String key) { return map.containsKey(key); }
        public Set<String> methodKeys() { return map.keySet(); }
        public List<Method> findMethods(Class<?> cls, String key) {
            List<String> names = map.getOrDefault(key, List.of());
            return ScriptUtils.findMethods(cls, names, false);
        }
        public static List<Method> findDirect(Class<?> cls, String key) {
            return ScriptUtils.findMethods(cls, List.of(key), false);
        }
        public static boolean hasDirect(Class<?> cls, String key) {
            return !findDirect(cls, key).isEmpty();
        }
    }

    public static class FieldLookup {
        private final Map<String, String> map;
        public FieldLookup(Map<String, String> map) {
            this.map = map != null ? map : Collections.emptyMap();
        }

        public boolean hasField(Class<?> cls,String key) {
            if (map.containsKey(key)) {
                return true;
            }
            try {
                ScriptUtils.findField(cls, key);
                return true;
            } catch (NoSuchFieldException e) {
                return false;
            }
        }

        public Set<String> fieldKeys() { return map.keySet(); }

        public Field accessField(Class<?> cls, String key) throws NoSuchFieldException {
            String runtimeName = map.get(key);
            if (runtimeName != null) {
                try {
                    Field f = ScriptUtils.findField(cls, runtimeName);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignored) {
                }
            }

            Field f = ScriptUtils.findField(cls, key);
            f.setAccessible(true);
            return f;
        }
    }

    public Object getJavaInstance() {
        return this.javaInstance;
    }
}
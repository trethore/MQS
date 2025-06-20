package net.me.scripting.utils;

import net.me.scripting.extenders.proxies.ExtendedInstanceProxy;
import net.me.scripting.extenders.proxies.MappedInstanceProxy;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.wrappers.JsObjectWrapper;
import org.graalvm.polyglot.Value;

import java.util.Map;


public final class ScriptUtils {

    private ScriptUtils() {
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
        if (v == null || v.isNull()) {
            return null;
        }

        Object potentialUnwrapped = unwrapReceiver(v);

        if (potentialUnwrapped != v && !(potentialUnwrapped instanceof Value)) {
            return potentialUnwrapped;
        }

        if (expected != null) {
            try {
                return v.as(expected);
            } catch (Exception ignored) {
            }
        }

        if (v.isBoolean()) return v.asBoolean();
        if (v.isString()) return v.asString();
        if (v.isNumber()) return convertNumber(v, expected);
        if (v.isHostObject()) return v.asHostObject();
        if (v.isProxyObject()) return v.asProxyObject();

        return v;
    }

    public static Object unwrapReceiver(Object o) {
        if (o == null) return null;

        Object current = o;
        if (current instanceof Value val) {
            if (val.isHostObject()) return val.asHostObject();
            if (val.isProxyObject()) current = val.asProxyObject();
            else return o;
        }

        while (true) {
            if (current instanceof ExtendedInstanceProxy proxy) {
                current = proxy.getBaseInstance();
                continue;
            }
            if (current instanceof MappedInstanceProxy proxy) {
                current = proxy.getInstance();
                continue;
            }
            if (current instanceof JsObjectWrapper wrapper) {
                current = wrapper.getJavaInstance();
                continue;
            }
            break;
        }

        return current;
    }

    private static Object convertNumber(Value v, Class<?> expected) {
        if (expected == int.class || expected == Integer.class) return v.asInt();
        if (expected == long.class || expected == Long.class) return v.asLong();
        if (expected == float.class || expected == Float.class) return v.asFloat();
        return v.asDouble();
    }

    public static Object wrapReturn(Object o) {
        if (o == null || o instanceof String || o instanceof Number || o instanceof Boolean) return o;
        Class<?> c = o.getClass();
        Map<String, String> runtimeToYarn = MappingsManager.getInstance().getRuntimeToYarnClassMap();
        if (runtimeToYarn.containsKey(c.getName()) || c.isArray()) {
            MappingUtils.ClassMappings cm = MappingUtils.combineMappings(c, runtimeToYarn,
                    MappingsManager.getInstance().getMethodMap(),
                    MappingsManager.getInstance().getFieldMap());
            return new JsObjectWrapper(o, c, cm.methods(), cm.fields());
        }
        return Value.asValue(o);
    }
}
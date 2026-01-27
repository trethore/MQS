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

import net.me.scripting.ScriptManager;
import net.me.scripting.engine.ScriptingClassResolver;
import net.me.scripting.extenders.proxies.ExtendedInstanceProxy;
import net.me.scripting.extenders.proxies.MappedInstanceProxy;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.wrappers.JsObjectWrapper;
import org.graalvm.polyglot.Value;

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

        if (v.isNumber() && Object.class.equals(expected)) {
            return v.asDouble();
        }

        Object potentialUnwrapped = unwrapReceiver(v);

        if (potentialUnwrapped != v && !(potentialUnwrapped instanceof Value)) {
            return potentialUnwrapped;
        }

        if (expected != null) {
            try {
                return v.as(expected);
            } catch (Exception _) {
                // Conversion failed, fall through to type-specific handling below
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
        if (o == null) {
            return null;
        }

        Object current = unwrapValue(o);
        if (current == null) {
            return null;
        }

        Object unwrapped = tryUnwrapProxy(current);
        while (unwrapped != current) {
            current = unwrapped;
            unwrapped = tryUnwrapProxy(current);
        }

        return current;
    }

    private static Object unwrapValue(Object o) {
        if (!(o instanceof Value val)) {
            return o;
        }
        if (val.isNull()) {
            return null;
        }
        if (val.isHostObject()) {
            return val.asHostObject();
        }
        if (val.isProxyObject()) {
            return val.asProxyObject();
        }
        return o;
    }

    private static Object tryUnwrapProxy(Object current) {
        if (current instanceof ExtendedInstanceProxy proxy) {
            return proxy.getBaseInstance();
        }
        if (current instanceof MappedInstanceProxy proxy) {
            return proxy.getInstance();
        }
        if (current instanceof JsObjectWrapper wrapper) {
            return wrapper.getJavaInstance();
        }
        return current;
    }

    private static Object convertNumber(Value v, Class<?> expected) {
        if (double.class.equals(expected)) {
            return v.asDouble();
        }
        if (int.class.equals(expected)) {
            return v.asInt();
        }
        if (long.class.equals(expected)) {
            return v.asLong();
        }
        if (float.class.equals(expected)) {
            return v.asFloat();
        }

        return v.asDouble();
    }

    public static Object wrapReturn(Object o, MappingsManager mappingsManager, ScriptManager scriptManager) {
        if (o == null || o instanceof String || o instanceof Number || o instanceof Boolean) {
            return o;
        }

        if (mappingsManager == null || !mappingsManager.isReady()) {
            return o;
        }

        ScriptingClassResolver classResolver = scriptManager.getClassResolver();
        Class<?> c = o.getClass();

        if (!classResolver.isMcRelated(c)) {
            return o;
        }

        MappingUtils.ClassMappings cm = MappingUtils.combineMappings(c,
                mappingsManager.getRuntimeToYarnClassMap(),
                mappingsManager.getMethodMap(),
                mappingsManager.getFieldMap());

        return new JsObjectWrapper(o, c, cm.methods(), cm.fields(), mappingsManager, scriptManager);
    }
}

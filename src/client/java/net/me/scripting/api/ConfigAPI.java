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

package net.me.scripting.api;

import net.me.scripting.ConfigManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.api.internal.ScriptContextHelper;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.me.scripting.api.ApiConstants.*;

public class ConfigAPI implements ProxyObject {

    private static final Set<String> MEMBER_KEYS = Set.of(
            GET,
            SET,
            HAS,
            SAVE,
            LOAD,
            GET_ALL,
            GET_BOOL,
            GET_NUMBER,
            GET_STRING
    );
    private final ConfigManager configManager;
    private final ScriptContextHelper contextHelper;

    public ConfigAPI(ConfigManager configManager, ScriptManager scriptManager) {
        this.configManager = configManager;
        this.contextHelper = new ScriptContextHelper(scriptManager);
    }

    public static Object toSerializableObject(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isNumber()) {
            return value.as(Number.class);
        }
        if (value.hasArrayElements()) {
            List<Object> javaList = new ArrayList<>();
            for (int i = 0; i < value.getArraySize(); i++) {
                javaList.add(toSerializableObject(value.getArrayElement(i)));
            }
            return javaList;
        }
        if (value.isHostObject()) {
            return value.asHostObject();
        }
        if (value.hasMembers() || value.isProxyObject()) {
            Map<String, Object> javaMap = new LinkedHashMap<>();
            for (String k : value.getMemberKeys()) {
                javaMap.put(k, toSerializableObject(value.getMember(k)));
            }
            return javaMap;
        }
        return value;
    }

    @Override
    public Object getMember(String key) {
        return (ProxyExecutable) args -> {
            RunningScript script = contextHelper.require("Config API");
            switch (key) {
                case GET: {
                    ApiArgumentChecks.requireArgCountAtLeast(args, 1, "Config.get requires at least one argument (key).");
                    String configKey = args[0].asString();
                    Object result = configManager.get(script.getId(), configKey);
                    if (result == null) {
                        return args.length > 1 ? args[1] : null;
                    }
                    return script.getContext().asValue(result);
                }
                case SET: {
                    ApiArgumentChecks.requireArgCount(args, 2, "Config.set requires two arguments (key, value).");
                    String configKey = args[0].asString();
                    Object value = toSerializableObject(args[1]);
                    configManager.set(script.getId(), configKey, value);
                    return null;
                }
                case GET_BOOL: {
                    ApiArgumentChecks.requireArgCountAtLeast(args, 1, "Config.getBool requires at least one argument (key).");
                    boolean defaultValue = args.length > 1 && coerceBoolean(args[1]);
                    Object stored = readRaw(script, args, defaultValue);
                    if (stored instanceof Boolean value) {
                        return value;
                    }
                    if (stored instanceof Number number) {
                        return number.intValue() != 0;
                    }
                    if (stored instanceof String text) {
                        return Boolean.parseBoolean(text);
                    }
                    return defaultValue;
                }
                case GET_NUMBER: {
                    ApiArgumentChecks.requireArgCountAtLeast(args, 1, "Config.getNumber requires at least one argument (key).");
                    double defaultValue = args.length > 1 ? coerceNumber(args[1]) : 0D;
                    Object stored = readRaw(script, args, defaultValue);
                    if (stored instanceof Number number) {
                        return number.doubleValue();
                    }
                    if (stored instanceof String text) {
                        try {
                            return Double.parseDouble(text);
                        } catch (NumberFormatException _) {
                            return defaultValue;
                        }
                    }
                    return defaultValue;
                }
                case GET_STRING: {
                    ApiArgumentChecks.requireArgCountAtLeast(args, 1, "Config.getString requires at least one argument (key).");
                    String defaultValue = args.length > 1 && args[1] != null ? args[1].toString() : null;
                    Object stored = readRaw(script, args, defaultValue);
                    return stored != null ? stored.toString() : defaultValue;
                }
                case HAS: {
                    ApiArgumentChecks.requireArgCount(args, 1, "Config.has requires one argument (key).");
                    String configKey = args[0].asString();
                    return configManager.get(script.getId(), configKey) != null;
                }
                case SAVE: {
                    ApiArgumentChecks.requireArgCount(args, 0, "Config.save takes no arguments.");
                    configManager.saveConfig(script);
                    return null;
                }
                case LOAD: {
                    ApiArgumentChecks.requireArgCount(args, 0, "Config.load takes no arguments.");
                    configManager.unloadConfig(script);
                    configManager.getConfigForScript(script);
                    return null;
                }
                case GET_ALL: {
                    ApiArgumentChecks.requireArgCount(args, 0, "Config.getAll takes no arguments.");
                    return configManager.getConfigForScript(script);
                }
                default:
                    throw new UnsupportedOperationException("Unsupported Config operation: " + key);
            }
        };
    }

    private Object readRaw(RunningScript script, Value[] args, Object defaultValue) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Config helpers require a key.");
        }
        if (!args[0].isString()) {
            throw new IllegalArgumentException("Config key must be a string.");
        }
        Object stored = configManager.get(script.getId(), args[0].asString());
        return stored != null ? stored : defaultValue;
    }

    private boolean coerceBoolean(Value value) {
        if (value == null) {
            return false;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isNumber()) {
            return value.asInt() != 0;
        }
        if (value.isString()) {
            return Boolean.parseBoolean(value.asString());
        }
        return false;
    }

    private double coerceNumber(Value value) {
        if (value == null) {
            return 0D;
        }
        if (value.isNumber()) {
            return value.asDouble();
        }
        if (value.isString()) {
            try {
                return Double.parseDouble(value.asString());
            } catch (NumberFormatException _) {
                return 0D;
            }
        }
        return 0D;
    }

    @Override
    public Object getMemberKeys() {
        return MEMBER_KEYS.toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        return MEMBER_KEYS.contains(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify the Config object itself.");
    }
}

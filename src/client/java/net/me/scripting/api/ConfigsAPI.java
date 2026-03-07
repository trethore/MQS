/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Réthoré
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
import net.me.scripting.typings.MqsApiFragment;
import net.me.scripting.typings.TypingsConstants;
import net.me.scripting.typings.schema.TsObject;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.*;

import static net.me.scripting.typings.schema.TsDescriptors.*;

public class ConfigsAPI implements ProxyObject {

    private static final String GET = "get";
    private static final String SET = "set";
    private static final String HAS = "has";
    private static final String SAVE = "save";
    private static final String LOAD = "load";
    private static final String GET_ALL = "getAll";
    private static final String GET_BOOL = "getBool";
    private static final String GET_NUMBER = "getNumber";
    private static final String GET_STRING = "getString";

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

    public ConfigsAPI(ConfigManager configManager, ScriptManager scriptManager) {
        this.configManager = configManager;
        this.contextHelper = new ScriptContextHelper(scriptManager);
    }

    public static MqsApiFragment describeTypeScript() {
        return new MqsApiFragment(
                List.of(),
                List.of(),
                List.of(),
                List.of(describeApi())
        );
    }

    private static TsObject describeApi() {
        return new TsObject(
                "MQSConfigApi",
                List.of(
                        method(GET, fn(TypingsConstants.UNKNOWN, p("key", TypingsConstants.STRING), opt(TypingsConstants.DEFAULT_VALUE, TypingsConstants.UNKNOWN))),
                        method(SET, fn(TypingsConstants.VOID, p("key", TypingsConstants.STRING), p("value", TypingsConstants.UNKNOWN))),
                        method(HAS, fn(TypingsConstants.BOOLEAN, p("key", TypingsConstants.STRING))),
                        method(SAVE, fn(TypingsConstants.VOID)),
                        method(LOAD, fn(TypingsConstants.VOID)),
                        method(GET_ALL, fn("Record<string, unknown>")),
                        method(GET_BOOL, fn(TypingsConstants.BOOLEAN, p("key", TypingsConstants.STRING), opt(TypingsConstants.DEFAULT_VALUE, TypingsConstants.BOOLEAN))),
                        method(GET_NUMBER, fn(TypingsConstants.NUMBER, p("key", TypingsConstants.STRING), opt(TypingsConstants.DEFAULT_VALUE, TypingsConstants.NUMBER))),
                        method(GET_STRING, fn(TypingsConstants.STRING + " | null", p("key", TypingsConstants.STRING), opt(TypingsConstants.DEFAULT_VALUE, TypingsConstants.STRING)))
                )
        );
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
        return switch (key) {
            case GET -> (ProxyExecutable) this::handleGet;
            case SET -> (ProxyExecutable) this::handleSet;
            case GET_BOOL -> (ProxyExecutable) this::handleGetBool;
            case GET_NUMBER -> (ProxyExecutable) this::handleGetNumber;
            case GET_STRING -> (ProxyExecutable) this::handleGetString;
            case HAS -> (ProxyExecutable) this::handleHas;
            case SAVE -> (ProxyExecutable) this::handleSave;
            case LOAD -> (ProxyExecutable) this::handleLoad;
            case GET_ALL -> (ProxyExecutable) this::handleGetAll;
            default -> unsupportedOperation(key);
        };
    }

    private Object handleGet(Value[] args) {
        ApiArgumentChecks.requireArgCountAtLeast(args, 1, "Config.get requires at least one argument (key).");
        RunningScript script = requireScript();
        Object result = configManager.get(script.getId(), requireConfigKey(args));
        if (result == null) {
            return args.length > 1 ? args[1] : null;
        }
        return script.getContext().asValue(result);
    }

    private Object handleSet(Value[] args) {
        ApiArgumentChecks.requireArgCount(args, 2, "Config.set requires two arguments (key, value).");
        RunningScript script = requireScript();
        String configKey = requireConfigKey(args);
        Object value = toSerializableObject(args[1]);
        configManager.set(script.getId(), configKey, value);
        return null;
    }

    private Object handleGetBool(Value[] args) {
        ApiArgumentChecks.requireArgCountAtLeast(args, 1, "Config.getBool requires at least one argument (key).");
        RunningScript script = requireScript();
        String configKey = requireConfigKey(args);
        boolean defaultValue = args.length > 1 && coerceBoolean(args[1]);
        Object stored = readRaw(script, configKey, defaultValue);
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

    private Object handleGetNumber(Value[] args) {
        ApiArgumentChecks.requireArgCountAtLeast(args, 1, "Config.getNumber requires at least one argument (key).");
        RunningScript script = requireScript();
        String configKey = requireConfigKey(args);
        double defaultValue = args.length > 1 ? coerceNumber(args[1]) : 0D;
        Object stored = readRaw(script, configKey, defaultValue);
        if (stored instanceof Number number) {
            return number.doubleValue();
        }
        if (stored instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                // String value cannot be converted to a number; use the fallback default.
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Object handleGetString(Value[] args) {
        ApiArgumentChecks.requireArgCountAtLeast(args, 1, "Config.getString requires at least one argument (key).");
        RunningScript script = requireScript();
        String configKey = requireConfigKey(args);
        String defaultValue = args.length > 1 && args[1] != null ? args[1].toString() : null;
        Object stored = readRaw(script, configKey, defaultValue);
        return stored != null ? stored.toString() : defaultValue;
    }

    private Object handleHas(Value[] args) {
        ApiArgumentChecks.requireArgCount(args, 1, "Config.has requires one argument (key).");
        RunningScript script = requireScript();
        return configManager.get(script.getId(), requireConfigKey(args)) != null;
    }

    private Object handleSave(Value[] args) {
        ApiArgumentChecks.requireArgCount(args, 0, "Config.save takes no arguments.");
        configManager.saveConfig(requireScript());
        return null;
    }

    private Object handleLoad(Value[] args) {
        ApiArgumentChecks.requireArgCount(args, 0, "Config.load takes no arguments.");
        RunningScript script = requireScript();
        configManager.unloadConfig(script);
        configManager.getConfigForScript(script);
        return null;
    }

    private Object handleGetAll(Value[] args) {
        ApiArgumentChecks.requireArgCount(args, 0, "Config.getAll takes no arguments.");
        return configManager.getConfigForScript(requireScript());
    }

    private ProxyExecutable unsupportedOperation(String key) {
        return ignored -> {
            throw new UnsupportedOperationException("Unsupported Config operation: " + key);
        };
    }

    private RunningScript requireScript() {
        return contextHelper.require("Config API");
    }

    private String requireConfigKey(Value[] args) {
        Value keyValue = args[0];
        if (keyValue == null || !keyValue.isString()) {
            throw new IllegalArgumentException("Config key must be a string.");
        }
        return keyValue.asString();
    }

    private Object readRaw(RunningScript script, String configKey, Object defaultValue) {
        Object stored = configManager.get(script.getId(), configKey);
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
            } catch (NumberFormatException ignored) {
                // Input cannot be parsed to a number; return the numeric fallback.
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

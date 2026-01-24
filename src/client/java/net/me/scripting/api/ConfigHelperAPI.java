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
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Set;

import static net.me.scripting.api.ApiConstants.*;

public class ConfigHelperAPI implements ProxyObject {
    private static final Set<String> MEMBER_KEYS = Set.of(GET, SET, GET_BOOL, GET_NUMBER, GET_STRING, HAS, SAVE, LOAD, GET_ALL);

    private final ConfigManager configManager;
    private final ScriptManager scriptManager;

    public ConfigHelperAPI(ConfigManager configManager, ScriptManager scriptManager) {
        this.configManager = configManager;
        this.scriptManager = scriptManager;
    }

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case GET -> (ProxyExecutable) args -> {
                if (args.length == 0) {
                    throw new IllegalArgumentException("Config.get requires a key.");
                }
                RunningScript script = getCurrentScript();
                Object stored = configManager.get(script.getId(), args[0].asString());
                if (stored == null) {
                    return args.length > 1 ? args[1] : null;
                }
                return script.getContext().asValue(stored);
            };
            case SET -> (ProxyExecutable) args -> {
                if (args.length != 2) {
                    throw new IllegalArgumentException("Config.set requires a key and value.");
                }
                RunningScript script = getCurrentScript();
                Object serialized = ConfigAPI.toSerializableObject(args[1]);
                configManager.set(script.getId(), args[0].asString(), serialized);
                return null;
            };
            case GET_BOOL -> (ProxyExecutable) args -> {
                boolean defaultValue = args.length > 1 && coerceBoolean(args[1]);
                Object stored = readRaw(args, defaultValue);
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
            };
            case GET_NUMBER -> (ProxyExecutable) args -> {
                double defaultValue = args.length > 1 ? coerceNumber(args[1]) : 0D;
                Object stored = readRaw(args, defaultValue);
                if (stored instanceof Number number) {
                    return number.doubleValue();
                }
                if (stored instanceof String text) {
                    try {
                        return Double.parseDouble(text);
                    } catch (NumberFormatException _) {
                        // Configured value is not a valid number string
                    }
                }
                return defaultValue;
            };
            case GET_STRING -> (ProxyExecutable) args -> {
                String defaultValue = args.length > 1 && args[1] != null ? args[1].toString() : null;
                Object stored = readRaw(args, defaultValue);
                return stored != null ? stored.toString() : defaultValue;
            };
            case HAS -> (ProxyExecutable) args -> {
                if (args.length != 1) {
                    throw new IllegalArgumentException("Config.has requires a key.");
                }
                RunningScript script = getCurrentScript();
                return configManager.get(script.getId(), args[0].asString()) != null;
            };
            case SAVE -> (ProxyExecutable) args -> {
                if (args.length != 0) {
                    throw new IllegalArgumentException("Config.save takes no arguments.");
                }
                configManager.saveConfig(getCurrentScript());
                return null;
            };
            case LOAD -> (ProxyExecutable) args -> {
                if (args.length != 0) {
                    throw new IllegalArgumentException("Config.load takes no arguments.");
                }
                RunningScript script = getCurrentScript();
                configManager.unloadConfig(script);
                configManager.getConfigForScript(script);
                return null;
            };
            case GET_ALL -> (ProxyExecutable) args -> {
                if (args.length != 0) {
                    throw new IllegalArgumentException("Config.getAll takes no arguments.");
                }
                RunningScript script = getCurrentScript();
                return configManager.getConfigForScript(script);
            };
            default -> null;
        };
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
        throw new UnsupportedOperationException("Cannot modify MQS.config.");
    }

    private RunningScript getCurrentScript() {
        RunningScript script = scriptManager.getCurrentScript();
        if (script == null) {
            throw new IllegalStateException("Config helpers can only be used from an active script.");
        }
        return script;
    }

    private Object readRaw(Value[] args, Object defaultValue) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Config helpers require a key.");
        }
        if (!args[0].isString()) {
            throw new IllegalArgumentException("Config key must be a string.");
        }
        RunningScript script = getCurrentScript();
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
                // Not a valid number string
            }
        }
        return 0D;
    }
}

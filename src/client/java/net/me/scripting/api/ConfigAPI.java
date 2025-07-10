package net.me.scripting.api;

import net.me.scripting.ConfigManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigAPI implements ProxyObject {

    private final ConfigManager configManager;
    private final ScriptManager scriptManager;

    public ConfigAPI(ConfigManager configManager, ScriptManager scriptManager) {
        this.configManager = configManager;
        this.scriptManager = scriptManager;
    }

    private RunningScript getCurrentScript() {
        RunningScript script = scriptManager.getCurrentScript();
        if (script == null) {
            throw new IllegalStateException("Config API can only be used within a running script context (e.g., onEnable, onDisable, or an event).");
        }
        return script;
    }

    private static Object toSerializableObject(Value value) {
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
            RunningScript script = getCurrentScript();
            switch (key) {
                case "get": {
                    if (args.length == 0)
                        throw new IllegalArgumentException("Config.get requires at least one argument (key).");
                    String configKey = args[0].asString();
                    Object result = configManager.get(script.getId(), configKey);
                    if (result == null) {
                        return args.length > 1 ? args[1] : script.getContext().eval("js", "null");
                    }
                    return script.getContext().asValue(result);
                }
                case "set": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("Config.set requires two arguments (key, value).");
                    String configKey = args[0].asString();
                    Object value = toSerializableObject(args[1]);
                    configManager.set(script.getId(), configKey, value);
                    return null;
                }
                case "has": {
                    if (args.length != 1)
                        throw new IllegalArgumentException("Config.has requires one argument (key).");
                    String configKey = args[0].asString();
                    return configManager.get(script.getId(), configKey) != null;
                }
                case "save": {
                    if (args.length != 0)
                        throw new IllegalArgumentException("Config.save takes no arguments.");
                    configManager.saveConfig(script);
                    return null;
                }
                case "load": {
                    if (args.length != 0)
                        throw new IllegalArgumentException("Config.load takes no arguments.");
                    configManager.unloadConfig(script);
                    configManager.getConfigForScript(script);
                    return null;
                }
                case "getAll": {
                    if (args.length != 0)
                        throw new IllegalArgumentException("Config.getAll takes no arguments.");
                    return configManager.getConfigForScript(script);
                }
                default:
                    throw new UnsupportedOperationException("Unsupported Config operation: " + key);
            }
        };
    }

    @Override
    public Object getMemberKeys() {
        return new String[]{"get", "set", "has", "save", "load", "getAll"};
    }

    @Override
    public boolean hasMember(String key) {
        return "get".equals(key) || "set".equals(key) || "has".equals(key) || "save".equals(key) || "load".equals(key) || "getAll".equals(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify the Config object itself.");
    }
}
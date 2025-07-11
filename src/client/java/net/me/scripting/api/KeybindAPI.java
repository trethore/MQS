package net.me.scripting.api;

import net.me.keybinds.KeybindManager;
import net.me.keybinds.Keys;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Arrays;

import static net.me.scripting.api.ApiConstants.*;

public class KeybindAPI implements ProxyObject {

    private static final ProxyObject KEYS_PROXY = new ProxyObject() {
        @Override
        public Object getMember(String key) {
            return Arrays.stream(Keys.values())
                    .filter(k -> k.name().equalsIgnoreCase(key))
                    .findFirst()
                    .map(Keys::getCode)
                    .orElse(null);
        }

        @Override
        public Object getMemberKeys() {
            return Arrays.stream(Keys.values()).map(Enum::name).toArray(String[]::new);
        }

        @Override
        public boolean hasMember(String key) {
            return Arrays.stream(Keys.values()).anyMatch(k -> k.name().equalsIgnoreCase(key));
        }

        @Override
        public void putMember(String key, Value value) {
            throw new UnsupportedOperationException("Cannot modify the Keys enum object.");
        }
    };

    private final KeybindManager keybindManager;
    private final ScriptManager scriptManager;

    public KeybindAPI(KeybindManager keybindManager, ScriptManager scriptManager) {
        this.keybindManager = keybindManager;
        this.scriptManager = scriptManager;
    }

    private RunningScript getCurrentScript() {
        RunningScript script = scriptManager.getCurrentScript();
        if (script == null) {
            throw new IllegalStateException("KeybindAPI can only be used within a running script context.");
        }
        return script;
    }

    @Override
    public Object getMember(String key) {
        if (KEYS.equalsIgnoreCase(key)) {
            return KEYS_PROXY;
        }

        return (ProxyExecutable) args -> {
            RunningScript owner = getCurrentScript();
            switch (key) {
                case REGISTER: {
                    if (args.length < 2 || !args[0].isString() || !args[1].canExecute()) {
                        throw new IllegalArgumentException("Usage: KeybindManager.register('name', actionFunction, { key, repeatable, debounce })");
                    }

                    String name = args[0].asString();
                    Value action = args[1];

                    Value options = (args.length > 2 && args[2] != null && args[2].hasMembers()) ? args[2] : null;

                    keybindManager.register(name, action, owner, options);
                    return null;
                }
                case UNREGISTER: {
                    if (args.length != 1 || !args[0].isString()) {
                        throw new IllegalArgumentException("Usage: Keybinds.unregister('name')");
                    }
                    String name = args[0].asString();
                    keybindManager.unregister(owner, name);
                    return null;
                }
                default:
                    throw new UnsupportedOperationException("Unsupported Keybinds operation: " + key);
            }
        };
    }

    @Override
    public Object getMemberKeys() {
        return new String[]{REGISTER, UNREGISTER, KEYS};
    }

    @Override
    public boolean hasMember(String key) {
        return REGISTER.equals(key) || UNREGISTER.equals(key) || KEYS.equalsIgnoreCase(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify the Keybinds API object.");
    }
}
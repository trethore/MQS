package net.me.scripting.keybinds;

import net.me.keybinds.KeybindManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

public class KeybindAPI implements ProxyObject {
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
        return (ProxyExecutable) args -> {
            RunningScript owner = getCurrentScript();
            switch (key) {
                case "register": {
                    if (args.length < 3 || !args[0].isString() || !args[1].isNumber() || !args[2].canExecute()) {
                        throw new IllegalArgumentException("Usage: KeybindManager.register('name', keyCode, action, isRepeatable = false, debounceMs = 100)");
                    }
                    String name = args[0].asString();
                    int keyCode = args[1].asInt();
                    Value action = args[2];
                    boolean repeatable = args.length > 3 && args[3].isBoolean() && args[3].asBoolean();
                    int debounceTime = args.length > 4 && args[4].isNumber() ? args[4].asInt() : 100;

                    keybindManager.register(name, keyCode, repeatable, owner, action, debounceTime);
                    return null;
                }
                case "unregister": {
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
        return new String[]{"register", "unregister"};
    }

    @Override
    public boolean hasMember(String key) {
        return "register".equals(key) || "unregister".equals(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify the Keybinds API object.");
    }
}
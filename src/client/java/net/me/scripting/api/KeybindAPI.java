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

import net.me.keybinds.KeybindManager;
import net.me.keybinds.KeybindOptions;
import net.me.keybinds.Keys;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Arrays;
import java.util.Set;

import static net.me.scripting.api.ApiConstants.*;

public class KeybindAPI implements ProxyObject {

    private static final String REGISTER_USAGE = "Usage: KeybindManager.register('name', actionFunction, { key, repeatable, debounce })";
    private static final Set<String> MEMBER_KEYS = Set.of(REGISTER, UNREGISTER, UNREGISTER_ALL, KEYS);
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
                    ApiArgumentChecks.requireArgCountAtLeast(args, 2, REGISTER_USAGE);
                    String name = ApiArgumentChecks.requireString(args, 0, REGISTER_USAGE);
                    Value action = ApiArgumentChecks.requireExecutable(args, 1, REGISTER_USAGE);

                    Value optionsArg = args.length > 2 ? args[2] : null;
                    KeybindOptions options = KeybindOptions.fromScript(optionsArg, Keys.UNBOUND.getCode());
                    keybindManager.register(name, action, owner, options);
                    return null;
                }
                case UNREGISTER: {
                    ApiArgumentChecks.requireArgCount(args, 1, "Usage: KeybindManager.unregister('name')");
                    String name = ApiArgumentChecks.requireString(args, 0, "Usage: KeybindManager.unregister('name')");
                    keybindManager.unregister(owner, name);
                    return null;
                }
                case UNREGISTER_ALL: {
                    ApiArgumentChecks.requireArgCount(args, 0, "Usage: KeybindManager.unregisterAll()");
                    keybindManager.unregister(owner);
                    return null;
                }
                default:
                    throw new UnsupportedOperationException("Unsupported KeybindManager operation: " + key);
            }
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
        throw new UnsupportedOperationException("Cannot modify the KeybindManager API object.");
    }
}

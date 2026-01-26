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
import java.util.concurrent.atomic.AtomicBoolean;

import static net.me.scripting.api.ApiConstants.*;

public class KeybindsAPI implements ProxyObject {
    private static final Set<String> MEMBER_KEYS = Set.of(
            KEYBIND_BIND,
            KEYBIND_BIND_TOGGLE,
            KEYBIND_UNBIND,
            KEYBIND_UNBIND_ALL,
            KEY_KEYS,
            OPTIONS
    );

    private final KeybindManager keybindManager;
    private final ScriptManager scriptManager;
    private final ProxyObject keysProxy = createKeysProxy();

    public KeybindsAPI(KeybindManager keybindManager, ScriptManager scriptManager) {
        this.keybindManager = keybindManager;
        this.scriptManager = scriptManager;
    }

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case KEY_KEYS -> keysProxy;
            case OPTIONS -> (ProxyExecutable) _ -> getCurrentScript().getContext().asValue(KeybindOptions.builder());
            case KEYBIND_BIND -> (ProxyExecutable) args -> {
                ApiArgumentChecks.requireArgCountAtLeast(args, 3, "Usage: MQS.keybinds.bind(name, key, handler, options?)");
                RunningScript owner = getCurrentScript();
                String name = ApiArgumentChecks.requireString(args, 0, "Keybind name must be a string.");
                int keyCode = extractKeyCode(args[1]);
                Value handler = ApiArgumentChecks.requireExecutable(args, 2, "Handler must be executable.");

                KeybindOptions options = resolveOptions(args.length > 3 ? args[3] : null, keyCode);

                return registerKeybind(owner, name, handler, options);
            };
            case KEYBIND_BIND_TOGGLE -> (ProxyExecutable) args -> {
                ApiArgumentChecks.requireArgCountAtLeast(args, 3, "Usage: MQS.keybinds.bindToggle(name, key, handler, options?)");
                RunningScript owner = getCurrentScript();
                String name = ApiArgumentChecks.requireString(args, 0, "Keybind name must be a string.");
                int keyCode = extractKeyCode(args[1]);
                Value handler = ApiArgumentChecks.requireExecutable(args, 2, "Handler must be executable.");

                AtomicBoolean state = new AtomicBoolean(false);
                ProxyExecutable toggleExecutable = _ -> {
                    boolean next = !state.get();
                    state.set(next);
                    handler.execute(next);
                    return null;
                };

                Value toggleHandler = owner.getContext().asValue(toggleExecutable);
                KeybindOptions options = resolveOptions(args.length > 3 ? args[3] : null, keyCode);

                return registerKeybind(owner, name, toggleHandler, options);
            };
            case KEYBIND_UNBIND -> (ProxyExecutable) args -> {
                ApiArgumentChecks.requireArgCount(args, 1, "Usage: MQS.keybinds.unbind(name)");
                String name = ApiArgumentChecks.requireString(args, 0, "Usage: MQS.keybinds.unbind(name)");
                RunningScript owner = getCurrentScript();
                keybindManager.unregister(owner, name);
                return null;
            };
            case KEYBIND_UNBIND_ALL -> (ProxyExecutable) args -> {
                ApiArgumentChecks.requireArgCount(args, 0, "Usage: MQS.keybinds.unbindAll()");
                RunningScript owner = getCurrentScript();
                keybindManager.unregister(owner);
                return null;
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
        throw new UnsupportedOperationException("Cannot modify MQS.keybinds.");
    }

    private RunningScript getCurrentScript() {
        RunningScript script = scriptManager.getCurrentScript();
        if (script == null) {
            throw new IllegalStateException("Keybind helpers can only be used from an active script.");
        }
        return script;
    }

    private int extractKeyCode(Value keyValue) {
        if (keyValue == null) {
            throw new IllegalArgumentException("Key cannot be null.");
        }
        if (keyValue.isNumber()) {
            return keyValue.asInt();
        }
        if (keyValue.isHostObject() && keyValue.asHostObject() instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Key must be a numeric code or retrieved from MQS.keybinds.keys.");
    }

    private KeybindOptions resolveOptions(Value optionsArg, int keyCode) {
        if (optionsArg == null) {
            return KeybindOptions.builder().keyCode(keyCode).build();
        }
        if (optionsArg.isHostObject()) {
            Object host = optionsArg.asHostObject();
            if (host instanceof KeybindOptions options) {
                return options.withKeyCode(keyCode);
            }
            if (host instanceof KeybindOptions.Builder builder) {
                builder.key(keyCode);
                return builder.build();
            }
        }
        KeybindOptions parsed = KeybindOptions.fromScript(optionsArg, keyCode);
        return parsed.withKeyCode(keyCode);
    }

    private Value registerKeybind(RunningScript owner, String name, Value handler, KeybindOptions options) {
        keybindManager.register(name, handler, owner, options);
        AtomicBoolean disposed = new AtomicBoolean(false);
        ProxyExecutable exec = _ -> {
            if (disposed.compareAndSet(false, true)) {
                keybindManager.unregister(owner, name);
            }
            return null;
        };
        return owner.getContext().asValue(exec);
    }

    private ProxyObject createKeysProxy() {
        return new ProxyObject() {
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
                throw new UnsupportedOperationException("Cannot modify MQS.keybinds.keys.");
            }
        };
    }
}

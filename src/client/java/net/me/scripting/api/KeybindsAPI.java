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

import net.me.config.ConfigKeys;
import net.me.keybinds.KeybindManager;
import net.me.keybinds.Keys;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class KeybindsAPI implements ProxyObject {
    private static final Set<String> MEMBER_KEYS = Set.of("bind", "bindToggle", "unbind", "unbindAll", "keys", "options");

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
            case "keys" -> keysProxy;
            case "options" -> {
                RunningScript owner = getCurrentScript();
                yield owner.getContext().asValue(new KeybindOptionsBuilder(owner));
            }
            case "bind" -> (ProxyExecutable) args -> {
                if (args.length < 3) {
                    throw new IllegalArgumentException("Usage: MQS.keybinds.bind(name, key, handler, options?)");
                }
                RunningScript owner = getCurrentScript();
                if (!args[0].isString()) {
                    throw new IllegalArgumentException("Keybind name must be a string.");
                }
                String name = args[0].asString();
                int keyCode = extractKeyCode(args[1]);
                Value handler = args[2];
                if (!handler.canExecute()) {
                    throw new IllegalArgumentException("Handler must be executable.");
                }

                Value options = buildOptions(owner, args.length > 3 ? args[3] : null, keyCode);

                return registerKeybind(owner, name, handler, options);
            };
            case "bindToggle" -> (ProxyExecutable) args -> {
                if (args.length < 3) {
                    throw new IllegalArgumentException("Usage: MQS.keybinds.bindToggle(name, key, handler, options?)");
                }
                RunningScript owner = getCurrentScript();
                if (!args[0].isString()) {
                    throw new IllegalArgumentException("Keybind name must be a string.");
                }
                String name = args[0].asString();
                int keyCode = extractKeyCode(args[1]);
                Value handler = args[2];
                if (!handler.canExecute()) {
                    throw new IllegalArgumentException("Handler must be executable.");
                }

                AtomicBoolean state = new AtomicBoolean(false);
                ProxyExecutable toggleExecutable = execArgs -> {
                    boolean next = !state.get();
                    state.set(next);
                    handler.execute(next);
                    return null;
                };

                Value toggleHandler = owner.getContext().asValue(toggleExecutable);
                Value options = buildOptions(owner, args.length > 3 ? args[3] : null, keyCode);

                return registerKeybind(owner, name, toggleHandler, options);
            };
            case "unbind" -> (ProxyExecutable) args -> {
                if (args.length != 1 || !args[0].isString()) {
                    throw new IllegalArgumentException("Usage: MQS.keybinds.unbind(name)");
                }
                RunningScript owner = getCurrentScript();
                keybindManager.unregister(owner, args[0].asString());
                return null;
            };
            case "unbindAll" -> (ProxyExecutable) args -> {
                if (args.length != 0) {
                    throw new IllegalArgumentException("Usage: MQS.keybinds.unbindAll()");
                }
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

    private Value buildOptions(RunningScript owner, Value optionsArg, int keyCode) {
        Value options;
        if (optionsArg != null && optionsArg.isHostObject() && optionsArg.asHostObject() instanceof KeybindOptionsBuilder builder) {
            options = builder.buildObject();
        } else if (optionsArg != null && optionsArg.hasMembers()) {
            options = optionsArg;
        } else {
            options = owner.getContext().eval("js", "({})");
        }
        options.putMember(ConfigKeys.KEYBIND_OPT_KEY, keyCode);
        freeze(owner, options);
        return options;
    }

    private void freeze(RunningScript owner, Value value) {
        Value freezeFunction = owner.getContext().eval("js", "Object.freeze");
        freezeFunction.execute(value);
    }

    private Value registerKeybind(RunningScript owner, String name, Value handler, Value options) {
        keybindManager.register(name, handler, owner, options);
        AtomicBoolean disposed = new AtomicBoolean(false);
        ProxyExecutable exec = disposeArgs -> {
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

    private static final class KeybindOptionsBuilder {
        private final RunningScript owner;
        private final Map<String, Object> values = new HashMap<>();

        private KeybindOptionsBuilder(RunningScript owner) {
            this.owner = owner;
        }

        @HostAccess.Export
        public KeybindOptionsBuilder repeatable(boolean repeatable) {
            values.put(ConfigKeys.KEYBIND_OPT_REPEATABLE, repeatable);
            return this;
        }

        @HostAccess.Export
        public KeybindOptionsBuilder debounce(Number millis) {
            if (millis == null) {
                values.remove(ConfigKeys.KEYBIND_OPT_DEBOUNCE);
                return this;
            }
            int coerced = Math.max(0, millis.intValue());
            values.put(ConfigKeys.KEYBIND_OPT_DEBOUNCE, coerced);
            return this;
        }

        private Value buildObject() {
            Value object = owner.getContext().eval("js", "({})");
            if (!values.isEmpty()) {
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    object.putMember(entry.getKey(), entry.getValue());
                }
            }
            return object;
        }
    }
}

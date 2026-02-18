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

import net.me.keybinds.KeybindManager;
import net.me.keybinds.KeybindOptions;
import net.me.keybinds.Keys;
import net.me.scripting.ScriptManager;
import net.me.scripting.api.internal.HandleTracker;
import net.me.scripting.api.internal.ScriptContextHelper;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class KeybindsAPI implements ProxyObject {
    private static final String API_NAME = "Keybinds API";
    private static final String REGISTER = "register";
    private static final String UNREGISTER = "unregister";
    private static final String UNREGISTER_ALL = "unregisterAll";
    private static final String KEYS = "Keys";
    private static final String KEY_KEYS = "keys";
    private static final String KEYBIND_BIND = "bind";
    private static final String KEYBIND_BIND_TOGGLE = "bindToggle";
    private static final String KEYBIND_UNBIND = "unbind";
    private static final String KEYBIND_UNBIND_ALL = "unbindAll";
    private static final String OPTIONS = "options";
    private static final String REGISTER_USAGE = "Usage: MQS.keybinds.register('name', actionFunction, { key, repeatable, debounce })";
    private static final Set<String> MEMBER_KEYS = Set.of(
            KEYBIND_BIND,
            KEYBIND_BIND_TOGGLE,
            KEYBIND_UNBIND,
            KEYBIND_UNBIND_ALL,
            KEY_KEYS,
            OPTIONS,
            REGISTER,
            UNREGISTER,
            UNREGISTER_ALL,
            KEYS
    );

    private final KeybindManager keybindManager;
    private final ScriptContextHelper contextHelper;
    private final HandleTracker<String> keybindTracker;
    private final ProxyObject keysProxy = createKeysProxy();

    public KeybindsAPI(KeybindManager keybindManager, ScriptManager scriptManager) {
        this.keybindManager = keybindManager;
        this.contextHelper = new ScriptContextHelper(scriptManager);
        this.keybindTracker = new HandleTracker<>();
    }

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case KEY_KEYS, KEYS -> keysProxy;
            case OPTIONS ->
                    (ProxyExecutable) ignored -> contextHelper.require(API_NAME).getContext().asValue(KeybindOptions.builder());
            case KEYBIND_BIND -> (ProxyExecutable) args -> {
                ApiArgumentChecks.requireArgCountAtLeast(args, 3, "Usage: MQS.keybinds.bind(name, key, handler, options?)");
                RunningScript owner = contextHelper.require(API_NAME);
                String name = ApiArgumentChecks.requireString(args, 0, "Keybind name must be a string.");
                int keyCode = extractKeyCode(args[1]);
                Value handler = ApiArgumentChecks.requireExecutable(args, 2, "Handler must be executable.");

                KeybindOptions options = resolveOptions(args.length > 3 ? args[3] : null, keyCode);

                return registerKeybind(owner, name, handler, options);
            };
            case KEYBIND_BIND_TOGGLE -> (ProxyExecutable) args -> {
                ApiArgumentChecks.requireArgCountAtLeast(args, 3, "Usage: MQS.keybinds.bindToggle(name, key, handler, options?)");
                RunningScript owner = contextHelper.require(API_NAME);
                String name = ApiArgumentChecks.requireString(args, 0, "Keybind name must be a string.");
                int keyCode = extractKeyCode(args[1]);
                Value handler = ApiArgumentChecks.requireExecutable(args, 2, "Handler must be executable.");

                AtomicBoolean state = new AtomicBoolean(false);
                ProxyExecutable toggleExecutable = ignored -> {
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
                RunningScript owner = contextHelper.require(API_NAME);
                keybindManager.unregister(owner, name);
                keybindTracker.remove(owner, name);
                return null;
            };
            case KEYBIND_UNBIND_ALL -> (ProxyExecutable) args -> {
                ApiArgumentChecks.requireArgCount(args, 0, "Usage: MQS.keybinds.unbindAll()");
                RunningScript owner = contextHelper.require(API_NAME);
                keybindManager.unregister(owner);
                keybindTracker.disposeAll(owner, ignored -> {
                });
                return null;
            };
            case REGISTER -> (ProxyExecutable) args -> {
                ApiArgumentChecks.requireArgCountAtLeast(args, 2, REGISTER_USAGE);
                RunningScript owner = contextHelper.require(API_NAME);
                String name = ApiArgumentChecks.requireString(args, 0, REGISTER_USAGE);
                Value action = ApiArgumentChecks.requireExecutable(args, 1, REGISTER_USAGE);
                Value optionsArg = args.length > 2 ? args[2] : null;
                KeybindOptions options = KeybindOptions.fromScript(optionsArg, Keys.UNBOUND.getCode());
                return registerKeybind(owner, name, action, options);
            };
            case UNREGISTER -> (ProxyExecutable) args -> {
                ApiArgumentChecks.requireArgCount(args, 1, "Usage: MQS.keybinds.unregister('name')");
                String name = ApiArgumentChecks.requireString(args, 0, "Usage: MQS.keybinds.unregister('name')");
                RunningScript owner = contextHelper.require(API_NAME);
                keybindManager.unregister(owner, name);
                keybindTracker.remove(owner, name);
                return null;
            };
            case UNREGISTER_ALL -> (ProxyExecutable) args -> {
                ApiArgumentChecks.requireArgCount(args, 0, "Usage: MQS.keybinds.unregisterAll()");
                RunningScript owner = contextHelper.require(API_NAME);
                keybindManager.unregister(owner);
                keybindTracker.disposeAll(owner, ignored -> {
                });
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
        keybindTracker.track(owner, name);
        return contextHelper.createIdempotentDisposer(owner, () -> {
            keybindManager.unregister(owner, name);
            keybindTracker.remove(owner, name);
        });
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

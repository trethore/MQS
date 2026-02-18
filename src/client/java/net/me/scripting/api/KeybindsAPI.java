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
import net.me.keybinds.KeyBinding;
import net.me.keybinds.KeybindOptions;
import net.me.keybinds.Keys;
import net.me.scripting.ScriptManager;
import net.me.scripting.api.internal.HandleTracker;
import net.me.scripting.api.internal.ScriptContextHelper;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class KeybindsAPI implements ProxyObject {
    private static final String API_NAME = "Keybinds API";
    private static final String KEYS = "Keys";
    private static final String KEY_KEYS = "keys";
    private static final String KEYBIND_BIND = "bind";
    private static final String KEYBIND_BIND_TOGGLE = "bindToggle";
    private static final String KEYBIND_UNBIND = "unbind";
    private static final String KEYBIND_UNBIND_ALL = "unbindAll";
    private static final String OPTIONS = "options";

    private static final String BIND_USAGE = "Usage: MQS.keybinds.bind(name, key, handler, options?)";
    private static final String BIND_TOGGLE_USAGE = "Usage: MQS.keybinds.bindToggle(name, key, handler, options?)";
    private static final String UNBIND_USAGE = "Usage: MQS.keybinds.unbind(name)";
    private static final String UNBIND_ALL_USAGE = "Usage: MQS.keybinds.unbindAll()";

    private static final Set<String> MEMBER_KEYS = Set.of(
            KEYBIND_BIND,
            KEYBIND_BIND_TOGGLE,
            KEYBIND_UNBIND,
            KEYBIND_UNBIND_ALL,
            KEY_KEYS,
            OPTIONS,
            KEYS
    );
    private static final Map<String, Integer> KEY_CODES_BY_NAME = createKeyCodeLookup();
    private static final String[] KEY_NAMES = createKeyNameArray();

    private final KeybindManager keybindManager;
    private final ScriptContextHelper contextHelper;
    private final HandleTracker<KeyBinding> keybindTracker;
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
            case OPTIONS -> createOptionsExecutable();
            case KEYBIND_BIND -> createBindExecutable();
            case KEYBIND_BIND_TOGGLE -> createBindToggleExecutable();
            case KEYBIND_UNBIND -> createUnbindExecutable();
            case KEYBIND_UNBIND_ALL -> createUnbindAllExecutable();
            default -> null;
        };
    }

    @Override
    public Object getMemberKeys() {
        return MEMBER_KEYS.toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        return key != null && MEMBER_KEYS.contains(key);
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

        if (keyValue.isString()) {
            return resolveKeyCodeFromName(keyValue.asString());
        }

        if (keyValue.isHostObject()) {
            Object hostObject = keyValue.asHostObject();
            if (hostObject instanceof Number number) {
                return number.intValue();
            }
            if (hostObject instanceof Keys keyEnum) {
                return keyEnum.getCode();
            }
            if (hostObject instanceof String text) {
                return resolveKeyCodeFromName(text);
            }
        }

        throw new IllegalArgumentException("Key must be a numeric code, a key name string, or retrieved from MQS.keybinds.keys.");
    }

    private ProxyExecutable createOptionsExecutable() {
        return args -> {
            ApiArgumentChecks.requireArgCount(args, 0, "Usage: MQS.keybinds.options()");
            return contextHelper.require(API_NAME).getContext().asValue(KeybindOptions.builder());
        };
    }

    private ProxyExecutable createBindExecutable() {
        return args -> {
            ApiArgumentChecks.requireArgCountRange(args, 3, 4, BIND_USAGE);
            RunningScript owner = contextHelper.require(API_NAME);
            String name = ApiArgumentChecks.requireString(args, 0, "Keybind name must be a string.");
            int keyCode = extractKeyCode(args[1]);
            Value handler = ApiArgumentChecks.requireExecutable(args, 2, "Handler must be executable.");
            KeybindOptions options = resolveOptions(args.length > 3 ? args[3] : null);
            return registerKeybind(owner, name, keyCode, handler, options);
        };
    }

    private ProxyExecutable createBindToggleExecutable() {
        return args -> {
            ApiArgumentChecks.requireArgCountRange(args, 3, 4, BIND_TOGGLE_USAGE);
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
            KeybindOptions options = resolveOptions(args.length > 3 ? args[3] : null);
            return registerKeybind(owner, name, keyCode, toggleHandler, options);
        };
    }

    private ProxyExecutable createUnbindExecutable() {
        return args -> {
            ApiArgumentChecks.requireArgCount(args, 1, UNBIND_USAGE);
            String name = ApiArgumentChecks.requireString(args, 0, UNBIND_USAGE);
            RunningScript owner = contextHelper.require(API_NAME);
            boolean removed = keybindManager.unregister(owner, name);
            keybindTracker.dispose(owner, binding -> binding.getName().equals(name), ignored -> {
            });
            return removed;
        };
    }

    private ProxyExecutable createUnbindAllExecutable() {
        return args -> {
            ApiArgumentChecks.requireArgCount(args, 0, UNBIND_ALL_USAGE);
            RunningScript owner = contextHelper.require(API_NAME);
            keybindManager.unregister(owner);
            keybindTracker.disposeAll(owner, ignored -> {
            });
            return null;
        };
    }

    private KeybindOptions resolveOptions(Value optionsArg) {
        if (optionsArg == null) {
            return KeybindOptions.builder().build();
        }
        if (optionsArg.isHostObject()) {
            Object host = optionsArg.asHostObject();
            if (host instanceof KeybindOptions options) {
                return options;
            }
            if (host instanceof KeybindOptions.Builder builder) {
                return builder.build();
            }
        }
        return KeybindOptions.fromScript(optionsArg);
    }

    private Value registerKeybind(RunningScript owner, String name, int keyCode, Value handler, KeybindOptions options) {
        KeyBinding binding = keybindManager.register(name, handler, owner, keyCode, options);
        keybindTracker.dispose(owner, tracked -> tracked.getName().equals(name), ignored -> {
        });
        keybindTracker.track(owner, binding);
        return contextHelper.createIdempotentDisposer(owner, () -> {
            keybindManager.unregister(owner, name, binding);
            keybindTracker.remove(owner, binding);
        });
    }

    private int resolveKeyCodeFromName(String keyName) {
        if (keyName == null || keyName.isBlank()) {
            throw new IllegalArgumentException("Key name cannot be empty.");
        }

        Integer keyCode = resolveKeyCodeValue(keyName);
        if (keyCode != null) {
            return keyCode;
        }

        throw new IllegalArgumentException("Unknown key name '" + keyName + "'. Use MQS.keybinds.keys.* for supported names.");
    }

    private Integer resolveKeyCodeValue(String keyName) {
        if (keyName == null) {
            return null;
        }

        String normalizedLower = keyName.trim().toLowerCase(Locale.ROOT);
        Integer directMatch = KEY_CODES_BY_NAME.get(normalizedLower);
        if (directMatch != null) {
            return directMatch;
        }

        String normalizedVariant = normalizeKeyName(keyName);
        return KEY_CODES_BY_NAME.get(normalizedVariant);
    }

    private static String normalizeKeyName(String keyName) {
        return keyName.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private static Map<String, Integer> createKeyCodeLookup() {
        Map<String, Integer> keyCodesByName = new HashMap<>();
        for (Keys key : Keys.values()) {
            int keyCode = key.getCode();
            String enumName = key.name().toLowerCase(Locale.ROOT);
            String friendlyName = key.toString().toLowerCase(Locale.ROOT);
            keyCodesByName.put(enumName, keyCode);
            keyCodesByName.put(normalizeKeyName(enumName), keyCode);
            keyCodesByName.put(friendlyName, keyCode);
            keyCodesByName.put(normalizeKeyName(friendlyName), keyCode);
        }
        return Map.copyOf(keyCodesByName);
    }

    private static String[] createKeyNameArray() {
        Keys[] values = Keys.values();
        String[] names = new String[values.length];
        for (int index = 0; index < values.length; index++) {
            names[index] = values[index].name();
        }
        return names;
    }

    private ProxyObject createKeysProxy() {
        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                return resolveKeyCodeValue(key);
            }

            @Override
            public Object getMemberKeys() {
                return KEY_NAMES.clone();
            }

            @Override
            public boolean hasMember(String key) {
                return resolveKeyCodeValue(key) != null;
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("Cannot modify MQS.keybinds.keys.");
            }
        };
    }
}

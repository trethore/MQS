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

package net.me.keybinds;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.me.Main;
import net.me.config.ConfigKeys;
import net.me.scripting.ConfigManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class KeybindManager {
    private static final String HOST_ID = Main.MOD_ID + "::host";

    private final Map<String, KeyBinding> keybindsByName = new ConcurrentHashMap<>();
    private final Map<String, HostKeyBinding> hostKeybindsByName = new ConcurrentHashMap<>();
    private final Map<Integer, List<KeybindEntry>> keybindsByKeycode = new ConcurrentHashMap<>();
    private final Set<Integer> heldKeys = ConcurrentHashMap.newKeySet();
    private final ScriptManager scriptManager;
    private final ConfigManager configManager;

    public KeybindManager(ScriptManager scriptManager, ConfigManager configManager) {
        this.scriptManager = scriptManager;
        this.configManager = configManager;
        ClientTickEvents.END_CLIENT_TICK.register(client -> onTick());
    }

    private void onTick() {
        if (heldKeys.isEmpty()) {
            return;
        }

        for (Integer button : heldKeys) {
            processInput(button, GLFW.GLFW_REPEAT);
        }
    }

    private void processInput(int keyCode, int action) {
        List<KeybindEntry> bindings = keybindsByKeycode.get(keyCode);
        if (bindings != null) {
            for (KeybindEntry keyBinding : bindings) {
                keyBinding.execute(action);
            }
        }
    }

    private void detachBinding(KeybindEntry binding) {
        if (binding == null) return;
        heldKeys.remove(binding.getKey());
        List<KeybindEntry> bindings = keybindsByKeycode.get(binding.getKey());
        if (bindings != null) {
            bindings.remove(binding);
            if (bindings.isEmpty()) {
                keybindsByKeycode.remove(binding.getKey());
            }
        }
    }

    public void onKey(int key, int action) {
        if (action != GLFW.GLFW_REPEAT) {
            processInput(key, action);
        }

        if (action == GLFW.GLFW_PRESS) {
            heldKeys.add(key);
        } else if (action == GLFW.GLFW_RELEASE) {
            heldKeys.remove(key);
        }
    }

    public void onMouseClick(int button, int action) {
        if (action == GLFW.GLFW_PRESS) {
            heldKeys.add(button);
        } else if (action == GLFW.GLFW_RELEASE) {
            heldKeys.remove(button);
        }
        processInput(button, action);
    }

    public void register(String name, Value action, RunningScript owner, Value options) {
        int defaultKey = Keys.UNBOUND.getCode();
        boolean repeatable = false;
        int debounceTime = 100;

        if (options != null && options.hasMembers()) {
            if (options.hasMember(ConfigKeys.KEYBIND_OPT_KEY) && options.getMember(ConfigKeys.KEYBIND_OPT_KEY).isNumber()) {
                defaultKey = options.getMember(ConfigKeys.KEYBIND_OPT_KEY).asInt();
            }
            if (options.hasMember(ConfigKeys.KEYBIND_OPT_REPEATABLE) && options.getMember(ConfigKeys.KEYBIND_OPT_REPEATABLE).isBoolean()) {
                repeatable = options.getMember(ConfigKeys.KEYBIND_OPT_REPEATABLE).asBoolean();
            }
            if (options.hasMember(ConfigKeys.KEYBIND_OPT_DEBOUNCE) && options.getMember(ConfigKeys.KEYBIND_OPT_DEBOUNCE).isNumber()) {
                debounceTime = options.getMember(ConfigKeys.KEYBIND_OPT_DEBOUNCE).asInt();
            }
        }

        String uniqueName = owner.getId() + "::" + name;
        if (keybindsByName.containsKey(uniqueName)) {
            Main.LOGGER.warn("Keybind '{}' is already registered for script '{}'. It will be replaced.", name, owner.getName());
            this.unregister(owner, name);
        }

        int finalKey = configManager.getKeybind(owner.getId(), name).orElse(defaultKey);

        KeyBinding keyBinding = new KeyBinding(name, finalKey, repeatable, owner, action, debounceTime, scriptManager);

        keybindsByName.put(uniqueName, keyBinding);
        if (finalKey >= 0) {
            keybindsByKeycode.computeIfAbsent(finalKey, k -> new CopyOnWriteArrayList<>()).add(keyBinding);
        }
    }

    public HostKeyBinding registerHost(String name, Runnable action, int defaultKey, boolean repeatable, int debounceTime) {
        String uniqueName = HOST_ID + "::" + name;
        HostKeyBinding existing = hostKeybindsByName.remove(uniqueName);
        if (existing != null) {
            detachBinding(existing);
        }

        int finalKey = configManager.getKeybind(HOST_ID, name).orElse(defaultKey);
        HostKeyBinding binding = new HostKeyBinding(name, finalKey, repeatable, action, debounceTime);

        hostKeybindsByName.put(uniqueName, binding);
        if (finalKey >= 0) {
            keybindsByKeycode.computeIfAbsent(finalKey, k -> new CopyOnWriteArrayList<>()).add(binding);
        }

        return binding;
    }

    public void unregister(RunningScript owner, String name) {
        String uniqueName = owner.getId() + "::" + name;
        KeyBinding keyBinding = keybindsByName.remove(uniqueName);
        if (keyBinding == null) {
            Main.LOGGER.warn("Script '{}' attempted to unregister keybind '{}', which was not found.", owner.getName(), name);
            return;
        }
        detachBinding(keyBinding);
    }

    public void unregister(RunningScript owner) {
        keybindsByName.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(owner.getId() + "::")) {
                KeyBinding kb = entry.getValue();
                detachBinding(kb);
                return true;
            }
            return false;
        });
    }

    public void rebindKey(KeyBinding binding, int newKeyCode) {
        if (binding == null) return;

        List<KeybindEntry> oldBindings = keybindsByKeycode.get(binding.getKey());
        if (oldBindings != null) {
            oldBindings.remove(binding);
            if (oldBindings.isEmpty()) {
                keybindsByKeycode.remove(binding.getKey());
            }
        }

        binding.setKey(newKeyCode);

        if (newKeyCode >= 0) {
            keybindsByKeycode.computeIfAbsent(newKeyCode, k -> new CopyOnWriteArrayList<>()).add(binding);
        }

        configManager.setKeybind(binding.getOwner().getId(), binding.getName(), newKeyCode);
    }

    public Map<RunningScript, List<KeyBinding>> getGroupedKeybinds() {
        Map<RunningScript, List<KeyBinding>> grouped = new ConcurrentHashMap<>();
        for (KeyBinding binding : keybindsByName.values()) {
            grouped.computeIfAbsent(binding.getOwner(), k -> new CopyOnWriteArrayList<>()).add(binding);
        }
        return grouped;
    }
}

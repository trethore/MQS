package net.me.keybinds;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.me.Main;
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

    private final Map<String, KeyBinding> keybindsByName = new ConcurrentHashMap<>();
    private final Map<Integer, List<KeyBinding>> keybindsByKeycode = new ConcurrentHashMap<>();
    private final Set<Integer> heldKeys = ConcurrentHashMap.newKeySet();
    private ScriptManager scriptManager;

    public void init(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
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
        List<KeyBinding> bindings = keybindsByKeycode.get(keyCode);
        if (bindings != null) {
            for (KeyBinding keyBinding : bindings) {
                keyBinding.execute(action, scriptManager);
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

    public void register(String name, int defaultKey, boolean repeatable, RunningScript owner, Value action) {
        String uniqueName = owner.getId() + "::" + name;
        if (keybindsByName.containsKey(uniqueName)) {
            Main.LOGGER.warn("Keybind '{}' is already registered for script '{}'. It will be replaced.", name, owner.getName());
            this.unregister(owner, name);
        }

        KeyBinding keyBinding = new KeyBinding(name, defaultKey, repeatable, owner, action);
        keybindsByName.put(uniqueName, keyBinding);
        keybindsByKeycode.computeIfAbsent(defaultKey, k -> new CopyOnWriteArrayList<>()).add(keyBinding);
    }

    public void unregister(RunningScript owner, String name) {
        String uniqueName = owner.getId() + "::" + name;
        KeyBinding keyBinding = keybindsByName.remove(uniqueName);
        if (keyBinding == null) {
            Main.LOGGER.warn("Script '{}' attempted to unregister keybind '{}', which was not found.", owner.getName(), name);
            return;
        }
        heldKeys.remove(keyBinding.getKey());
        List<KeyBinding> bindings = keybindsByKeycode.get(keyBinding.getKey());
        if (bindings != null) {
            bindings.remove(keyBinding);
            if (bindings.isEmpty()) {
                keybindsByKeycode.remove(keyBinding.getKey());
            }
        }
    }

    public void unregister(RunningScript owner) {
        keybindsByName.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(owner.getId() + "::")) {
                KeyBinding kb = entry.getValue();
                heldKeys.remove(kb.getKey());
                List<KeyBinding> bindings = keybindsByKeycode.get(kb.getKey());
                if (bindings != null) {
                    bindings.remove(kb);
                    if (bindings.isEmpty()) {
                        keybindsByKeycode.remove(kb.getKey());
                    }
                }
                return true;
            }
            return false;
        });
    }
}
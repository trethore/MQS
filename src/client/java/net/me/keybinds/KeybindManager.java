package net.me.keybinds;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.me.Main;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import net.minecraft.client.MinecraftClient;
import org.graalvm.polyglot.Value;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KeybindManager {

    private final Map<String, KeyBinding> registeredKeybinds = new ConcurrentHashMap<>();
    private ScriptManager scriptManager;

    public void init(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
        ClientTickEvents.END_CLIENT_TICK.register(client -> this.onTick());
    }

    private void onTick() {
        if (MinecraftClient.getInstance().currentScreen != null) return;
        for (KeyBinding keyBinding : registeredKeybinds.values()) {
            keyBinding.execute(scriptManager);
        }
    }

    public void register(String name, int defaultKey, boolean repeatable, RunningScript owner, Value action) {
        String uniqueName = owner.getId() + "::" + name;
        if (registeredKeybinds.containsKey(uniqueName)) {
            Main.LOGGER.warn("Keybind '{}' is already registered for script '{}'. Overwriting.", name, owner.getName());
        }

        KeyBinding keyBinding = new KeyBinding(name, defaultKey, repeatable, owner, action, scriptManager);
        registeredKeybinds.put(uniqueName, keyBinding);
    }

    public void unregister(RunningScript owner) {
        registeredKeybinds.keySet().removeIf(key -> key.startsWith(owner.getId() + "::"));
    }
}
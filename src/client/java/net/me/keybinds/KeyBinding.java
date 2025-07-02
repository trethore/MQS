package net.me.keybinds;

import net.me.Main;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.graalvm.polyglot.Value;
import org.lwjgl.glfw.GLFW;

public class KeyBinding {
    private final String name;
    private final RunningScript owner;
    private final Value action;
    private final boolean repeatable;

    private int key;

    private boolean isHeld = false;
    private long lastPressTime = 0;

    private static final int COOLDOWN_MS = 200;

    public KeyBinding(String name, int key, boolean repeatable, RunningScript owner, Value action) {
        this.name = name;
        this.key = key;
        this.repeatable = repeatable;
        this.owner = owner;
        this.action = action;
    }

    public void execute(ScriptManager scriptManager) {
        if (key < 0 || action == null || !action.canExecute()) return;
        if (isPressed(key)) {
            if (!isHeld) {
                isHeld = true;
                fireAction(scriptManager);
            } else if (repeatable) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastPressTime > COOLDOWN_MS) {
                    fireAction(scriptManager);
                }
            }
        } else {
            if (isHeld) {
                isHeld = false;
            }
        }
    }
    private boolean isPressed(int key) {
        long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();
        return (key < 8)
                ? GLFW.glfwGetMouseButton(windowHandle, key) == GLFW.GLFW_PRESS
                : InputUtil.isKeyPressed(windowHandle, key);
    }

    private void fireAction(ScriptManager scriptManager) {
        this.lastPressTime = System.currentTimeMillis();
        scriptManager.setCurrentScript(owner);
        try {
            action.execute();
        } catch (Exception e) {
            Main.LOGGER.error("Error executing keybind '{}' for script '{}'", name, owner.getName(), e);
        } finally {
            scriptManager.clearCurrentScript();
        }
    }

    public String getName() {
        return name;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public RunningScript getOwner() {
        return owner;
    }
}
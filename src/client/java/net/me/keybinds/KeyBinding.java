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

    private long lastPress = 0;
    private boolean hasBeenSet = false;

    public KeyBinding(String name, int key, boolean repeatable, RunningScript owner, Value action, ScriptManager scriptManager) {
        this.name = name;
        this.key = key;
        this.repeatable = repeatable;
        this.owner = owner;
        this.action = action;
    }

    public void execute(ScriptManager scriptManager) {
        if (key < 0 || action == null || !action.canExecute()) return;

        long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();
        boolean isPressed = (key < 8)
                ? GLFW.glfwGetMouseButton(windowHandle, key) == GLFW.GLFW_PRESS
                : InputUtil.isKeyPressed(windowHandle, key);

        if (isPressed) {
            long currentTime = System.currentTimeMillis();
            int cooldown = 200;
            if (currentTime - lastPress > cooldown || (repeatable && !hasBeenSet)) {
                lastPress = currentTime;
                hasBeenSet = false;

                scriptManager.setCurrentScript(owner);
                try {
                    action.execute();
                } catch (Exception e) {
                    Main.LOGGER.error("Error executing keybind '{}' for script '{}'", name, owner.getName(), e);
                } finally {
                    scriptManager.clearCurrentScript();
                }
            }
        }
    }

    public String getName() { return name; }
    public int getKey() { return key; }
    public void setKey(int key) {
        this.key = key;
    }

    public RunningScript getOwner() { return owner; }
}
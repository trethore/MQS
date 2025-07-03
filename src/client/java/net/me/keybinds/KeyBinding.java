package net.me.keybinds;

import net.me.Main;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.lwjgl.glfw.GLFW;

public class KeyBinding {
    private final String name;
    private final RunningScript owner;
    private final Value action;
    private final boolean repeatable;
    private final int debounceTime;
    private int key;
    private long lastReleaseTime = 0;
    private boolean hasBeenPressed = false;

    public KeyBinding(String name, int key, boolean repeatable, RunningScript owner, Value action, int debounceTime) {
        this.name = name;
        this.key = key;
        this.repeatable = repeatable;
        this.owner = owner;
        this.action = action;
        this.debounceTime = debounceTime;
    }

    public void execute(int glfwAction, ScriptManager scriptManager) {
        if (key < 0 || action == null || !action.canExecute()) return;
        long currentTime = System.currentTimeMillis();
        if (glfwAction == GLFW.GLFW_PRESS || (glfwAction == GLFW.GLFW_REPEAT && repeatable)) {
            if (currentTime - lastReleaseTime < debounceTime) {
                return;
            }
            fireAction(scriptManager);
            hasBeenPressed = true;
        } else if (glfwAction == GLFW.GLFW_RELEASE && !(currentTime - lastReleaseTime < debounceTime) && hasBeenPressed) {
            lastReleaseTime = System.currentTimeMillis();
            hasBeenPressed = false;
        }
    }

    private void fireAction(ScriptManager scriptManager) {
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
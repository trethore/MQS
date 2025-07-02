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

    private int key;

    public KeyBinding(String name, int key, boolean repeatable, RunningScript owner, Value action) {
        this.name = name;
        this.key = key;
        this.repeatable = repeatable;
        this.owner = owner;
        this.action = action;
    }

    public void execute(int glfwAction, ScriptManager scriptManager) {
        if (key < 0 || action == null || !action.canExecute()) return;

        if (glfwAction == GLFW.GLFW_PRESS) {
            fireAction(scriptManager);
        } else if (glfwAction == GLFW.GLFW_REPEAT && repeatable) {
            fireAction(scriptManager);
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
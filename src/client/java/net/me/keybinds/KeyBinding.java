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
    private final ScriptManager scriptManager;
    private int key;
    private long lastReleaseTime = 0;
    private boolean hasBeenPressed = false;

    public KeyBinding(String name, int key, boolean repeatable, RunningScript owner, Value action, int debounceTime, ScriptManager scriptManager) {
        this.name = name;
        this.key = key;
        this.repeatable = repeatable;
        this.owner = owner;
        this.action = action;
        this.debounceTime = debounceTime;
        this.scriptManager = scriptManager;
    }

    public void execute(int glfwAction) {
        if (key < 0 || action == null || !action.canExecute() || scriptManager == null) return;
        long currentTime = System.currentTimeMillis();
        if (glfwAction == GLFW.GLFW_PRESS || (glfwAction == GLFW.GLFW_REPEAT && repeatable)) {
            if (currentTime - lastReleaseTime < debounceTime) {
                return;
            }
            fireAction();
            hasBeenPressed = true;
        } else if (glfwAction == GLFW.GLFW_RELEASE && !(currentTime - lastReleaseTime < debounceTime) && hasBeenPressed) {
            lastReleaseTime = System.currentTimeMillis();
            hasBeenPressed = false;
        }
    }

    private void fireAction() {
        RunningScript previousScript = scriptManager.getCurrentScript();
        scriptManager.setCurrentScript(owner);
        try {
            action.execute();
        } catch (Exception e) {
            Main.LOGGER.error("Error executing keybind '{}' for script '{}'", name, owner.getName(), e);
        } finally {
            scriptManager.setCurrentScript(previousScript);
        }
    }

    public String getKeyName() {
        if (key < 0) {
            return "UNKNOWN";
        } else if (key < 7) {
            return "BUTTON_" + key;
        }
        return Keys.fromCode(this.key).map(Keys::toString).orElse("Unknown");
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
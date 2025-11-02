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

import lombok.Getter;
import org.lwjgl.glfw.GLFW;

public class HostKeyBinding implements KeybindEntry {
    @Getter
    private final String name;
    private final Runnable action;
    private final boolean repeatable;
    private final int debounceTime;
    private int key;
    private long lastReleaseTime = 0;
    private boolean hasBeenPressed = false;

    HostKeyBinding(String name, int key, boolean repeatable, Runnable action, int debounceTime) {
        this.name = name;
        this.key = key;
        this.repeatable = repeatable;
        this.action = action;
        this.debounceTime = debounceTime;
    }

    @Override
    public int getKey() {
        return key;
    }

    @Override
    public void setKey(int key) {
        this.key = key;
    }

    @Override
    public void execute(int glfwAction) {
        if (key < 0 || action == null) return;
        long currentTime = System.currentTimeMillis();
        if (glfwAction == GLFW.GLFW_PRESS || (glfwAction == GLFW.GLFW_REPEAT && repeatable)) {
            if (currentTime - lastReleaseTime < debounceTime) {
                return;
            }
            action.run();
            hasBeenPressed = true;
        } else if (glfwAction == GLFW.GLFW_RELEASE && !(currentTime - lastReleaseTime < debounceTime) && hasBeenPressed) {
            lastReleaseTime = System.currentTimeMillis();
            hasBeenPressed = false;
        }
    }
}

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

package net.me.ui.widgets;

import net.me.scripting.ScriptingService;
import net.me.scripting.module.ScriptDescriptor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;

import java.util.Collection;
import org.jetbrains.annotations.Nullable;
public class ScriptListWidget extends ElementListWidget<ScriptListEntry> {

    private final ScriptingService scriptingService;
    private final Runnable onToggle;
    private String selectedId;

    public ScriptListWidget(MinecraftClient client, int width, int height, int y, int itemHeight, ScriptingService scriptingService, Runnable onToggle) {
        super(client, width, height, y, itemHeight);
        this.scriptingService = scriptingService;
        this.onToggle = onToggle;
    }

    public void setScripts(Collection<ScriptDescriptor> descriptors) {
        this.clearEntries();
        for (ScriptDescriptor descriptor : descriptors) {
            boolean running = scriptingService.isRunning(descriptor.getId());
            ScriptListEntry entry = new ScriptListEntry(descriptor, running, scriptingService, onToggle);
            entry.setParentList(this);
            this.addEntry(entry);
        }

        if (selectedId != null) {
            for (ScriptListEntry entry : this.children()) {
                if (selectedId.equals(entry.descriptor().getId())) {
                    this.setSelected(entry);
                    this.ensureVisible(entry);
                    break;
                }
            }
        }
    }

    public void selectById(@Nullable String id) {
        this.selectedId = id;
        if (id == null) {
            this.setSelected(null);
            return;
        }
        for (ScriptListEntry entry : this.children()) {
            if (!id.equals(entry.descriptor().getId())) {
                continue;
            }
            this.setSelected(entry);
            this.ensureVisible(entry);
            break;
        }
    }

    @Nullable
    public String getSelectedId() {
        return this.selectedId;
    }

    @Override
    public void setSelected(@org.jetbrains.annotations.Nullable ScriptListEntry entry) {
        super.setSelected(entry);
        if (entry == null) {
            this.selectedId = null;
            return;
        }
        this.selectedId = entry.descriptor().getId();
        this.setFocused(entry);
        entry.focusAnchor();
    }

    @Override
    public int getRowWidth() {
        return Math.max(260, this.getWidth() - 16);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UP) {
            int count = this.getEntryCount();
            if (count == 0) {
                return false;
            }
            ScriptListEntry current = this.getSelectedOrNull();
            int idx = current == null ? -1 : this.children().indexOf(current);
            int next = keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN ? idx + 1 : idx - 1;
            next = Math.max(0, Math.min(count - 1, next));
            if (next != idx) {
                ScriptListEntry target = this.getEntry(next);
                this.setSelected(target);
                this.ensureVisible(target);
            }
            return true;
        } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            ScriptListEntry current = this.getSelectedOrNull();
            if (current != null) {
                current.toggle();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public ScriptListEntry getSelectedEntry() {
        return this.getSelectedOrNull();
    }

    @Override
    protected int getScrollbarX() {
        return this.getRowLeft() + getRowWidth() + 4;
    }

    @Override
    protected void renderDecorations(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY) {
        // Don't render background
    }
}

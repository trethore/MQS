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

package net.me.screen.component.components;

import net.me.config.GlobalConfigManager;
import net.me.screen.component.AbstractToggleEntryWidget;
import net.me.screen.screens.AllScriptsScreen;
import net.me.screen.screens.AssignCategoryScreen;
import net.me.scripting.ScriptingService;
import net.me.scripting.module.ScriptDescriptor;
import net.me.utils.McUtils;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ScriptDescriptorToggleWidget extends AbstractToggleEntryWidget {
    private final ScriptingService scriptingService;
    private final GlobalConfigManager globalConfigManager;
    private ScriptDescriptor descriptor;

    protected ScriptDescriptorToggleWidget(int x, int y, int width, int height, @Nullable ScriptDescriptor descriptor, ScriptingService scriptingService, GlobalConfigManager globalConfigManager) {
        super(x, y, width, height);
        this.scriptingService = scriptingService;
        this.globalConfigManager = globalConfigManager;
        this.update(descriptor);
    }

    public void update(@Nullable ScriptDescriptor descriptor) {
        this.descriptor = descriptor;
        this.visible = (descriptor != null);
        this.active = (descriptor != null);
    }

    @Override
    protected boolean isToggled() {
        if (descriptor == null) return false;
        return scriptingService.isRunning(descriptor.getId());
    }

    @Override
    protected String getPrimaryText() {
        if (descriptor == null) return "";
        return this.descriptor.moduleName() + " v" + this.descriptor.version();
    }

    @Override
    protected String getSecondaryText() {
        if (descriptor == null) return "";
        return this.descriptor.path().getFileName().toString();
    }

    @Override
    public void onPress() {
        if (descriptor == null) return;
        boolean isRunning = scriptingService.isRunning(descriptor.getId());
        if (isRunning) {
            scriptingService.disable(descriptor.getId());
        } else {
            scriptingService.enable(descriptor.getId());
        }
        if (this.globalConfigManager.isEnableDisableToastEnabled()) {
            String state = (isRunning ? Formatting.RED + "disabled" : Formatting.GREEN + "enabled") + Formatting.RESET;
            MQSToast.show(descriptor.moduleName() + " " + state, "This script has been " + (isRunning ? "disabled" : "enabled"), 1500, MQSToast.Corner.TOP_LEFT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && this.isMouseOver(mouseX, mouseY)) {
            if (button == 1) { // Right-click
                if (this.descriptor != null) {
                    McUtils.getMc().ifPresent(mc -> {
                        if (mc.currentScreen instanceof AllScriptsScreen parent) {
                            new AssignCategoryScreen(parent, this.descriptor).open();
                        }
                    });
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Nullable
    public ScriptDescriptor getDescriptor() {
        return descriptor;
    }

    public static class Builder {
        private final ScriptingService scriptingService;
        private final GlobalConfigManager globalConfigManager;

        private ScriptDescriptor descriptor = null;
        private int x = 0;
        private int y = 0;
        private int width = 200;
        private int height = 30;

        public Builder(ScriptingService scriptingService, GlobalConfigManager globalConfigManager) {
            this.scriptingService = scriptingService;
            this.globalConfigManager = globalConfigManager;
        }

        public Builder descriptor(ScriptDescriptor descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        public Builder dimensions(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public ScriptDescriptorToggleWidget build() {
            return new ScriptDescriptorToggleWidget(this.x, this.y, this.width, this.height, this.descriptor, this.scriptingService, this.globalConfigManager);
        }
    }
}
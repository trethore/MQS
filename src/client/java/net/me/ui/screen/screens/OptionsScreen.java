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

package net.me.ui.screen.screens;

import net.me.Main;
import net.me.config.GlobalConfigManager;
import net.me.ui.screen.MQSScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.Positioner;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Path;

public class OptionsScreen extends MQSScreen {

    private static final int PADDING = 16;
    private static final int BUTTON_WIDTH = 180;
    private static final int SMALL_BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TITLE_HEIGHT = 14;
    private static final int COMPONENT_SPACING = 6;
    private static final int HEIGHT_OFFSET = 25;

    private final GlobalConfigManager globalConfigManager;

    public OptionsScreen(MQSScreen parent, GlobalConfigManager globalConfigManager) {
        super(parent, Text.translatable("screen.mqs.options.title"));
        this.globalConfigManager = globalConfigManager;
    }

    @Override
    protected void init() {
        super.init();
        DirectionalLayoutWidget layout = DirectionalLayoutWidget.vertical().spacing(COMPONENT_SPACING);

        layout.add(new TextWidget(BUTTON_WIDTH, TITLE_HEIGHT, Text.translatable("screen.mqs.options.title"), this.textRenderer)
                .alignCenter(), Positioner::alignHorizontalCenter);
        layout.add(createActionButton(Text.translatable("screen.mqs.options.settings"), this::openSettings), Positioner::alignHorizontalCenter);
        layout.add(createActionButton(Text.translatable("screen.mqs.options.commands"), this::placeholder), Positioner::alignHorizontalCenter);
        layout.add(createActionButton(Text.translatable("screen.mqs.options.keybinds"), this::openKeybindings), Positioner::alignHorizontalCenter);
        layout.add(createActionButton(Text.translatable("screen.mqs.scripts.open_folder"), this::openScriptsFolder), Positioner::alignHorizontalCenter);
        layout.add(createActionButton(Text.translatable("screen.mqs.options.open_ide"), this::openInIde), Positioner::alignHorizontalCenter);
        layout.add(createActionButton(ScreenTexts.DONE, this::closeToParent, SMALL_BUTTON_WIDTH), positioner -> positioner.alignHorizontalCenter().marginTop(PADDING));

        layout.refreshPositions();
        layout.setX(getMiddle().x() - layout.getWidth() / 2);
        layout.setY(getMiddle().y() - layout.getHeight() / 2 - HEIGHT_OFFSET);
        layout.forEachChild(this::addDrawableChild);
    }

    private ButtonWidget createActionButton(Text label, Runnable action) {
        return createActionButton(label, action, BUTTON_WIDTH);
    }

    private ButtonWidget createActionButton(Text label, Runnable action, int width) {
        return ButtonWidget.builder(label, button -> action.run())
                .dimensions(0, 0, width, BUTTON_HEIGHT)
                .build();
    }

    private void placeholder() {
    }

    private void openSettings() {
        new SettingsScreen(this, globalConfigManager).open();
    }

    private void openKeybindings() {
        new KeybindingsScreen(this, Main.getInstance().getKeybindManager()).open();
    }

    private void openScriptsFolder() {
        Path scriptsPath = Main.MOD_DIR.resolve("scripts");
        Util.getOperatingSystem().open(scriptsPath.toUri());
    }

    private void openInIde() {
        Path scriptsPath = Main.MOD_DIR.resolve("scripts");
        String command = globalConfigManager.getDefaultIdeCommand();
        try {
            new ProcessBuilder(command, scriptsPath.toString()).start();
        } catch (IOException e) {
            if (Util.getOperatingSystem() == Util.OperatingSystem.WINDOWS) {
                try {
                    new ProcessBuilder("cmd", "/c", command, scriptsPath.toString()).start();
                    return;
                } catch (IOException ignored) {
                    // fall through to logging below
                }
            }
            Main.LOGGER.error("Failed to open scripts folder in IDE command '{}'", command, e);
        }
    }
}

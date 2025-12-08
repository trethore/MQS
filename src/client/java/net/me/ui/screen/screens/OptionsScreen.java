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
import net.me.ui.UIConstants;
import net.me.ui.UIUtils;
import net.me.ui.screen.MQSScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.Positioner;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class OptionsScreen extends MQSScreen {

    private static final int HEIGHT_OFFSET = 25;

    private final GlobalConfigManager globalConfigManager;

    public OptionsScreen(MQSScreen parent, GlobalConfigManager globalConfigManager) {
        super(parent, Text.translatable("screen.mqs.options.title"));
        this.globalConfigManager = globalConfigManager;
    }

    @Override
    protected void init() {
        super.init();
        DirectionalLayoutWidget layout = DirectionalLayoutWidget.vertical().spacing(UIConstants.COMPONENT_SPACING);

        layout.add(new TextWidget(UIConstants.BUTTON_WIDTH_LARGE, UIConstants.TITLE_HEIGHT, Text.translatable("screen.mqs.options.title"), this.textRenderer)
                .alignCenter(), Positioner::alignHorizontalCenter);
        layout.add(createActionButton(Text.translatable("screen.mqs.options.settings"), this::openSettings), Positioner::alignHorizontalCenter);
        layout.add(createActionButton(Text.translatable("screen.mqs.options.keybinds"), this::openKeybindings), Positioner::alignHorizontalCenter);
        layout.add(createActionButton(Text.translatable("screen.mqs.scripts.open_folder"), UIUtils::openScriptsFolder), Positioner::alignHorizontalCenter);
        layout.add(createActionButton(Text.translatable("screen.mqs.options.open_ide"), this::openInIde), Positioner::alignHorizontalCenter);
        layout.add(createActionButton(ScreenTexts.DONE, this::close, UIConstants.BUTTON_WIDTH_SMALL), positioner -> positioner.alignHorizontalCenter().marginTop(UIConstants.PADDING));

        layout.refreshPositions();
        layout.setX(getMiddle().x() - layout.getWidth() / 2);
        layout.setY(getMiddle().y() - layout.getHeight() / 2 - HEIGHT_OFFSET);
        layout.forEachChild(this::addDrawableChild);
    }

    private ButtonWidget createActionButton(Text label, Runnable action) {
        return createActionButton(label, action, UIConstants.BUTTON_WIDTH_LARGE);
    }

    private ButtonWidget createActionButton(Text label, Runnable action, int width) {
        return ButtonWidget.builder(label, button -> action.run())
                .dimensions(0, 0, width, UIConstants.BUTTON_HEIGHT)
                .build();
    }

    private void openSettings() {
        new SettingsScreen(this, globalConfigManager).open();
    }

    private void openKeybindings() {
        new KeybindingsScreen(this, Main.getInstance().getKeybindManager()).open();
    }

    private void openInIde() {
        UIUtils.openInIde(globalConfigManager.getDefaultIdeCommand());
    }
}

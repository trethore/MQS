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

import net.me.keybinds.KeybindManager;
import net.me.ui.UIConstants;
import net.me.ui.screen.MQSScreen;
import net.me.ui.widgets.KeybindingsListWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class KeybindingsScreen extends MQSScreen {

    private static final int HEADER_HEIGHT = 30;
    private static final int FOOTER_HEIGHT = 40;
    private static final int LIST_ITEM_HEIGHT = 24;

    private final KeybindManager keybindManager;
    private KeybindingsListWidget listWidget;
    private ThreePartsLayoutWidget layout;
    private DirectionalLayoutWidget headerLayout;
    private DirectionalLayoutWidget footerLayout;

    public KeybindingsScreen(MQSScreen parent, KeybindManager keybindManager) {
        super(parent, Text.translatable("screen.mqs.keybinds.title"));
        this.keybindManager = keybindManager;
    }

    @Override
    protected void init() {
        super.init();
        layout = new ThreePartsLayoutWidget(this, HEADER_HEIGHT, FOOTER_HEIGHT);

        headerLayout = layout.addHeader(DirectionalLayoutWidget.vertical().spacing(UIConstants.COMPONENT_SPACING), positioner -> positioner.alignHorizontalCenter().marginTop(UIConstants.TITLE_HEIGHT));
        headerLayout.add(new TextWidget(220, UIConstants.TITLE_HEIGHT, Text.translatable("screen.mqs.keybinds.title"), this.textRenderer).alignCenter());

        int listWidth = Math.min(420, this.width - UIConstants.PADDING * 2);

        listWidget = new KeybindingsListWidget(this.client, listWidth, 0, 0, LIST_ITEM_HEIGHT, keybindManager);
        refreshList();
        this.addDrawableChild(listWidget);

        footerLayout = layout.addFooter(DirectionalLayoutWidget.vertical().spacing(UIConstants.COMPONENT_SPACING), positioner -> positioner.alignHorizontalCenter().marginBottom(UIConstants.PADDING));
        footerLayout.add(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(0, 0, UIConstants.BUTTON_WIDTH_LARGE, UIConstants.BUTTON_HEIGHT)
                .build());

        layout.forEachChild(this::addDrawableChild);
        this.refreshWidgetPositions();
    }

    @Override
    protected void refreshWidgetPositions() {
        if (layout != null) {
            layout.refreshPositions();
        }
        centerAndPositionList();
    }

    private void centerAndPositionList() {
        if (listWidget == null || headerLayout == null || footerLayout == null) {
            return;
        }

        int listWidth = Math.min(420, this.width - UIConstants.PADDING * 2);
        int maxListHeight = 260;
        int verticalBudget = this.height - UIConstants.PADDING * 2 - headerLayout.getHeight() - footerLayout.getHeight() - UIConstants.COMPONENT_SPACING * 2;
        int listHeight = Math.max(LIST_ITEM_HEIGHT * 3, Math.min(maxListHeight, verticalBudget));

        int blockHeight = headerLayout.getHeight() + UIConstants.COMPONENT_SPACING + listHeight + UIConstants.COMPONENT_SPACING + footerLayout.getHeight();
        int top = Math.max(UIConstants.PADDING, Math.min(getMiddle().y() - blockHeight / 2, this.height - UIConstants.PADDING - blockHeight));

        int listY = top + headerLayout.getHeight() + UIConstants.COMPONENT_SPACING;
        int footerY = listY + listHeight + UIConstants.COMPONENT_SPACING;

        headerLayout.setY(top);
        footerLayout.setY(footerY);

        int listX = getMiddle().x() - listWidth / 2;
        listWidget.setDimensionsAndPosition(listWidth, listHeight, listX, listY);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        if (listWidget != null && listWidget.hasListeningBinding()) {
            return false;
        }
        return super.shouldCloseOnEsc();
    }

    private void refreshList() {
        if (listWidget == null) {
            return;
        }
        listWidget.setEntries(
                keybindManager.getGroupedKeybinds(),
                keybindManager.getHostKeybinds()
        );
    }
}
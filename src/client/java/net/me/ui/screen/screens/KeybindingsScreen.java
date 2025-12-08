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
import net.me.ui.screen.MQSScreen;
import net.me.ui.widgets.KeybindingsListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class KeybindingsScreen extends MQSScreen {

    private static final int PADDING = 16;
    private static final int TITLE_HEIGHT = 14;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 140;
    private static final int LIST_ITEM_HEIGHT = 24;
    private static final int MAX_LIST_HEIGHT = 260;

    private final KeybindManager keybindManager;
    private KeybindingsListWidget listWidget;

    public KeybindingsScreen(MQSScreen parent, KeybindManager keybindManager) {
        super(parent, Text.translatable("screen.mqs.keybinds.title"));
        this.keybindManager = keybindManager;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = getMiddle().x();

        TextWidget title = new TextWidget(220, TITLE_HEIGHT, Text.translatable("screen.mqs.keybinds.title"), this.textRenderer).alignCenter();
        int listWidth = Math.min(420, this.width - PADDING * 2);
        int verticalBudget = this.height - PADDING * 2 - TITLE_HEIGHT - BUTTON_HEIGHT - PADDING * 2;
        int listHeight = Math.min(verticalBudget, MAX_LIST_HEIGHT);
        listHeight = Math.max(listHeight, LIST_ITEM_HEIGHT * 3);

        int blockHeight = TITLE_HEIGHT + PADDING + listHeight + PADDING + BUTTON_HEIGHT;

        int titleY = Math.max(PADDING, Math.min(getMiddle().y() - blockHeight / 2, this.height - PADDING - blockHeight));
        int listY = titleY + TITLE_HEIGHT + PADDING;
        int buttonY = listY + listHeight + PADDING;
        int listX = centerX - listWidth / 2;

        title.setX(centerX - title.getWidth() / 2);
        title.setY(titleY);
        this.addDrawableChild(title);

        listWidget = new KeybindingsListWidget(this.client, listWidth, listHeight, listY, LIST_ITEM_HEIGHT, keybindManager);
        listWidget.setDimensionsAndPosition(listWidth, listHeight, listX, listY);
        refreshList();
        this.addDrawableChild(listWidget);

        ButtonWidget done = ButtonWidget.builder(ScreenTexts.DONE, button -> closeToParent())
                .dimensions(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        done.setX(centerX - BUTTON_WIDTH / 2);
        done.setY(buttonY);
        this.addDrawableChild(done);
    }

    @Override
    public void resize(net.minecraft.client.MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        this.init();
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

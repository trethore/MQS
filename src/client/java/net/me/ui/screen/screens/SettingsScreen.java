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

import net.me.config.GlobalConfigManager;
import net.me.ui.screen.MQSScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.Positioner;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class SettingsScreen extends MQSScreen {

    private static final int PADDING = 16;
    private static final int LABEL_WIDTH = 180;
    private static final int CONTROL_WIDTH = 100;
    private static final int FIELD_WIDTH = 100;
    private static final int ROW_SPACING = 14;
    private static final int TITLE_HEIGHT = 16;
    private static final int CONTROL_HEIGHT = 20;
    private static final int GAP_BETWEEN = 12;
    private static final int LAYOUT_Y_OFFSET = 110;

    private final GlobalConfigManager globalConfigManager;

    private ButtonWidget logRedirectButton;
    private ButtonWidget allowClassesButton;

    public SettingsScreen(MQSScreen parent, GlobalConfigManager globalConfigManager) {
        super(parent, Text.translatable("screen.mqs.settings.title"));
        this.globalConfigManager = globalConfigManager;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = getMiddle().x();
        int titleY = getMiddle().y() - LAYOUT_Y_OFFSET;

        TextWidget title = new TextWidget(220, TITLE_HEIGHT, Text.translatable("screen.mqs.settings.title"), this.textRenderer).alignCenter();
        title.setX(centerX - title.getWidth() / 2);
        title.setY(titleY);
        this.addDrawableChild(title);

        GridWidget grid = new GridWidget();
        grid.setColumnSpacing(GAP_BETWEEN);
        grid.setRowSpacing(ROW_SPACING);

        int row = 0;
        logRedirectButton = addToggleRow(grid, row++, Text.translatable("screen.mqs.settings.log_redirect"), this::isLogRedirectEnabled, this::setLogRedirect);
        allowClassesButton = addToggleRow(grid, row++, Text.translatable("screen.mqs.settings.allow_all_classes"), this::areAllClassesAllowed, this::setAllClassesAllowed);
        TextFieldWidget ideCommandField = addTextFieldRow(grid, row, Text.translatable("screen.mqs.settings.default_ide_command"));

        grid.refreshPositions();
        grid.setX(centerX - grid.getWidth() / 2);
        grid.setY(titleY + TITLE_HEIGHT + PADDING);
        grid.refreshPositions();
        grid.forEachElement(widget -> widget.forEachChild(this::addDrawableChild));

        int doneY = grid.getY() + grid.getHeight() + PADDING;
        ButtonWidget done = ButtonWidget.builder(ScreenTexts.DONE, button -> closeToParent())
                .dimensions(0, 0, CONTROL_WIDTH, CONTROL_HEIGHT)
                .build();
        done.setX(centerX - CONTROL_WIDTH / 2);
        done.setY(doneY);
        this.addDrawableChild(done);

        this.setInitialFocus(ideCommandField);
    }

    private ButtonWidget addToggleRow(GridWidget grid, int row, Text label, BooleanSupplier getter, Consumer<Boolean> setter) {
        Positioner leftPos = grid.copyPositioner().alignLeft();
        Positioner rightPos = grid.copyPositioner().alignRight();

        TextWidget labelWidget = new TextWidget(LABEL_WIDTH, CONTROL_HEIGHT, label, this.textRenderer).alignLeft();
        grid.add(labelWidget, row, 0, leftPos);

        ButtonWidget button = ButtonWidget.builder(toggleText(getter.getAsBoolean()), b -> {
            boolean newValue = !getter.getAsBoolean();
            setter.accept(newValue);
            b.setMessage(toggleText(newValue));
        }).dimensions(0, 0, CONTROL_WIDTH, CONTROL_HEIGHT).build();
        grid.add(button, row, 1, rightPos);
        return button;
    }

    private TextFieldWidget addTextFieldRow(GridWidget grid, int row, Text label) {
        Positioner leftPos = grid.copyPositioner().alignLeft();
        Positioner rightPos = grid.copyPositioner().alignRight();

        TextWidget labelWidget = new TextWidget(LABEL_WIDTH, CONTROL_HEIGHT, label, this.textRenderer).alignLeft();
        grid.add(labelWidget, row, 0, leftPos);

        TextFieldWidget field = new TextFieldWidget(this.textRenderer, 0, 0, FIELD_WIDTH, CONTROL_HEIGHT, label);
        field.setText(globalConfigManager.getDefaultIdeCommand());
        field.setChangedListener(globalConfigManager::setDefaultIdeCommand);
        grid.add(field, row, 1, rightPos);
        return field;
    }

    private boolean isLogRedirectEnabled() {
        return globalConfigManager.isLogRedirectEnabled();
    }

    private void setLogRedirect(boolean enable) {
        globalConfigManager.setLogRedirectEnabled(enable);
        if (logRedirectButton != null) {
            logRedirectButton.setMessage(toggleText(enable));
        }
    }

    private boolean areAllClassesAllowed() {
        return globalConfigManager.areAllClassesAllowed();
    }

    private void setAllClassesAllowed(boolean allowed) {
        globalConfigManager.setAllClassesAllowed(allowed);
        if (allowClassesButton != null) {
            allowClassesButton.setMessage(toggleText(allowed));
        }
    }

    private Text toggleText(boolean enabled) {
        return Text.literal(enabled ? "ON" : "OFF").formatted(enabled ? Formatting.GREEN : Formatting.RED, Formatting.BOLD);
    }
}

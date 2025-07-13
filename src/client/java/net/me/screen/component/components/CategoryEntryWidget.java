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

import net.me.category.Category;
import net.me.screen.component.IResizableWidget;
import net.me.screen.theme.GUIColors;
import net.me.screen.theme.UIConstants;
import net.me.utils.Render2DUtils;
import net.me.utils.TextRenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class CategoryEntryWidget extends ClickableWidget implements IResizableWidget {

    private final Category category;
    private final MQSButtonWidget editButton;
    private final MQSButtonWidget deleteButton;

    public CategoryEntryWidget(int x, int y, int width, int height, Category category, Consumer<Category> onEdit, Consumer<Category> onDelete) {
        super(x, y, width, height, Text.literal(""));
        this.category = category;

        this.editButton = MQSButtonWidget.mqsBuilder("Edit", (btn) -> onEdit.accept(this.category))
                .size(50, UIConstants.BUTTON_HEIGHT)
                .build();

        this.deleteButton = MQSButtonWidget.mqsBuilder("Delete", (btn) -> onDelete.accept(this.category))
                .size(50, UIConstants.BUTTON_HEIGHT)
                .backgroundColors(GUIColors.ERROR.darker(10).getRGB(), GUIColors.ERROR.getRGB())
                .build();

        updateButtonPositions();
    }

    private void updateButtonPositions() {
        int buttonY = this.getY() + (this.getHeight() - UIConstants.BUTTON_HEIGHT) / 2;
        int deleteButtonX = this.getX() + this.getWidth() - deleteButton.getWidth() - UIConstants.PADDING_S;
        int editButtonX = deleteButtonX - editButton.getWidth() - UIConstants.PADDING_S;

        this.editButton.setPos(editButtonX, buttonY);
        this.deleteButton.setPos(deleteButtonX, buttonY);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        Render2DUtils.drawRoundedRect(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 3, 10, GUIColors.DARK_L2.getRGB());
        if (this.isFocused()) {
            Render2DUtils.drawRoundedOutline(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 3, 1, 5, GUIColors.DARK_L4.getRGB());
        }

        TextRenderUtils.drawCustomText(
                context,
                category.name(),
                this.getX() + UIConstants.PADDING_M,
                this.getY() + (this.getHeight() - 8) / 2f,
                GUIColors.TEXT.getRGB(),
                true,
                1f
        );

        editButton.render(context, mouseX, mouseY, delta);
        deleteButton.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return editButton.mouseClicked(mouseX, mouseY, button) || deleteButton.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void setPos(int x, int y) {
        this.setX(x);
        this.setY(y);
        updateButtonPositions();
    }

    @Override
    public void setSize(int width, int height) {
        this.setWidth(width);
        this.setHeight(height);
        updateButtonPositions();
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
        editButton.appendNarrations(builder);
        deleteButton.appendNarrations(builder);
    }
}
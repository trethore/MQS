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

import net.me.screen.component.IResizableWidget;
import net.me.utils.GUIColors;
import net.me.utils.Render2DUtils;
import net.me.utils.TextRendererUtils;
import net.me.utils.UIConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

@SuppressWarnings("unused")
public class MQSTextFieldWidget extends TextFieldWidget implements IResizableWidget {
    private int defaultBackgroundColor;
    private int hoveredBackgroundColor;
    private int focusedBackgroundColor;

    protected MQSTextFieldWidget(int x, int y, int width, int height, String placeholder, int placeholderColor,
                                 int defaultBackgroundColor, int hoveredBackgroundColor, int focusedBackgroundColor) {
        super(TextRendererUtils.getCustomTextRenderer(), x, y, width, height, Text.empty());

        this.defaultBackgroundColor = defaultBackgroundColor;
        this.hoveredBackgroundColor = hoveredBackgroundColor;
        this.focusedBackgroundColor = focusedBackgroundColor;

        if (placeholder != null) {
            this.setPlaceholder(Text.literal(placeholder).fillStyle(Style.EMPTY.withColor(placeholderColor)));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public void setBackgroundColors(int defaultColor, int hoveredColor, int focusedColor) {
        this.defaultBackgroundColor = defaultColor;
        this.hoveredBackgroundColor = hoveredColor;
        this.focusedBackgroundColor = focusedColor;
    }

    @Override
    public void setPos(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public void setSize(int width, int height) {
        this.setWidth(width);
        this.setHeight(height);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        this.setDrawsBackground(false);

        renderCustomBackground(context);

        context.getMatrices().push();
        context.getMatrices().translate(UIConstants.PADDING_S, UIConstants.PADDING_S + 1, 0);

        super.renderWidget(context, mouseX, mouseY, delta);

        context.getMatrices().pop();
    }

    private void renderCustomBackground(DrawContext context) {
        int color;
        if (this.isFocused()) {
            color = this.focusedBackgroundColor;
        } else if (this.isHovered()) {
            color = this.hoveredBackgroundColor;
        } else {
            color = this.defaultBackgroundColor;
        }
        Render2DUtils.drawRoundedRect(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 3, 10, color);
    }


    public void clearText() {
        this.setText("");
    }

    public void setCursorToEnd() {
        this.setCursor(this.getText().length(), false);
    }

    public static class Builder {
        private int x = 0;
        private int y = 0;
        private int width = 200;
        private int height = 20;
        private String placeholder;
        private String text = "";

        private int defaultBackgroundColor = GUIColors.DARK_L2.getRGB();
        private int hoveredBackgroundColor = GUIColors.DARK_L3.getRGB();
        private int focusedBackgroundColor = GUIColors.DARK_L4.getRGB();
        private int placeholderColor = GUIColors.TEXT_DISABLED.getRGB();

        public Builder() {
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder placeholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public Builder placeholderColor(int placeholderColor) {
            this.placeholderColor = placeholderColor;
            return this;
        }

        public Builder backgroundColors(int defaultColor, int hoveredColor, int focusedColor) {
            this.defaultBackgroundColor = defaultColor;
            this.hoveredBackgroundColor = hoveredColor;
            this.focusedBackgroundColor = focusedColor;
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

        public Builder dimensions(int x, int y, int width, int height) {
            this.position(x, y);
            this.size(width, height);
            return this;
        }

        public MQSTextFieldWidget build() {
            MQSTextFieldWidget widget = new MQSTextFieldWidget(
                    this.x, this.y, this.width, this.height, this.placeholder, this.placeholderColor,
                    this.defaultBackgroundColor, this.hoveredBackgroundColor, this.focusedBackgroundColor
            );
            widget.setText(this.text);
            return widget;
        }
    }
}
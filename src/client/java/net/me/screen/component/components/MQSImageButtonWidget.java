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

import net.me.utils.Render2DUtils;
import net.me.utils.TextRendererUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;

@SuppressWarnings("unused")
public class MQSImageButtonWidget extends MQSButtonWidget {
    protected Identifier image;
    protected int imagePadding = 4;
    protected int imageTextGap = 4;
    protected Color imageColor = Color.WHITE;
    protected int imageWidth;
    protected int imageHeight;

    protected MQSImageButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, Identifier image,
                                   int imageWidth, int imageHeight,
                                   int nonHoveredBackgroundColor, int hoveredBackgroundColor,
                                   int inactiveTextColor, int activeTextColor) {
        super(x, y, width, height, message, onPress, nonHoveredBackgroundColor, hoveredBackgroundColor, inactiveTextColor, activeTextColor);
        this.image = image;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public static Builder mqsBuilder(Identifier image, String message, PressAction onPress) {
        return new Builder(image, Text.literal(message), onPress);
    }

    public static Builder mqsBuilder(Identifier image, Text message, PressAction onPress) {
        return new Builder(image, message, onPress);
    }

    public static Builder mqsBuilder(Identifier image, PressAction onPress) {
        return new Builder(image, Text.empty(), onPress);
    }

    public void setImage(Identifier image) {
        this.image = image;
    }

    @Override
    protected void renderContent(DrawContext context) {
        TextRenderer textRenderer = TextRendererUtils.getCustomTextRenderer();
        Text message = this.getMessage();
        boolean hasText = !message.getString().isEmpty();

        int finalImageWidth = this.imageWidth;
        int finalImageHeight = this.imageHeight;

        if (finalImageWidth <= 0 || finalImageHeight <= 0) {
            int calculatedSize = this.getHeight() - this.imagePadding * 2;
            finalImageWidth = calculatedSize;
            finalImageHeight = calculatedSize;
        }

        int textWidth = hasText ? textRenderer.getWidth(message) : 0;
        int gap = hasText ? this.imageTextGap : 0;
        int totalContentWidth = finalImageWidth + gap + textWidth;

        int imageX = this.getX() + (this.getWidth() - totalContentWidth) / 2;
        int imageY = this.getY() + (this.getHeight() - finalImageHeight) / 2;

        if (this.image != null) {
            Render2DUtils.drawImage(image, imageX, imageY, imageX + finalImageWidth, imageY + finalImageHeight, 0, false, this.imageColor);
        }

        if (hasText) {
            int textColor = this.active ? this.activeTextColor : this.inactiveTextColor;
            int textX = imageX + finalImageWidth + gap;
            int textY = this.getY() + (this.height - 8) / 2;
            context.drawTextWithShadow(
                    textRenderer,
                    message,
                    textX,
                    textY,
                    textColor
            );
        }
    }

    public static class Builder extends MQSButtonWidget.Builder {
        private Identifier image;
        private int imageWidth = 0;
        private int imageHeight = 0;

        public Builder(Identifier image, Text message, PressAction onPress) {
            super(message, onPress);
            this.image = image;
        }

        public Builder image(Identifier image) {
            this.image = image;
            return this;
        }

        public Builder imageSize(int width, int height) {
            this.imageWidth = width;
            this.imageHeight = height;
            return this;
        }

        @Override
        public Builder dimensions(int x, int y, int width, int height) {
            super.dimensions(x, y, width, height);
            return this;
        }

        @Override
        public Builder position(int x, int y) {
            super.position(x, y);
            return this;
        }

        @Override
        public Builder size(int width, int height) {
            super.size(width, height);
            return this;
        }

        @Override
        public Builder backgroundColors(int nonHovered, int hovered) {
            super.backgroundColors(nonHovered, hovered);
            return this;
        }

        @Override
        public MQSImageButtonWidget build() {
            if (image == null) {
                throw new IllegalStateException("Image must be set for MQSImageButtonWidget");
            }
            return new MQSImageButtonWidget(
                    this.x, this.y, this.width, this.height,
                    this.message, this.onPress, this.image,
                    this.imageWidth, this.imageHeight,
                    this.nonHoveredBackgroundColor, this.hoveredBackgroundColor,
                    this.inactiveTextColor, this.activeTextColor
            );
        }
    }
}
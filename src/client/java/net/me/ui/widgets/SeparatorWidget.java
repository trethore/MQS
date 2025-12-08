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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class SeparatorWidget extends ClickableWidget {

    public static final int LEFT = 12;
    public static final int MIDDLE = 50;
    public static final int RIGHT = 87;
    private static final int DEFAULT_HEIGHT = 10;
    private static final int LABEL_PADDING = 6;
    private static final int DEFAULT_COLOR = 0xFFFFFFFF;
    private final int color;
    private final int positionPercent;
    private final boolean hasLabel;

    public SeparatorWidget(int x, int y, int width) {
        this(x, y, width, DEFAULT_HEIGHT, Text.empty(), MIDDLE, DEFAULT_COLOR);
    }

    public SeparatorWidget(int x, int y, int width, Text label) {
        this(x, y, width, DEFAULT_HEIGHT, label, MIDDLE, DEFAULT_COLOR);
    }

    public  SeparatorWidget(int x, int y, int width, int height, Text label, int positionPercent) {
        this(x, y, width, height, label, positionPercent, DEFAULT_COLOR);
    }

    public SeparatorWidget(int x, int y, int width, int height, Text label, int positionPercent, int color) {
        super(x, y, width, height, label == null ? Text.empty() : label);
        this.positionPercent = MathHelper.clamp(positionPercent, 0, 100);
        this.color = color;
        this.hasLabel = label != null && !label.getString().isEmpty();
        this.active = false;
        this.visible = true;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerY = this.getY() + this.getHeight() / 2;
        if (!hasLabel) {
            context.fill(this.getX(), centerY, this.getX() + this.getWidth(), centerY + 1, color);
            return;
        }

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int labelWidth = textRenderer.getWidth(this.getMessage());
        int available = this.getWidth() - labelWidth - LABEL_PADDING * 2;

        if (available <= 0) {
            context.drawTextWithShadow(textRenderer, this.getMessage(), this.getX(), centerY - textRenderer.fontHeight / 2, color);
            return;
        }

        float ratio = this.positionPercent / 100.0f;
        int leftWidth = Math.round(available * ratio);

        int leftStart = this.getX();
        int leftEnd = leftStart + leftWidth;
        int labelStart = leftEnd + LABEL_PADDING;
        int labelY = centerY - textRenderer.fontHeight / 2;
        int rightStart = labelStart + labelWidth + LABEL_PADDING;
        int rightEnd = this.getX() + this.getWidth();

        context.fill(leftStart, centerY, leftEnd, centerY + 1, color);
        context.drawTextWithShadow(textRenderer, this.getMessage(), labelStart, labelY, color);
        context.fill(rightStart, centerY, rightEnd, centerY + 1, color);
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        // Non-interactive separator; no narration needed.
    }
}

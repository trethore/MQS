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

package net.me.utils;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

@SuppressWarnings("unused")
public final class TextRenderUtils {

    private TextRenderUtils() {
    }

    public static void drawText(DrawContext context, String text, float x, float y, int color, boolean shadow, float scale) {
        McUtils.getMc().ifPresent(mc -> drawTextInternal(context, mc.textRenderer, text, x, y, color, shadow, scale));
    }

    public static void drawCenteredText(DrawContext context, String text, float x, float y, int color, boolean shadow, float scale) {
        McUtils.getMc().ifPresent(mc -> {
            TextRenderer textRenderer = mc.textRenderer;
            float textWidth = textRenderer.getWidth(text) * scale;
            float textHeight = textRenderer.fontHeight * scale;

            float drawX = x - textWidth / 2.0f;
            float drawY = y - textHeight / 2.0f;

            drawText(context, text, drawX, drawY, color, shadow, scale);
        });
    }

    public static void drawCustomText(DrawContext context, String text, float x, float y, int color, boolean shadow, float scale) {
        drawTextInternal(context, TextRendererUtils.getCustomTextRenderer(), text, x, y, color, shadow, scale);
    }


    public static void drawCustomCenteredText(DrawContext context, String text, float x, float y, int color, boolean shadow, float scale) {
        TextRenderer textRenderer = TextRendererUtils.getCustomTextRenderer();
        float textWidth = textRenderer.getWidth(text) * scale;
        float textHeight = textRenderer.fontHeight * scale;

        float drawX = x - textWidth / 2.0f;
        float drawY = y - textHeight / 2.0f;

        drawCustomText(context, text, drawX, drawY, color, shadow, scale);
    }


    private static void drawTextInternal(DrawContext context, TextRenderer textRenderer, String text, float x, float y, int color, boolean shadow, float scale) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0);
        matrices.scale(scale, scale, 1.0f);
        context.drawText(textRenderer, Text.literal(text), 0, 0, color, shadow);
        matrices.pop();
    }
}

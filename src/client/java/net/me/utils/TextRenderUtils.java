package net.me.utils;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

@SuppressWarnings("unused")
public final class TextRenderUtils {

    private TextRenderUtils() {}

    public static void drawText(DrawContext context, String text, float x, float y, int color, boolean shadow, float scale) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0);
        matrices.scale(scale, scale, 1.0f);

        McUtils.getMc().ifPresent(mc ->
                context.drawText(mc.textRenderer, Text.literal(text), 0, 0, color, shadow));

        matrices.pop();
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

}

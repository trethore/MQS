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

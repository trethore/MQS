package net.me.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

import java.awt.*;

@SuppressWarnings("unused")
public final class Render2DUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private Render2DUtils() {
    }

    public static int getRainbowColor(long speed, float saturation, float brightness) {
        return getRainbowColor(0, speed, saturation, brightness);
    }

    public static int getRainbowColor(long offset, long speed, float saturation, float brightness) {
        float hue = ((System.currentTimeMillis() + offset) % speed) / (float) speed;
        return Color.HSBtoRGB(hue, saturation, brightness);
    }


    public static void drawRect(DrawContext context, float x, float y, float width, float height, int color) {
        drawRect(x, y, width, height, color);
    }

    public static void drawRect(float x, float y, float width, float height, int color) {
        float x2 = x + width;
        float y2 = y + height;

        BufferBuilder buffer = setupRender(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        buffer.vertex(x, y2, 0).color(color);
        buffer.vertex(x2, y2, 0).color(color);
        buffer.vertex(x2, y, 0).color(color);
        buffer.vertex(x, y, 0).color(color);

        endRender(buffer);
    }

    public static void drawOutline(DrawContext context, float x, float y, float width, float height, float lineWidth, int color) {
        drawRect(x, y, width, lineWidth, color); // Top
        drawRect(x, y + height - lineWidth, width, lineWidth, color); // Bottom
        drawRect(x, y + lineWidth, lineWidth, height - (lineWidth * 2), color); // Left
        drawRect(x + width - lineWidth, y + lineWidth, lineWidth, height - (lineWidth * 2), color); // Right
    }


    public static void drawRoundedRect(DrawContext context, float x, float y, float width, float height, float radius, float quality, int color) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        BufferBuilder buffer = setupRender(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        radius = Math.min(Math.min(width, height) / 2, radius);
        if (radius < 0) radius = 0;

        float x2 = x + width;
        float y2 = y + height;

        float centerX = x + width / 2;
        float centerY = y + height / 2;

        float lastVx, lastVy;

        {
            float cx = x2 - radius;
            float cy = y2 - radius;
            lastVx = cx + radius;
            lastVy = cy;
        }

        for (int i = (int) quality; i <= 360; i += (int) quality) {
            double angle = Math.toRadians(i);
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);

            float cx, cy;
            if (i >= 0 && i <= 90) {
                cx = x2 - radius;
                cy = y2 - radius;
            } else if (i > 90 && i <= 180) {
                cx = x + radius;
                cy = y2 - radius;
            } else if (i > 180 && i <= 270) {
                cx = x + radius;
                cy = y + radius;
            } else {
                cx = x2 - radius;
                cy = y + radius;
            }

            float currentVx = cx + (float) (cos * radius);
            float currentVy = cy + (float) (sin * radius);

            buffer.vertex(matrix, centerX, centerY, 0).color(color);
            buffer.vertex(matrix, lastVx, lastVy, 0).color(color);
            buffer.vertex(matrix, currentVx, currentVy, 0).color(color);

            lastVx = currentVx;
            lastVy = currentVy;
        }
        // handle last segment
        buffer.vertex(matrix, centerX, centerY, 0).color(color);
        buffer.vertex(matrix, lastVx, lastVy + height - radius * 2, 0).color(color);
        buffer.vertex(matrix, lastVx, lastVy, 0).color(color);
        endRender(buffer);
    }

    public static void drawRoundedOutline(DrawContext context, float x, float y, float width, float height, float radius, float lineWidth, float quality, int color) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = setupRender(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        radius = Math.min(Math.min(width, height) / 2, radius);

        float x2 = x + width;
        float y2 = y + height;

        for (int i = 0; i <= 450; i += (int) quality) {
            double angle = Math.toRadians(i);
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);

            float cx, cy, r_inner, r_outer;
            r_outer = radius;
            r_inner = radius - lineWidth;

            if (i >= 0 && i <= 90) {
                cx = x2 - radius;
                cy = y2 - radius;
            } else if (i > 90 && i <= 180) {
                cx = x + radius;
                cy = y2 - radius;
            } else if (i > 180 && i <= 270) {
                cx = x + radius;
                cy = y + radius;
            } else if (i > 270 && i <= 360) {
                cx = x2 - radius;
                cy = y + radius;
            } else { // handle last segment
                cx = x2 - radius;
                cy = y2 - radius;
            }

            buffer.vertex(matrix, cx + (float) (cos * r_outer), cy + (float) (sin * r_outer), 0).color(color);
            buffer.vertex(matrix, cx + (float) (cos * r_inner), cy + (float) (sin * r_inner), 0).color(color);
        }

        endRender(buffer);
    }


    public static void drawText(DrawContext context, String text, float x, float y, int color, boolean shadow, float scale) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0);
        matrices.scale(scale, scale, 1.0f);

        context.drawText(mc.textRenderer, Text.literal(text), 0, 0, color, shadow);

        matrices.pop();
    }

    public static void drawCenteredText(DrawContext context, String text, float x, float y, int color, boolean shadow, float scale) {
        TextRenderer textRenderer = mc.textRenderer;
        float textWidth = textRenderer.getWidth(text) * scale;
        float textHeight = textRenderer.fontHeight * scale;

        float drawX = x - textWidth / 2.0f;
        float drawY = y - textHeight / 2.0f;

        drawText(context, text, drawX, drawY, color, shadow, scale);
    }


    private static BufferBuilder setupRender(VertexFormat.DrawMode drawMode, VertexFormat vertexFormat) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        return Tessellator.getInstance().begin(drawMode, vertexFormat);
    }

    private static void endRender(BufferBuilder buffer) {
        BuiltBuffer builtBuffer = buffer.end();
        if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }
        RenderSystem.disableBlend();
    }
}
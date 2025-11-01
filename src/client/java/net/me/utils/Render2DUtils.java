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

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.awt.*;

@SuppressWarnings("unused")
public final class Render2DUtils {

    private static final MinecraftClient mc = McUtils.getMc().orElse(null);

    private Render2DUtils() {
    }

    private static BufferBuilder setupRender(ShaderProgramKey shaderProgramKey, VertexFormat.DrawMode drawMode, VertexFormat vertexFormat) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShader(shaderProgramKey);
        return Tessellator.getInstance().begin(drawMode, vertexFormat);
    }

    private static void endRender(BufferBuilder buffer) {
        BuiltBuffer builtBuffer = buffer.end();
        if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }
        RenderSystem.disableBlend();
    }

    public static void drawRect(DrawContext context, float x, float y, float width, float height, int color) {
        drawRect(x, y, width, height, color);
    }

    public static void drawRect(float x, float y, float width, float height, int color) {
        float x2 = x + width;
        float y2 = y + height;

        BufferBuilder buffer = setupRender(ShaderProgramKeys.POSITION_COLOR, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

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

        BufferBuilder buffer = setupRender(ShaderProgramKeys.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

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

        float step = Math.max(1.0f, Math.abs(quality));
        int iterations = (int) Math.ceil(360.0f / step);

        for (int index = 1; index <= iterations; index++) {
            float degrees = Math.min(index * step, 360.0f);
            double angle = Math.toRadians(degrees);
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);

            float cx, cy;
            if (degrees >= 0 && degrees <= 90) {
                cx = x2 - radius;
                cy = y2 - radius;
            } else if (degrees > 90 && degrees <= 180) {
                cx = x + radius;
                cy = y2 - radius;
            } else if (degrees > 180 && degrees <= 270) {
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
        BufferBuilder buffer = setupRender(ShaderProgramKeys.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        radius = Math.min(Math.min(width, height) / 2, radius);

        float x2 = x + width;
        float y2 = y + height;

        float step = Math.max(1.0f, Math.abs(quality));
        int iterations = (int) Math.ceil(450.0f / step);

        for (int index = 0; index <= iterations; index++) {
            float degrees = Math.min(index * step, 450.0f);
            double angle = Math.toRadians(degrees);
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);

            float cx, cy, r_inner, r_outer;
            r_outer = radius;
            r_inner = radius - lineWidth;

            if (degrees >= 0 && degrees <= 90) {
                cx = x2 - radius;
                cy = y2 - radius;
            } else if (degrees > 90 && degrees <= 180) {
                cx = x + radius;
                cy = y2 - radius;
            } else if (degrees > 180 && degrees <= 270) {
                cx = x + radius;
                cy = y + radius;
            } else if (degrees > 270 && degrees <= 360) {
                cx = x2 - radius;
                cy = y + radius;
            } else {
                cx = x2 - radius;
                cy = y2 - radius;
            }

            buffer.vertex(matrix, cx + (float) (cos * r_outer), cy + (float) (sin * r_outer), 0).color(color);
            buffer.vertex(matrix, cx + (float) (cos * r_inner), cy + (float) (sin * r_inner), 0).color(color);
        }

        endRender(buffer);
    }

    public static void enableScissor(DrawContext context, int x, int y, int width, int height) {
        context.enableScissor(x, y, x + width, y + height);
    }

    public static void disableScissor(DrawContext context) {
        context.disableScissor();
    }

    public static void drawImage(Identifier id, int x1, int y1, int x2, int y2, int rotation, boolean parity, Color color) {
        int[][] texCoords = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (int i = 0; i < rotation % 4; i++) {
            int temp1 = texCoords[3][0], temp2 = texCoords[3][1];
            texCoords[3][0] = texCoords[2][0];
            texCoords[3][1] = texCoords[2][1];
            texCoords[2][0] = texCoords[1][0];
            texCoords[2][1] = texCoords[1][1];
            texCoords[1][0] = texCoords[0][0];
            texCoords[1][1] = texCoords[0][1];
            texCoords[0][0] = temp1;
            texCoords[0][1] = temp2;
        }
        if (parity) {
            int temp1 = texCoords[1][0];
            texCoords[1][0] = texCoords[0][0];
            texCoords[0][0] = temp1;
            temp1 = texCoords[3][0];
            texCoords[3][0] = texCoords[2][0];
            texCoords[2][0] = temp1;
        }
        RenderSystem.setShaderTexture(0, id);
        BufferBuilder bufferbuilder = setupRender(ShaderProgramKeys.POSITION_TEX_COLOR, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferbuilder.vertex(x1, y2, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).texture(texCoords[0][0], texCoords[0][1]);
        bufferbuilder.vertex(x2, y2, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).texture(texCoords[1][0], texCoords[1][1]);
        bufferbuilder.vertex(x2, y1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).texture(texCoords[2][0], texCoords[2][1]);
        bufferbuilder.vertex(x1, y1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).texture(texCoords[3][0], texCoords[3][1]);
        endRender(bufferbuilder);
    }

    public static void drawRoundedRectDropShadowOutline(DrawContext context, float x, float y, float width, float height, float radius, int quality, int shadowSize, int shadowColor) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float r = ((shadowColor >> 16) & 0xFF) / 255f;
        float g = ((shadowColor >> 8) & 0xFF) / 255f;
        float b = (shadowColor & 0xFF) / 255f;
        float a = ((shadowColor >> 24) & 0xFF) / 255f;

        BufferBuilder buffer = setupRender(ShaderProgramKeys.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        float totalSteps = quality * 4;
        float step = 360 / totalSteps;

        for (int i = 0; i <= totalSteps; i++) {
            float angle = (float) Math.toRadians(i * step);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            float centerX, centerY;
            if (i * step >= 0 && i * step < 90) {
                centerX = x + width - radius;
                centerY = y + height - radius;
            } else if (i * step >= 90 && i * step < 180) {
                centerX = x + radius;
                centerY = y + height - radius;
            } else if (i * step >= 180 && i * step < 270) {
                centerX = x + radius;
                centerY = y + radius;
            } else {
                centerX = x + width - radius;
                centerY = y + radius;
            }

            float outerX = centerX + (float) (cos * (radius + shadowSize));
            float outerY = centerY + (float) (sin * (radius + shadowSize));
            buffer.vertex(matrix, outerX, outerY, 0).color(r, g, b, 0);

            float innerX = centerX + (float) (cos * radius);
            float innerY = centerY + (float) (sin * radius);
            buffer.vertex(matrix, innerX, innerY, 0).color(r, g, b, a);

        }

        float startAngle = 0;
        double startCos = Math.cos(startAngle);
        double startSin = Math.sin(startAngle);
        float startCenterX = x + width - radius;
        float startCenterY = y + height - radius;

        float startOuterX = startCenterX + (float) (startCos * (radius + shadowSize));
        float startOuterY = startCenterY + (float) (startSin * (radius + shadowSize));
        buffer.vertex(matrix, startOuterX, startOuterY, 0).color(r, g, b, 0);

        float startInnerX = startCenterX + (float) (startCos * radius);
        float startInnerY = startCenterY + (float) (startSin * radius);
        buffer.vertex(matrix, startInnerX, startInnerY, 0).color(r, g, b, a);

        endRender(buffer);
    }

}

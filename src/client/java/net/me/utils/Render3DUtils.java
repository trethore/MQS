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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.me.Main;
import net.me.utils.math.Box6d;
import net.me.utils.math.Vector3f;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

@SuppressWarnings("unused")
public final class Render3DUtils {
    public static final Box6d DEFAULT_BOX = new Box6d(0, 0, 0, 1, 1, 1);

    private Render3DUtils() {
    }

    public static BufferBuilder setupRender(ShaderProgramKey key, VertexFormat.DrawMode drawMode, VertexFormat vertexFormat, boolean espMode) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShader(key);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableBlend();
        if (espMode) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthFunc(515);
            RenderSystem.defaultBlendFunc();
        }
        return Tessellator.getInstance().begin(drawMode, vertexFormat);
    }

    public static void endRender(BufferBuilder bufferBuilder) {
        BuiltBuffer builtBuffer = bufferBuilder.endNullable();
        if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    public static void drawLine(DrawContext context, BufferBuilder bufferBuilder, Color color, float x1, float y1, float z1, float x2, float y2, float z2, float width, boolean espMode) {
        McUtils.getMc().map(mc -> {
            GameRenderer renderer = mc.gameRenderer;
            if (renderer == null || renderer.getCamera() == null || !renderer.getCamera().isReady()) {
                Main.LOGGER.error("Error: GameRenderer or Camera is null or not ready when drawing a line.");
                return null;
            }
            return renderer.getCamera();
        }).ifPresentOrElse(camera -> {
            RenderSystem.lineWidth(width);
            Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

            Vec3d camPos = camera.getPos().negate();

            Vector3f v1 = new Vector3f(x1, y1, z1).add((float) camPos.x, (float) camPos.y, (float) camPos.z);
            Vector3f v2 = new Vector3f(x2, y2, z2).add((float) camPos.x, (float) camPos.y, (float) camPos.z);

            bufferBuilder.vertex(mat, v1.x(), v1.y(), v1.z()).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
            bufferBuilder.vertex(mat, v2.x(), v2.y(), v2.z()).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }, () -> Main.LOGGER.error("Error: MinecraftClient instance is not present when drawing a line."));
    }


    public static void drawBox(DrawContext context, BufferBuilder bufferBuilder, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color color, boolean espMode) {
        McUtils.getMc().map(mc -> {
            GameRenderer gameRenderer = mc.gameRenderer;
            if (gameRenderer == null || gameRenderer.getCamera() == null || !gameRenderer.getCamera().isReady()) {
                Main.LOGGER.error("Error: GameRenderer or Camera is null or not ready when drawing a box.");
                return null;
            }
            return gameRenderer.getCamera();
        }).ifPresentOrElse(camera -> {
            Vec3d vec3d = camera.getPos().negate();

            Vector3f v1 = new Vector3f(minX, minY, minZ);
            Vector3f v2 = new Vector3f(maxX, maxY, maxZ);

            v1 = v1.add((float) vec3d.x, (float) vec3d.y, (float) vec3d.z);
            v2 = v2.add((float) vec3d.x, (float) vec3d.y, (float) vec3d.z);

            Matrix4f matrix4f = context.getMatrices().peek().getPositionMatrix();

            doDrawBox(bufferBuilder, matrix4f, v1.x(), v1.y(), v1.z(), v2.x(), v2.y(), v2.z(), color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }, () -> Main.LOGGER.error("Error: MinecraftClient instance is not present when drawing a box."));
    }

    private static void doDrawBox(BufferBuilder bufferBuilder, Matrix4f matrix4f, float boxMinX, float boxMinY, float boxMinZ, float boxMaxX, float boxMaxY, float boxMaxZ, int red, int green, int blue, int alpha) {
        bufferBuilder.vertex(matrix4f, boxMinX, boxMinY, boxMinZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMinX, boxMinY, boxMaxZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMinX, boxMaxY, boxMinZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMinX, boxMaxY, boxMaxZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMinX, boxMaxY, boxMaxZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMinX, boxMinY, boxMaxZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMaxX, boxMaxY, boxMaxZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMaxX, boxMinY, boxMaxZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMaxX, boxMinY, boxMaxZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMaxX, boxMinY, boxMinZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMaxX, boxMaxY, boxMaxZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMaxX, boxMaxY, boxMinZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMaxX, boxMaxY, boxMinZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMaxX, boxMinY, boxMinZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMinX, boxMaxY, boxMinZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMinX, boxMinY, boxMinZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMinX, boxMinY, boxMinZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMaxX, boxMinY, boxMinZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMinX, boxMinY, boxMaxZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMaxX, boxMinY, boxMaxZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMinX, boxMaxY, boxMaxZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMaxX, boxMaxY, boxMaxZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMinX, boxMaxY, boxMinZ).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix4f, boxMaxX, boxMaxY, boxMinZ).color(red, green, blue, alpha);
    }


    public static Box6d getInterpolatedBoundingBox(LivingEntity entity, float partialTicks) {
        double interpolatedX = entity.prevX + (entity.getX() - entity.prevX) * partialTicks;
        double interpolatedY = entity.prevY + (entity.getY() - entity.prevY) * partialTicks;
        double interpolatedZ = entity.prevZ + (entity.getZ() - entity.prevZ) * partialTicks;

        return new Box6d(
                entity.getBoundingBox().minX - entity.getX() + interpolatedX,
                entity.getBoundingBox().minY - entity.getY() + interpolatedY,
                entity.getBoundingBox().minZ - entity.getZ() + interpolatedZ,
                entity.getBoundingBox().maxX - entity.getX() + interpolatedX,
                entity.getBoundingBox().maxY - entity.getY() + interpolatedY,
                entity.getBoundingBox().maxZ - entity.getZ() + interpolatedZ
        );
    }

}

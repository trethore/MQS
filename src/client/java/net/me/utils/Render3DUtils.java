package net.me.utils;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.me.utils.records.Box6d;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

@SuppressWarnings("unused")
public final class Render3DUtils {
    private Render3DUtils() {}

    public static final Box6d DEFAULT_BOX = new Box6d(0,0,0, 1, 1, 1);

    public static BufferBuilder setupRender(ShaderProgramKey key, VertexFormat.DrawMode drawMode, VertexFormat vertexFormat, boolean espMode) {
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


    public static void drawBox(DrawContext context, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color color, boolean espMode) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        float alpha = (float) (color.getRGB() >> 24 & 255) / 255.0F;
        float red = (float) (color.getRGB() >> 16 & 255) / 255.0F;
        float green = (float) (color.getRGB() >> 8 & 255) / 255.0F;
        float blue = (float) (color.getRGB() & 255) / 255.0F;

        Camera camera = McUtils.getMc().map(mc -> {
            GameRenderer gameRenderer = mc.gameRenderer;
            if (gameRenderer == null || gameRenderer.getCamera() == null || !gameRenderer.getCamera().isReady()) return null;
            return gameRenderer.getCamera();
        }).orElse(null);

        if (camera == null) {
            return;
        }
        Vec3d vec3d = camera.getPos().negate();
        Vec3d v1 = new Vec3d(minX, minY, minZ);
        Vec3d v2 = new Vec3d(maxX, maxY, maxZ);
        v1.add(vec3d);
        v2.add(vec3d);

        float boxMinX = (float) v1.getX();
        float boxMinY = (float) v1.getY();
        float boxMinZ = (float) v1.getZ();
        float boxMaxX = (float) v2.getX();
        float boxMaxY = (float) v2.getY();
        float boxMaxZ = (float) v2.getZ();

        BufferBuilder bufferBuilder = setupRender(ShaderProgramKeys.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR,espMode);
        Matrix4f matrix4f = context.getMatrices().peek().getPositionMatrix();
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

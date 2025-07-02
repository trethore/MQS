package net.me.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@SuppressWarnings("unused")
public final class CameraUtils {
    private CameraUtils() {
    }

    public static Vec3d getCameraPos() {
        return McUtils.getMc()
                .map(mc -> mc.getBlockEntityRenderDispatcher().camera.getPos())
                .orElse(Vec3d.ZERO);
    }


    public static BlockPos getCameraBlockPos() {
        return McUtils.getMc()
                .map(mc -> mc.getBlockEntityRenderDispatcher().camera.getBlockPos())
                .orElse(BlockPos.ORIGIN);
    }

}

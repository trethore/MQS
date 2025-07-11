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

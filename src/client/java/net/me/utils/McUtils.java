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

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

import java.util.Optional;

@SuppressWarnings("unused")
public final class McUtils {

    private McUtils() {
    }

    public static Optional<Minecraft> getMc() {
        return Optional.of(Minecraft.getInstance());
    }

    public static Optional<LocalPlayer> getPlayer() {
        return getMc().map(mc -> mc.player);
    }

    public static Optional<ClientLevel> getWorld() {
        return getMc().map(mc -> mc.level);
    }
}

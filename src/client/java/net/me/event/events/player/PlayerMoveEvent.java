/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Réthoré
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

package net.me.event.events.player;

import lombok.Getter;
import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("unused")
public class PlayerMoveEvent extends CancellableEvent {
    @Getter
    private final LocalPlayer player;
    private final MoverType type;
    @Getter
    private final Vec3 movement;

    public PlayerMoveEvent(LocalPlayer player, MoverType type, Vec3 movement) {
        this.player = player;
        this.type = type;
        this.movement = movement;
    }

    public MoverType getMouvementType() {
        return type;
    }

    @Override
    public Events getType() {
        return Events.PLAYER_MOVE_EVENT;
    }
}

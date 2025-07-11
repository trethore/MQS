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

package net.me.event.events.player;

import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

@SuppressWarnings("unused")
public class PlayerMoveEvent extends CancellableEvent {
    private final ClientPlayerEntity player;
    private final MovementType type;
    private final Vec3d movement;

    public PlayerMoveEvent(ClientPlayerEntity player, MovementType type, Vec3d movement) {
        this.player = player;
        this.type = type;
        this.movement = movement;
    }

    public ClientPlayerEntity getPlayer() {
        return player;
    }

    public MovementType getMouvementType() {
        return type;
    }

    public Vec3d getMovement() {
        return movement;
    }

    @Override
    public Events getType() {
        return Events.PlayerMoveEvent;
    }
}

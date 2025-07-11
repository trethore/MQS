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

package net.me.event.events.interact;

import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

@SuppressWarnings("unused")
public class BlockInteractEvent extends CancellableEvent {
    private final PlayerEntity player;
    private final Hand hand;
    private final BlockHitResult hitResult;

    public BlockInteractEvent(PlayerEntity player, Hand hand, BlockHitResult hitResult) {
        this.player = player;
        this.hand = hand;
        this.hitResult = hitResult;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public Hand getHand() {
        return hand;
    }

    public BlockHitResult getHitResult() {
        return hitResult;
    }

    @Override
    public Events getType() {
        return Events.BlockInteractEvent;
    }
}
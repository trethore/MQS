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

import lombok.Getter;
import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

@Getter
@SuppressWarnings("unused")
public class ItemUseEvent extends CancellableEvent {
    private final Player player;
    private final InteractionHand hand;
    private final ItemStack itemStack;

    public ItemUseEvent(Player player, InteractionHand hand) {
        this.player = player;
        this.hand = hand;
        this.itemStack = player.getStackInHand(hand);
    }

    @Override
    public Events getType() {
        return Events.ItemUseEvent;
    }
}

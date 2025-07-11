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

package net.me.event.events.title;

import net.me.event.Events;
import net.me.event.PacketEvent;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;

public class TitleEvent extends PacketEvent<TitleS2CPacket> {

    public TitleEvent(TitleS2CPacket packet) {
        super(packet);
    }

    @Override
    public Events getType() {
        return Events.TitleEvent;
    }
}
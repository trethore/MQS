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

package net.me.mixin.client.mixins;

import io.netty.channel.ChannelHandlerContext;
import net.me.event.MQSEventBus;
import net.me.event.events.packet.ClientPacketInputEvent;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MQSClientConnectionMixin {
    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onChannelRead0Head(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        ClientPacketInputEvent event = new ClientPacketInputEvent(packet);
        MQSEventBus.post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
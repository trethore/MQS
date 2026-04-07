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

package net.me.mixin.client.mixins;

import net.me.event.MQSEventBus;
import net.me.event.events.player.PlayerRespawnEvent;
import net.me.event.events.title.SubTitleEvent;
import net.me.event.events.title.TitleEvent;
import net.me.utils.McUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MQSClientPlayNetworkHandlerMixin {

    @Inject(method = "setTitleText", at = @At("HEAD"), cancellable = true)
    private void onTitleHead(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
        TitleEvent event = new TitleEvent(packet);
        MQSEventBus.post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "setSubtitleText", at = @At("HEAD"), cancellable = true)
    private void onSubtitleHead(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
        SubTitleEvent event = new SubTitleEvent(packet);
        MQSEventBus.post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void onPlayerRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        McUtils.getPlayer().ifPresent(player -> {
            PlayerRespawnEvent event = new PlayerRespawnEvent(packet, player);
            MQSEventBus.post(event);
        });
    }
}

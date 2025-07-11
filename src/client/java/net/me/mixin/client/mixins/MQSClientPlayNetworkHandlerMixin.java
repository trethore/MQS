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

import net.me.event.MQSEventBus;
import net.me.event.events.player.PlayerRespawnEvent;
import net.me.event.events.title.SubTitleEvent;
import net.me.event.events.title.TitleEvent;
import net.me.utils.McUtils;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MQSClientPlayNetworkHandlerMixin {

    @Inject(method = "onTitle", at = @At("HEAD"), cancellable = true)
    private void onTitleHead(TitleS2CPacket packet, CallbackInfo ci) {
        TitleEvent event = new TitleEvent(packet);
        MQSEventBus.post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onSubtitle", at = @At("HEAD"), cancellable = true)
    private void onSubtitleHead(SubtitleS2CPacket packet, CallbackInfo ci) {
        SubTitleEvent event = new SubTitleEvent(packet);
        MQSEventBus.post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onPlayerRespawn", at = @At("HEAD"))
    private void onPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        McUtils.getPlayer().ifPresent(player -> {
            PlayerRespawnEvent event = new PlayerRespawnEvent(packet, player);
            MQSEventBus.post(event);
        });
    }
}
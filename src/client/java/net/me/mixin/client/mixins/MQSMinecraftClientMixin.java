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

import net.me.Main;
import net.me.event.MQSEventBus;
import net.me.event.events.MinecraftClientStopEvent;
import net.me.event.events.world.WorldChangeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MQSMinecraftClientMixin {
    @Shadow
    @Nullable
    public LocalPlayer player;

    @Inject(at = @At("HEAD"), method = "stop()V")
    private void onStop(CallbackInfo ci) {
        Main main = Main.getInstance();
        main.getGlobalConfigManager().save();
        main.getConfigManager().saveAllConfigs();
        MQSEventBus.post(new MinecraftClientStopEvent());
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void onSetWorld(ClientLevel world, CallbackInfo ci) {
        WorldChangeEvent event = new WorldChangeEvent(world);
        MQSEventBus.post(event);
    }
}

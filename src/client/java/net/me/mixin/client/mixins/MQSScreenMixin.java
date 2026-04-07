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
import net.me.event.events.screen.*;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MQSScreenMixin {

    @Inject(method = "init(II)V", at = @At("HEAD"))
    private void onFirstInitHead(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ScreenFirstInitEvent event = new ScreenFirstInitEvent(screen);
        MQSEventBus.post(event);
    }

    @Inject(method = "init()V", at = @At("TAIL"))
    private void onInitTail(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ScreenInitEvent event = new ScreenInitEvent(screen);
        MQSEventBus.post(event);
    }

    @Inject(method = "added", at = @At("HEAD"))
    private void onDisplayedHead(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ScreenDisplayedEvent event = new ScreenDisplayedEvent(screen);
        MQSEventBus.post(event);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemovedHead(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ScreenRemovedEvent event = new ScreenRemovedEvent(screen);
        MQSEventBus.post(event);
    }

    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void onCloseHead(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ScreenCloseEvent event = new ScreenCloseEvent(screen);
        MQSEventBus.post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}

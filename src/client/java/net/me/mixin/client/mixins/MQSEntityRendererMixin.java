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

import com.mojang.blaze3d.vertex.PoseStack;
import net.me.event.MQSEventBus;
import net.me.event.events.render.EntityRenderEvent;
import net.me.event.events.render.NameTagRenderEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MQSEntityRendererMixin<S extends EntityRenderState> {

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void onRenderPre(S state, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        EntityRenderEvent.Pre<S> event = new EntityRenderEvent.Pre<>(state, matrices, submitNodeCollector, state.lightCoords);
        MQSEventBus.post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "submit", at = @At("TAIL"))
    private void onRenderPost(S state, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        EntityRenderEvent.Post<S> event = new EntityRenderEvent.Post<>(state, matrices, submitNodeCollector, state.lightCoords);
        MQSEventBus.post(event);
    }

    @Inject(method = "submitNameTag", at = @At("HEAD"), cancellable = true)
    private void onRenderLabelHead(S state, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        Component text = state.nameTag;
        NameTagRenderEvent<S> event = new NameTagRenderEvent<>(state, text, matrices, submitNodeCollector, state.lightCoords);
        MQSEventBus.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}

package net.me.mixin.client.mixins;

import net.me.event.MQSEventBus;
import net.me.event.events.render.HudRenderEvent;
import net.me.screen.component.components.MQSToast;
import net.me.utils.McUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MQSHudMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        // toast render
        McUtils.getMc().ifPresent(mc -> {
            if (mc.currentScreen == null) {
                MQSToast.renderAll(context);
            }
        });

        HudRenderEvent event = new HudRenderEvent(context, tickCounter);
        MQSEventBus.post(event);
    }
}
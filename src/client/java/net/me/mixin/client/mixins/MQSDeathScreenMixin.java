package net.me.mixin.client.mixins;

import net.me.event.MQSEventBus;
import net.me.event.events.screen.deathscreen.DeathScreenInitEvent;
import net.minecraft.client.gui.screen.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(DeathScreen.class)
public class MQSDeathScreenMixin {

    @Inject(at = @At("TAIL"), method = "init()V")
    private void onInit(CallbackInfo ci) {
        MQSEventBus.post(new DeathScreenInitEvent((DeathScreen) (Object) this));
    }
}

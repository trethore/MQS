package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.screen.TitleScreenInitEvent;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MQSTitleScreenMixin {

    @Inject(at = @At("TAIL"), method = "init()V")
    private void onInit(CallbackInfo ci) {
        Main.getInstance().getEventManager().post(new TitleScreenInitEvent((TitleScreen)(Object)this));
    }
}

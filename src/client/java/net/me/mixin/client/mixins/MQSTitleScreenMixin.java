package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.screen.titlescreen.TitleScreenInitEvent;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MQSTitleScreenMixin {

    @Inject(method = "init()V", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        Main.getInstance().getEventManager().post(new TitleScreenInitEvent((TitleScreen) (Object) this));
    }
}

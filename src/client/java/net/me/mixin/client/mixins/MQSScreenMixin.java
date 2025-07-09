package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.screen.*;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MQSScreenMixin {

    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("HEAD"))
    private void onFirstInitHead(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ScreenFirstInitEvent event = new ScreenFirstInitEvent(screen);
        Main.getInstance().getEventManager().post(event);
    }

    @Inject(method = "init()V", at = @At("TAIL"))
    private void onInitTail(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ScreenInitEvent event = new ScreenInitEvent(screen);
        Main.getInstance().getEventManager().post(event);
    }

    @Inject(method = "onDisplayed", at = @At("HEAD"))
    private void onDisplayedHead(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ScreenDisplayedEvent event = new ScreenDisplayedEvent(screen);
        Main.getInstance().getEventManager().post(event);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemovedHead(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ScreenRemovedEvent event = new ScreenRemovedEvent(screen);
        Main.getInstance().getEventManager().post(event);
    }

    @Inject(method = "close", at = @At("HEAD"), cancellable = true)
    private void onCloseHead(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ScreenCloseEvent event = new ScreenCloseEvent(screen);
        Main.getInstance().getEventManager().post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.MinecraftClientStopEvent;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MQSMinecraftClientMixin {
    @Inject(at = @At("HEAD"), method = "run")
    private void init(CallbackInfo info) {
        // System.out.println("Client has started!");
    }

    @Inject(at = @At("HEAD"), method = "stop()V")
    private void onStop(CallbackInfo ci) {
        Main.getGlobalConfigManager().save();
        Main.getConfigManager().saveAllConfigs();
        Main.getEventManager().post(new MinecraftClientStopEvent());
    }
}
package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.MinecraftClientStopEvent;
import net.me.event.events.world.WorldChangeEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MQSMinecraftClientMixin {
    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Inject(at = @At("HEAD"), method = "run")
    private void init(CallbackInfo info) {
        // System.out.println("Client has started!");
    }

    @Inject(at = @At("HEAD"), method = "stop()V")
    private void onStop(CallbackInfo ci) {
        Main main = Main.getInstance();
        main.getGlobalConfigManager().save();
        main.getConfigManager().saveAllConfigs();
        main.getEventManager().post(new MinecraftClientStopEvent());
    }

    @Inject(method = "setWorld", at = @At("HEAD"))
    private void onSetWorld(ClientWorld world, CallbackInfo ci) {
        WorldChangeEvent event = new WorldChangeEvent(world);
        Main.getInstance().getEventManager().post(event);
    }
}
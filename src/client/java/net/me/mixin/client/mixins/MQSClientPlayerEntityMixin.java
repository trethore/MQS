package net.me.mixin.client.mixins;

import net.me.event.MQSEventBus;
import net.me.event.events.player.PlayerMoveEvent;
import net.me.event.events.tick.ClientPlayerTickEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class MQSClientPlayerEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        ClientPlayerTickEvent event = new ClientPlayerTickEvent(player);
        MQSEventBus.post(event);
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void onMove(MovementType type, Vec3d movement, CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        PlayerMoveEvent event = new PlayerMoveEvent(player, type, movement);
        MQSEventBus.post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
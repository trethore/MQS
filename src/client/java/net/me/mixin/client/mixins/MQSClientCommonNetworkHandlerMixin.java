package net.me.mixin.client.mixins;

import net.me.event.MQSEventBus;
import net.me.event.events.packet.ClientPacketOutputEvent;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public class MQSClientCommonNetworkHandlerMixin {

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    public void onSendPacketHead(Packet<?> packet, CallbackInfo ci) {
        ClientPacketOutputEvent event = new ClientPacketOutputEvent(packet);
        MQSEventBus.post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
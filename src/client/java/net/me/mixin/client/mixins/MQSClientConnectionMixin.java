package net.me.mixin.client.mixins;

import io.netty.channel.ChannelHandlerContext;
import net.me.event.MQSEventBus;
import net.me.event.events.packet.ClientPacketInputEvent;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MQSClientConnectionMixin {
    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onChannelRead0Head(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        ClientPacketInputEvent event = new ClientPacketInputEvent(packet);
        MQSEventBus.post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
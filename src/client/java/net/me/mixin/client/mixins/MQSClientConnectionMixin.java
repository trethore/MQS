package net.me.mixin.client.mixins;

import io.netty.channel.ChannelHandlerContext;
import net.me.Main;
import net.me.event.events.packet.ClientPacketInputEvent;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MQSClientConnectionMixin {

    @Unique
    private final ThreadLocal<ClientPacketInputEvent> currentPacketEvent = new ThreadLocal<>();

    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onChannelRead0Head(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        ClientPacketInputEvent event = new ClientPacketInputEvent(packet);

        Main.getInstance().getEventManager().post(event);

        if (event.isCancelled()) {
            ci.cancel();
        } else {
            currentPacketEvent.set(event);
        }
    }

    @ModifyVariable(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Packet<?> onChannelRead0ModifyPacket(Packet<?> originalPacket) {
        ClientPacketInputEvent event = currentPacketEvent.get();

        currentPacketEvent.remove();

        if (event != null) {
            return event.getPacket();
        }

        return originalPacket;
    }
}
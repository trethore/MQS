package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.packet.ClientPacketOutputEvent;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public class MQSClientCommonNetworkHandlerMixin {

    @Unique
    private final ThreadLocal<ClientPacketOutputEvent> currentPacketEvent = new ThreadLocal<>();

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    public void onSendPacketHead(Packet<?> packet, CallbackInfo ci) {
        ClientPacketOutputEvent event = new ClientPacketOutputEvent(packet);
        Main.getInstance().getEventManager().post(event);

        if (event.isCancelled()) {
            ci.cancel();
        } else {
            currentPacketEvent.set(event);
        }
    }

    @ModifyVariable(method = "sendPacket", at = @At("HEAD"), argsOnly = true)
    private Packet<?> onSendPacketModify(Packet<?> originalPacket) {
        ClientPacketOutputEvent event = currentPacketEvent.get();
        currentPacketEvent.remove();

        if (event != null) {
            return event.getPacket();
        }

        return originalPacket;
    }
}
package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.packet.ClientPacketOutputEvent;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public class MQSClientCommonNetworkHandlerMixin {
    @Inject(method = "sendPacket", at = @At("HEAD"))
    public void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        Main.getInstance().getEventManager().post(new ClientPacketOutputEvent(packet));
    }

}

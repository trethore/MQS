package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.title.SubTitleEvent;
import net.me.event.events.title.TitleEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MQSClientPlayNetworkHandlerMixin {

    @Inject(method = "onTitle", at = @At("HEAD"))
    private void onTitle(TitleS2CPacket packet, CallbackInfo info) {
        Main.getInstance().getEventManager().post(new TitleEvent(packet));
    }

    @Inject(method = "onSubtitle", at = @At("HEAD"))
    private void onSubtitle(SubtitleS2CPacket packet, CallbackInfo ci) {
        Main.getInstance().getEventManager().post(new SubTitleEvent(packet));
    }
}

package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.player.PlayerRespawnEvent;
import net.me.event.events.title.SubTitleEvent;
import net.me.event.events.title.TitleEvent;
import net.me.utils.McUtils;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MQSClientPlayNetworkHandlerMixin {

    @Unique
    private final ThreadLocal<TitleEvent> currentTitleEvent = new ThreadLocal<>();
    @Unique
    private final ThreadLocal<SubTitleEvent> currentSubTitleEvent = new ThreadLocal<>();

    @Inject(method = "onTitle", at = @At("HEAD"), cancellable = true)
    private void onTitleHead(TitleS2CPacket packet, CallbackInfo ci) {
        TitleEvent event = new TitleEvent(packet);
        Main.getInstance().getEventManager().post(event);

        if (event.isCancelled()) {
            ci.cancel();
        } else {
            currentTitleEvent.set(event);
        }
    }

    @ModifyVariable(method = "onTitle", at = @At("HEAD"), argsOnly = true)
    private TitleS2CPacket onTitleModify(TitleS2CPacket originalPacket) {
        TitleEvent event = currentTitleEvent.get();
        currentTitleEvent.remove();

        if (event != null) {
            return event.getPacket();
        }
        return originalPacket;
    }

    @Inject(method = "onSubtitle", at = @At("HEAD"), cancellable = true)
    private void onSubtitleHead(SubtitleS2CPacket packet, CallbackInfo ci) {
        SubTitleEvent event = new SubTitleEvent(packet);
        Main.getInstance().getEventManager().post(event);

        if (event.isCancelled()) {
            ci.cancel();
        } else {
            currentSubTitleEvent.set(event);
        }
    }

    @ModifyVariable(method = "onSubtitle", at = @At("HEAD"), argsOnly = true)
    private SubtitleS2CPacket onSubtitleModify(SubtitleS2CPacket originalPacket) {
        SubTitleEvent event = currentSubTitleEvent.get();
        currentSubTitleEvent.remove();

        if (event != null) {
            return event.getPacket();
        }
        return originalPacket;
    }

    @Inject(method = "onPlayerRespawn", at = @At("HEAD"))
    private void onPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        PlayerRespawnEvent event = new PlayerRespawnEvent(packet, McUtils.getPlayer().orElse(null));
        Main.getInstance().getEventManager().post(event);
    }
}
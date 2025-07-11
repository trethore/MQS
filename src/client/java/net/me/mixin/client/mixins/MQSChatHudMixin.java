package net.me.mixin.client.mixins;

import net.me.event.MQSEventBus;
import net.me.event.events.chat.ChatMessageReceivedEvent;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class MQSChatHudMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessageHead(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(message, signature, indicator);
        MQSEventBus.post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
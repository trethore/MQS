package net.me.event.events.chat;

import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ChatMessageReceivedEvent extends CancellableEvent {
    private final MessageSignatureData signature;
    private final MessageIndicator indicator;
    private Text message;

    public ChatMessageReceivedEvent(Text message, @Nullable MessageSignatureData signature, @Nullable MessageIndicator indicator) {
        this.message = message;
        this.signature = signature;
        this.indicator = indicator;
    }

    public Text getMessage() {
        return message;
    }

    public void setMessage(Text message) {
        this.message = message;
    }

    @Nullable
    public MessageSignatureData getSignature() {
        return signature;
    }

    @Nullable
    public MessageIndicator getIndicator() {
        return indicator;
    }

    @Override
    public Events getType() {
        return Events.ChatMessageReceivedEvent;
    }
}
/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
    private final Text message;

    public ChatMessageReceivedEvent(Text message, MessageSignatureData signature, MessageIndicator indicator) {
        this.message = message;
        this.signature = signature;
        this.indicator = indicator;
    }

    public Text getMessage() {
        return message;
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
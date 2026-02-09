/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Réthoré
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

import lombok.Getter;
import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ChatMessageReceivedEvent extends CancellableEvent {
    private final MessageSignature signature;
    private final GuiMessageTag indicator;
    @Getter
    private final Component message;

    public ChatMessageReceivedEvent(Component message, MessageSignature signature, GuiMessageTag indicator) {
        this.message = message;
        this.signature = signature;
        this.indicator = indicator;
    }

    @Nullable
    public MessageSignature getSignature() {
        return signature;
    }

    @Nullable
    public GuiMessageTag getIndicator() {
        return indicator;
    }

    @Override
    public Events getType() {
        return Events.CHAT_MESSAGE_RECEIVED_EVENT;
    }
}

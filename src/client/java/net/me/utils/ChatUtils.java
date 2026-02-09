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

package net.me.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

@SuppressWarnings("unused")
public final class ChatUtils {
    public static final String TAG = ChatFormatting.GRAY + "[" + ChatFormatting.GREEN + "MQS" + ChatFormatting.GRAY + "] " + ChatFormatting.RESET;

    private ChatUtils() {
    }

    public static void sendChatMessage(String s) {
        McUtils.getPlayer().ifPresent(player ->
                player.connection.sendChat(s)
        );
    }

    public static void sendChatCommand(String s) {
        McUtils.getPlayer().ifPresent(player ->
                player.connection.sendCommand(s)
        );
    }

    public static void addInfoChatMessage(String message, boolean prefix) {
        addChatMessage(message, Level.INFO, prefix);
    }

    public static void addWarnChatMessage(String message, boolean prefix) {
        addChatMessage(message, Level.WARN, prefix);
    }

    public static void addErrorChatMessage(String message, boolean prefix) {
        addChatMessage(message, Level.ERROR, prefix);
    }

    public static void addSuccessChatMessage(String message, boolean prefix) {
        addChatMessage(message, Level.SUCCESS, prefix);
    }

    public static void addChatMessage(String message, Level level, boolean prefix) {
        MutableComponent text = Component.literal(message).withStyle(level.getFormatting());
        if (prefix) {
            text = Component.literal(TAG).append(text);
        }
        MutableComponent finalText = text;
        McUtils.getPlayer().ifPresent(p -> p.displayClientMessage(finalText, false));
    }

    public static void addRawMessage(String message) {
        McUtils.getPlayer().ifPresent(player -> player.displayClientMessage(Component.literal(message), false));
    }


    public enum Level {
        ERROR(ChatFormatting.RED),
        INFO(ChatFormatting.WHITE),
        WARN(ChatFormatting.GOLD),
        SUCCESS(ChatFormatting.GREEN);

        private final ChatFormatting fmt;

        Level(ChatFormatting fmt) {
            this.fmt = fmt;
        }

        public ChatFormatting getFormatting() {
            return fmt;
        }
    }


}

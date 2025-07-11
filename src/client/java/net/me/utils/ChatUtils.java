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

package net.me.utils;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@SuppressWarnings("unused")
public final class ChatUtils {
    public final static String TAG = Formatting.GRAY + "[" + Formatting.GREEN + "MQS" + Formatting.GRAY + "] " + Formatting.RESET;

    private ChatUtils() {
    }

    public static void sendChatMessage(String s) {
        McUtils.getPlayer().ifPresent(player ->
                player.networkHandler.sendChatMessage(s)
        );
    }

    public static void sendChatCommand(String s) {
        McUtils.getPlayer().ifPresent(player ->
                player.networkHandler.sendChatCommand(s)
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
        Text text = Text.literal(message).formatted(level.getFormatting());
        if (prefix) {
            text = Text.literal(TAG).append(text);
        }
        Text finalText = text;
        McUtils.getPlayer().ifPresent(p -> p.sendMessage(finalText, false));
    }

    public static void addRawMessage(String message) {
        McUtils.getPlayer().ifPresent(player -> player.sendMessage(Text.literal(message), false));
    }


    public enum Level {
        ERROR(Formatting.RED),
        INFO(Formatting.WHITE),
        WARN(Formatting.GOLD),
        SUCCESS(Formatting.GREEN);
        private final Formatting fmt;

        Level(Formatting fmt) {
            this.fmt = fmt;
        }

        public Formatting getFormatting() {
            return fmt;
        }
    }


}

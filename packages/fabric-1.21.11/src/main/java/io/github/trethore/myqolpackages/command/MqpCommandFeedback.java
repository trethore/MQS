/*
 * My QOL Packages - Client-side Minecraft modding at runtime
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
package io.github.trethore.myqolpackages.command;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public final class MqpCommandFeedback {
    private MqpCommandFeedback() {}

    public static void sendInfo(FabricClientCommandSource source, String message) {
        sendInfo(source, Component.literal(message));
    }

    public static void sendInfo(FabricClientCommandSource source, Component message) {
        source.sendFeedback(format(message, ChatFormatting.YELLOW));
    }

    public static void sendError(FabricClientCommandSource source, String message) {
        sendError(source, Component.literal(message));
    }

    public static void sendError(FabricClientCommandSource source, Component message) {
        source.sendError(format(message, ChatFormatting.RED));
    }

    public static void sendWarning(FabricClientCommandSource source, Component message) {
        source.sendFeedback(format(message, ChatFormatting.GOLD));
    }

    public static void sendHeader(FabricClientCommandSource source) {
        MutableComponent header = Component.empty();
        header.append(Component.literal("----- ").withStyle(ChatFormatting.GRAY));
        header.append(createTag(ChatFormatting.YELLOW));
        header.append(Component.literal(" -----").withStyle(ChatFormatting.GRAY));
        source.sendFeedback(header);
    }

    public static void sendLine(FabricClientCommandSource source, String message) {
        sendLine(source, Component.literal(message));
    }

    public static void sendLine(FabricClientCommandSource source, Component message) {
        source.sendFeedback(message);
    }

    public static Component action(String label, String command, String hoverText) {
        return Component.literal("[" + label + "]")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent.RunCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverText))));
    }

    private static Component ensureTerminalPeriod(Component message) {
        if (message.getString().endsWith(".")) {
            return message;
        }
        return Component.empty().append(message).append(Component.literal("."));
    }

    private static Component format(Component message, ChatFormatting labelColor) {
        MutableComponent formattedMessage = Component.empty();
        formattedMessage.append(createTag(labelColor));
        formattedMessage.append(Component.literal(" "));
        formattedMessage.append(ensureTerminalPeriod(message));
        return formattedMessage;
    }

    private static Component createTag(ChatFormatting labelColor) {
        MutableComponent tag = Component.empty();
        tag.append(Component.literal("[").withStyle(ChatFormatting.GRAY));
        tag.append(Component.literal("MQP").withStyle(labelColor, ChatFormatting.BOLD));
        tag.append(Component.literal("]").withStyle(ChatFormatting.GRAY));
        return tag;
    }
}

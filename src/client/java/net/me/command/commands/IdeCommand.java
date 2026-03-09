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

package net.me.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.Main;
import net.me.command.Command;
import net.me.command.CommandManager;
import net.me.ui.UiManager;
import net.me.utils.ChatUtils;

import java.io.IOException;

public class IdeCommand extends Command {
    private final UiManager uiManager;

    public IdeCommand(UiManager uiManager) {
        this.uiManager = uiManager;
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("ide")
                .executes(this::executeOpenIde);
    }

    private int executeOpenIde(CommandContext<FabricClientCommandSource> ignored) {
        try {
            this.uiManager.openIde();
            ChatUtils.addSuccessChatMessage("Opened your IDE!", true);
            return CommandManager.COMMAND_SUCCESS;
        } catch (IllegalArgumentException | IOException exception) {
            Main.LOGGER.error("Failed to open configured IDE for MQS.", exception);
            ChatUtils.addErrorChatMessage("Failed to open IDE. Check logs for details.", true);
            return CommandManager.COMMAND_FAILURE;
        }
    }
}

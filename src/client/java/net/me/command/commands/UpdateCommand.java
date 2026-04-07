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
import net.fabricmc.loader.api.FabricLoader;
import net.me.command.Command;
import net.me.command.CommandManager;
import net.me.utils.ChatUtils;
import net.me.utils.update.UpdateUtils;
import net.me.utils.update.VersionUtils;

import java.util.concurrent.CompletableFuture;

public class UpdateCommand extends Command {
    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("update")
                .executes(this::executeUpdate);
    }

    private int executeUpdate(CommandContext<FabricClientCommandSource> context) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            ChatUtils.addErrorChatMessage("Cannot update in a development environment.", true);
            return CommandManager.COMMAND_FAILURE;
        }

        ChatUtils.addInfoChatMessage("Checking for updates...", true);

        UpdateUtils.checkForUpdateAsync(result -> context.getSource().getClient().execute(() -> {
            switch (result.status()) {
                case UPDATE_AVAILABLE -> {
                    ChatUtils.addSuccessChatMessage("New version found: " + result.updateInfo().version(), true);
                    ChatUtils.addInfoChatMessage("Current version: " + VersionUtils.getCurrentVersion(), false);
                    ChatUtils.addInfoChatMessage("Changelog:\n" + result.updateInfo().changelog(), false);
                    ChatUtils.addInfoChatMessage("Downloading update...", true);

                    CompletableFuture.runAsync(() -> {
                        UpdateUtils.UpdateResult downloadResult = UpdateUtils.downloadAndPrepareUpdate(result.updateInfo());
                        context.getSource().getClient().execute(() -> ChatUtils.addInfoChatMessage(downloadResult.getMessage(), true));
                    });
                }
                case UP_TO_DATE ->
                        ChatUtils.addSuccessChatMessage("You're already running the latest version (" + VersionUtils.getCurrentVersion() + ")!", true);
                case ERROR -> ChatUtils.addErrorChatMessage("Failed to check for updates: " + result.message(), true);
            }
        }));

        return CommandManager.COMMAND_SUCCESS;
    }
}

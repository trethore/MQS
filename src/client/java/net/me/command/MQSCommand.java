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

package net.me.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.Main;
import net.me.command.commands.*;
import net.me.scripting.ScriptingService;
import net.me.scripting.typings.TypeDefinitionGenerator;
import net.me.ui.UiManager;
import net.me.utils.ChatUtils;

public class MQSCommand extends Command {
    private final ScriptCommand scriptCommand;
    private final GenerateCommand generateCommand;
    private final IdeCommand ideCommand;
    private final TemplateCommand templateCommand;
    private final UpdateCommand updateCommand;
    private final VscodeCommand vscodeCommand;
    private final UiManager uiManager;

    public MQSCommand(
            ScriptingService scriptingService,
            UiManager uiManager,
            TypeDefinitionGenerator typeDefinitionGenerator
    ) {
        this.scriptCommand = new ScriptCommand(scriptingService);
        this.generateCommand = new GenerateCommand(typeDefinitionGenerator);
        this.ideCommand = new IdeCommand(uiManager);
        this.templateCommand = new TemplateCommand();
        this.updateCommand = new UpdateCommand();
        this.vscodeCommand = new VscodeCommand(uiManager);
        this.uiManager = uiManager;
    }

    @Override
    protected LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("mqs")
                .executes(this::openUi)
                .then(ClientCommandManager.literal("ui")
                        .executes(this::openUi))
                .then(scriptCommand.buildCommand())
                .then(generateCommand.buildCommand())
                .then(templateCommand.buildCommand())
                .then(ideCommand.buildCommand())
                .then(vscodeCommand.buildCommand())
                .then(updateCommand.buildCommand());
    }

    private int openUi(CommandContext<FabricClientCommandSource> ignored) {
        try {
            uiManager.openUi();
            return CommandManager.COMMAND_SUCCESS;
        } catch (RuntimeException exception) {
            Main.LOGGER.atError().setCause(exception).log("Failed to open MQS UI.");
            ChatUtils.addErrorChatMessage("Failed to open MQS UI. Check logs for details.", true);
            return CommandManager.COMMAND_FAILURE;
        }
    }
}

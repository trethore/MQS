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

package net.me.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.command.commands.ScreenCommand;
import net.me.command.commands.ScriptCommand;
import net.me.command.commands.UpdateCommand;
import net.me.config.GlobalConfigManager;
import net.me.console.ConsoleManager;
import net.me.keybinds.KeybindManager;
import net.me.screen.screens.AllScriptsScreen;
import net.me.scripting.ScriptingService;

public class MQSCommand extends Command {
    private final ScriptingService scriptingService;
    private final ConsoleManager consoleManager;
    private final ScriptCommand scriptCommand;
    private final ScreenCommand screenCommand;
    private final UpdateCommand updateCommand;
    private final GlobalConfigManager globalConfigManager;


    public MQSCommand(ScriptingService scriptingService, ConsoleManager consoleManager, GlobalConfigManager globalConfigManager, KeybindManager keybindManager) {
        this.scriptingService = scriptingService;
        this.consoleManager = consoleManager;
        this.globalConfigManager = globalConfigManager;
        this.scriptCommand = new ScriptCommand(scriptingService);
        this.screenCommand = new ScreenCommand(scriptingService, consoleManager, globalConfigManager, keybindManager);
        this.updateCommand = new UpdateCommand();
    }

    @Override
    protected LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("mqs")
                .requires(source -> source.hasPermissionLevel(0))
                .executes(this::openMenu)
                .then(scriptCommand.buildCommand())
                .then(screenCommand.buildCommand())
                .then(updateCommand.buildCommand());
    }

    private int openMenu(CommandContext<FabricClientCommandSource> context) {
        new AllScriptsScreen(scriptingService, consoleManager, globalConfigManager).open();
        return CommandManager.COMMAND_SUCCESS;
    }
}
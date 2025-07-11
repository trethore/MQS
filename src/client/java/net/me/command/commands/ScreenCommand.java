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

package net.me.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.command.Command;
import net.me.command.CommandManager;
import net.me.config.GlobalConfigManager;
import net.me.console.ConsoleManager;
import net.me.keybinds.KeybindManager;
import net.me.screen.screens.*;
import net.me.scripting.ScriptingService;

public class ScreenCommand extends Command {
    private final ScriptingService scriptingService;
    private final ConsoleManager consoleManager;
    private final GlobalConfigManager globalConfigManager;
    private final KeybindManager keybindManager;

    public ScreenCommand(ScriptingService scriptingService, ConsoleManager consoleManager, GlobalConfigManager globalConfigManager, KeybindManager keybindManager) {
        this.scriptingService = scriptingService;
        this.consoleManager = consoleManager;
        this.globalConfigManager = globalConfigManager;
        this.keybindManager = keybindManager;
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("screen")
                .executes(this::openAllScriptsScreen)
                .then(ClientCommandManager.literal("allscripts")
                        .executes(this::openAllScriptsScreen))
                .then(ClientCommandManager.literal("moreoptions")
                        .executes(this::openMoreOptionsScreen))
                .then(ClientCommandManager.literal("keybinds")
                        .executes(this::openKeybindsScreen))
                .then(ClientCommandManager.literal("settings")
                        .executes(this::openSettingsScreen))
                .then(ClientCommandManager.literal("console")
                        .executes(this::openConsoleScreen));
    }

    private int openAllScriptsScreen(CommandContext<FabricClientCommandSource> context) {
        new AllScriptsScreen(scriptingService, consoleManager, globalConfigManager).open();
        return CommandManager.COMMAND_SUCCESS;
    }

    private int openMoreOptionsScreen(CommandContext<FabricClientCommandSource> context) {
        new MoreOptionsScreen(null).open();
        return CommandManager.COMMAND_SUCCESS;
    }

    private int openKeybindsScreen(CommandContext<FabricClientCommandSource> context) {
        new KeybindsScreen(null, keybindManager).open();
        return CommandManager.COMMAND_SUCCESS;
    }

    private int openSettingsScreen(CommandContext<FabricClientCommandSource> context) {
        new SettingsScreen(null, globalConfigManager).open();
        return CommandManager.COMMAND_SUCCESS;
    }

    private int openConsoleScreen(CommandContext<FabricClientCommandSource> context) {
        new ConsoleScreen(null, consoleManager).open();
        return CommandManager.COMMAND_SUCCESS;
    }
}
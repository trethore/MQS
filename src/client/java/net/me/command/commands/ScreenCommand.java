package net.me.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.command.Command;
import net.me.command.CommandManager;
import net.me.console.ConsoleManager;
import net.me.screen.screens.*;
import net.me.scripting.ScriptingService;

public class ScreenCommand extends Command {
    private final ScriptingService scriptingService;
    private final ConsoleManager consoleManager;

    public ScreenCommand(ScriptingService scriptingService, ConsoleManager consoleManager) {
        this.scriptingService = scriptingService;
        this.consoleManager = consoleManager;
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
        new AllScriptsScreen(scriptingService, consoleManager).open();
        return CommandManager.COMMAND_SUCCESS;
    }

    private int openMoreOptionsScreen(CommandContext<FabricClientCommandSource> context) {
        new MoreOptionsScreen(null).open();
        return CommandManager.COMMAND_SUCCESS;
    }

    private int openKeybindsScreen(CommandContext<FabricClientCommandSource> context) {
        new KeybindsScreen(null).open();
        return CommandManager.COMMAND_SUCCESS;
    }

    private int openSettingsScreen(CommandContext<FabricClientCommandSource> context) {
        new SettingsScreen(null).open();
        return CommandManager.COMMAND_SUCCESS;
    }

    private int openConsoleScreen(CommandContext<FabricClientCommandSource> context) {
        new ConsoleScreen(null, consoleManager).open();
        return CommandManager.COMMAND_SUCCESS;
    }
}
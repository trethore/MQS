package net.me.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.command.commands.ScreenCommand;
import net.me.command.commands.ScriptCommand;
import net.me.console.ConsoleManager;
import net.me.screen.screens.AllScriptsScreen;
import net.me.scripting.ScriptingService;

public class MQSCommand extends Command {
    private final ScriptingService scriptingService;
    private final ConsoleManager consoleManager;

    public MQSCommand(ScriptingService scriptingService, ConsoleManager consoleManager) {
        this.scriptingService = scriptingService;
        this.consoleManager = consoleManager;
    }

    @Override
    protected LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("mqs")
                .requires(source -> source.hasPermissionLevel(0))
                .executes(this::openMenu)
                .then(new ScriptCommand(scriptingService).buildCommand())
                .then(new ScreenCommand(scriptingService, consoleManager).buildCommand());
    }

    private int openMenu(CommandContext<FabricClientCommandSource> context) {
        new AllScriptsScreen(scriptingService, consoleManager).open();
        return CommandManager.COMMAND_SUCCESS;
    }
}
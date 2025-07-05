package net.me.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.command.commands.ScreenCommand;
import net.me.command.commands.ScriptCommand;
import net.me.config.GlobalConfigManager;
import net.me.console.ConsoleManager;
import net.me.keybinds.KeybindManager;
import net.me.screen.screens.AllScriptsScreen;
import net.me.scripting.ScriptingService;

public class MQSCommand extends Command {
    private final ScriptingService scriptingService;
    private final ConsoleManager consoleManager;
    private final GlobalConfigManager globalConfigManager;
    private final KeybindManager keybindManager;

    public MQSCommand(ScriptingService scriptingService, ConsoleManager consoleManager, GlobalConfigManager globalConfigManager, KeybindManager keybindManager) {
        this.scriptingService = scriptingService;
        this.consoleManager = consoleManager;
        this.globalConfigManager = globalConfigManager;
        this.keybindManager = keybindManager;
    }

    @Override
    protected LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("mqs")
                .requires(source -> source.hasPermissionLevel(0))
                .executes(this::openMenu)
                .then(new ScriptCommand(scriptingService).buildCommand())
                .then(new ScreenCommand(scriptingService, consoleManager, globalConfigManager, keybindManager).buildCommand());
    }

    private int openMenu(CommandContext<FabricClientCommandSource> context) {
        new AllScriptsScreen(scriptingService, consoleManager).open();
        return CommandManager.COMMAND_SUCCESS;
    }
}
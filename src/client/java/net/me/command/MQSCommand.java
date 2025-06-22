package net.me.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.command.commands.ScriptCommand;
import net.me.screen.screens.AllScriptsScreen;

public class MQSCommand extends Command {

    @Override
    protected LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("mqs")
                .requires(source -> source.hasPermissionLevel(0))
                .executes(this::openMenu)
                .then(new ScriptCommand().buildCommand());
    }

    private int openMenu(CommandContext<FabricClientCommandSource> context) {
        new AllScriptsScreen().open();
        return CommandManager.COMMAND_SUCCESS;
    }
}
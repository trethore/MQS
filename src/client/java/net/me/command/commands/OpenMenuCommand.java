package net.me.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.command.Command;
import net.me.command.CommandManager;
import net.me.screen.screens.AllScriptsScreen;

public class OpenMenuCommand extends Command {
    @Override
    protected LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("mqs")
                .executes(this::openMenu);
    }

    private int openMenu(CommandContext<FabricClientCommandSource> fabricClientCommandSourceCommandContext) {
        new AllScriptsScreen().open();
        return CommandManager.COMMAND_SUCCESS;
    }

}

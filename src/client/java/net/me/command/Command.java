package net.me.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public abstract class Command {
    protected abstract LiteralArgumentBuilder<FabricClientCommandSource> buildCommand();

    protected void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(buildCommand());
    }
}

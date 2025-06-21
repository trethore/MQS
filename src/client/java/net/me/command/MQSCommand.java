package net.me.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public abstract class MQSCommand extends Command {
    @Override
    public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("mqs")
                        .requires(source -> source.hasPermissionLevel(0))
                        .then(buildCommand())
        );
    }
}

package net.me.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.command.commands.OpenMenuCommand;
import net.me.command.commands.ScriptCommand;

import java.util.ArrayList;

public class CommandManager {
    public static final int COMMAND_SUCCESS = 1;
    public static final int COMMAND_FAILURE = -1;

    private final static CommandManager INSTANCE = new CommandManager();
    private final ArrayList<Command> commands = new ArrayList<>();

    private CommandManager() {
    }

    public static CommandManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        registerCommands();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerClientCommands(dispatcher));
    }

    private void registerClientCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        commands.add(new OpenMenuCommand());
        commands.add(new ScriptCommand());
        registerCommandsInDispatcher(dispatcher);
    }

    private void registerCommandsInDispatcher(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        commands.forEach(command -> command.register(dispatcher));
    }
}

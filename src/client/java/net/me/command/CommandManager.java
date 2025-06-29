package net.me.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.console.ConsoleManager;
import net.me.scripting.ScriptingService;

import java.util.ArrayList;

public class CommandManager {

    public static final int COMMAND_SUCCESS = 1;
    public static final int COMMAND_FAILURE = -1;

    private final ArrayList<Command> commands = new ArrayList<>();

    private ScriptingService scriptingService;
    private ConsoleManager consoleManager;


    public void init(ScriptingService scriptingService, ConsoleManager consoleManager) {
        this.scriptingService = scriptingService;
        this.consoleManager = consoleManager;
        registerCommands();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerClientCommands(dispatcher));
    }

    private void registerClientCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        MQSCommand mqsCommand = new MQSCommand(scriptingService, consoleManager);
        commands.add(mqsCommand);
        registerCommandsInDispatcher(dispatcher);
    }

    private void registerCommandsInDispatcher(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        commands.forEach(command -> command.register(dispatcher));
    }
}

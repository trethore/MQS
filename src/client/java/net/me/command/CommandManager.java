package net.me.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.config.GlobalConfigManager;
import net.me.console.ConsoleManager;
import net.me.keybinds.KeybindManager;
import net.me.scripting.ScriptingService;

import java.util.ArrayList;

public class CommandManager {

    public static final int COMMAND_SUCCESS = 1;
    public static final int COMMAND_FAILURE = -1;

    private final ArrayList<Command> commands = new ArrayList<>();

    private ScriptingService scriptingService;
    private ConsoleManager consoleManager;
    private GlobalConfigManager globalConfigManager;
    private KeybindManager keybindManager;


    public void init(ScriptingService scriptingService, ConsoleManager consoleManager, GlobalConfigManager globalConfigManager, KeybindManager keybindManager) {
        this.scriptingService = scriptingService;
        this.consoleManager = consoleManager;
        this.globalConfigManager = globalConfigManager;
        this.keybindManager = keybindManager;
        registerCommands();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerClientCommands(dispatcher));
    }

    private void registerClientCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        MQSCommand mqsCommand = new MQSCommand(scriptingService, consoleManager, globalConfigManager, keybindManager);
        commands.add(mqsCommand);
        registerCommandsInDispatcher(dispatcher);
    }

    private void registerCommandsInDispatcher(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        commands.forEach(command -> command.register(dispatcher));
    }
}

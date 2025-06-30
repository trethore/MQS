package net.me;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.me.command.CommandManager;
import net.me.console.ConsoleManager;
import net.me.event.EventManager;
import net.me.hooking.HookManager;
import net.me.scripting.ConfigManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.ScriptingService;
import net.me.scripting.mappings.MappingsManager;
import org.graalvm.polyglot.Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Main implements ClientModInitializer {
    public static final String MOD_ID = "my-qol-scripts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String MC_VERSION = "1.21.4";
    public static final Path MOD_DIR = FabricLoader.getInstance().getGameDir().resolve(MOD_ID);

    private static ConfigManager configManager;
    private static MappingsManager mappingsManager;

    @Override
    public void onInitializeClient() {
        Main.mappingsManager = new MappingsManager();
        Main.configManager = new ConfigManager();
        ScriptManager scriptManager = new ScriptManager();
        HookManager hookManager = new HookManager();
        EventManager eventManager = new EventManager();
        CommandManager commandManager = new CommandManager();
        ConsoleManager consoleManager = new ConsoleManager();
        ScriptingService scriptingService = new ScriptingService();

        Engine scriptEngine = Engine.create();

        mappingsManager.init();
        configManager.init(scriptEngine);
        eventManager.init(scriptManager);
        consoleManager.init(scriptingService);
        scriptManager.init(scriptEngine, mappingsManager, configManager, eventManager, hookManager);
        scriptingService.init(scriptManager, configManager);
        hookManager.init(scriptManager, mappingsManager);
        commandManager.init(scriptingService, consoleManager);

        LOGGER.info("Hello from MyQOLScripts!");
    }


    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public static MappingsManager getMappingsManager() {
        return mappingsManager;
    }
}
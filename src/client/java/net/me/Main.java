package net.me;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.me.command.CommandManager;
import net.me.config.GlobalConfigManager;
import net.me.console.ConsoleManager;
import net.me.event.EventManager;
import net.me.hooking.HookManager;
import net.me.keybinds.KeybindManager;
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
    private static ScriptManager scriptManager;
    private static HookManager hookManager;
    private static EventManager eventManager;
    private static CommandManager commandManager;
    private static ConsoleManager consoleManager;
    private static ScriptingService scriptingService;
    private static GlobalConfigManager globalConfigManager;
    private static KeybindManager keybindManager;
    private static Engine scriptEngine;

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public static MappingsManager getMappingsManager() {
        return mappingsManager;
    }

    public static EventManager getEventManager() {
        return eventManager;
    }

    public static GlobalConfigManager getGlobalConfigManager() {
        return globalConfigManager;
    }

    public static KeybindManager getKeybindManager() {
        return keybindManager;
    }

    @Override
    public void onInitializeClient() {
        Main.mappingsManager = new MappingsManager();
        Main.configManager = new ConfigManager();
        Main.scriptManager = new ScriptManager();
        Main.hookManager = new HookManager();
        Main.eventManager = new EventManager();
        Main.commandManager = new CommandManager();
        Main.consoleManager = new ConsoleManager();
        Main.scriptingService = new ScriptingService();
        Main.globalConfigManager = new GlobalConfigManager();
        Main.keybindManager = new KeybindManager();

        Main.scriptEngine = Engine.create();

        mappingsManager.init();
        configManager.init(scriptEngine);

        scriptManager.init(scriptEngine, mappingsManager, configManager, eventManager, hookManager, keybindManager);

        eventManager.init(scriptManager);
        hookManager.init(scriptManager, mappingsManager);
        keybindManager.init(scriptManager, configManager);

        scriptingService.init(scriptManager, configManager);

        consoleManager.init(scriptingService, globalConfigManager);
        commandManager.init(scriptingService, consoleManager);
        globalConfigManager.init(consoleManager);

        // and finally, enable all scripts !
        scriptManager.loadAndEnableScriptsFromConfig();
        LOGGER.info("Hello from MyQOLScripts!");
    }
}
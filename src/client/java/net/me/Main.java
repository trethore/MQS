/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.me;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.me.command.CommandManager;
import net.me.command.MQSCommand;
import net.me.config.GlobalConfigManager;
import net.me.console.ConsoleManager;
import net.me.console.commands.*;
import net.me.event.EventManager;
import net.me.event.MQSEventBus;
import net.me.hooking.HookManager;
import net.me.keybinds.KeybindManager;
import net.me.scripting.ConfigManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.ScriptingService;
import net.me.scripting.mappings.MappingsManager;
import net.me.utils.McUtils;
import org.graalvm.polyglot.Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.weave.WeaveCore;

import java.nio.file.Path;

public class Main implements ClientModInitializer {
    public static final String MOD_ID = "my-qol-scripts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String MC_VERSION = "1.21.4";
    public static final Path MOD_DIR = FabricLoader.getInstance().getGameDir().resolve(MOD_ID);

    private static Main instance;

    private ConfigManager configManager;
    private MappingsManager mappingsManager;
    private ScriptManager scriptManager;
    private HookManager hookManager;
    private EventManager eventManager;
    private CommandManager commandManager;
    private ConsoleManager consoleManager;
    private ScriptingService scriptingService;
    private GlobalConfigManager globalConfigManager;
    private KeybindManager keybindManager;
    private Engine scriptEngine;

    public static Main getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MappingsManager getMappingsManager() {
        return mappingsManager;
    }

    public GlobalConfigManager getGlobalConfigManager() {
        return globalConfigManager;
    }

    public KeybindManager getKeybindManager() {
        return keybindManager;
    }

    @Override
    public void onInitializeClient() {
        WeaveCore.init();
        instance = this;

        this.scriptEngine = Engine.create();
        this.mappingsManager = new MappingsManager();
        this.configManager = new ConfigManager();
        this.scriptManager = new ScriptManager();

        this.commandManager = new CommandManager();
        this.consoleManager = new ConsoleManager();
        this.globalConfigManager = new GlobalConfigManager(consoleManager);

        this.eventManager = new EventManager(scriptManager);
        MQSEventBus.setManager(eventManager);
        this.keybindManager = new KeybindManager(scriptManager, configManager);

        this.hookManager = new HookManager(scriptManager, mappingsManager);
        this.scriptingService = new ScriptingService(scriptManager, configManager);

        configManager.init();
        consoleManager.init();
        commandManager.init();

        this.registerConsoleCommands();
        this.registerClientCommands();

        mappingsManager.init();

        mappingsManager.whenReady(() -> McUtils.getMc().ifPresent(mc -> mc.send(() -> {
            scriptManager.init(scriptEngine, mappingsManager, configManager, eventManager, hookManager, keybindManager);
            globalConfigManager.init();
            scriptManager.loadAndEnableScriptsFromConfig();

            LOGGER.info("MyQOLScripts initialization complete! Hello !");
        })));
    }

    private void registerClientCommands() {
        this.commandManager.addCommand(new MQSCommand(this.scriptingService));
    }

    private void registerConsoleCommands() {
        this.consoleManager.addCommand(new HelpCommand(this.consoleManager));
        this.consoleManager.addCommand(new ClearCommand(this.consoleManager));
        this.consoleManager.addCommand(new ScriptCommands.ListScriptsCommand(this.consoleManager, this.scriptingService));
        this.consoleManager.addCommand(new ScriptCommands.EnableScriptCommand(this.consoleManager, this.scriptingService));
        this.consoleManager.addCommand(new ScriptCommands.DisableScriptCommand(this.consoleManager, this.scriptingService));
        this.consoleManager.addCommand(new ScriptCommands.RefreshScriptsCommand(this.consoleManager, this.scriptingService));
        this.consoleManager.addCommand(new ScriptCommands.RefreshAndReenableCommand(this.consoleManager, this.scriptingService));
        this.consoleManager.addCommand(new ScriptCommands.DisableAllCommand(this.consoleManager, this.scriptingService));
        this.consoleManager.addCommand(new LogRedirectCommand(this.consoleManager, this.globalConfigManager));
        this.consoleManager.addCommand(new CopyTailCommand(this.consoleManager));
        this.consoleManager.addCommand(new SaveConfigCommand(this.consoleManager, this.scriptingService));
        this.consoleManager.addCommand(new SaveAllConfigsCommand(this.consoleManager, this.scriptingService));
        this.consoleManager.addCommand(new AllowAllClassesCommand(this.consoleManager, this.globalConfigManager));
    }
}

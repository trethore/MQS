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

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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
import net.me.ui.ScriptView;
import net.me.utils.McUtils;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.graalvm.polyglot.Engine;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.minegui.MineGuiCore;
import tytoo.minegui.MineGuiInitializationOptions;
import tytoo.minegui.manager.UIManager;
import tytoo.minegui.view.cursor.CursorPolicies;

import java.nio.file.Path;

public class Main implements ClientModInitializer {
    public static final String MOD_ID = "my_qol_scripts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String MC_VERSION = "1.21.4";
    public static final Path MOD_DIR = FabricLoader.getInstance().getGameDir().resolve(MOD_ID);
    private static final String KEY_CATEGORY = "key.categories.my_qol_scripts";
    private static final String KEY_TOGGLE_SCRIPT_VIEW = "key.my-qol-scripts.toggle_ui";

    @Getter
    private static Main instance;

    @Getter
    private ConfigManager configManager;
    @Getter
    private MappingsManager mappingsManager;
    @Getter
    private ScriptManager scriptManager;
    private HookManager hookManager;
    private EventManager eventManager;
    private CommandManager commandManager;
    private ConsoleManager consoleManager;
    private ScriptingService scriptingService;
    @Getter
    private GlobalConfigManager globalConfigManager;
    @Getter
    private ScriptView scriptView;
    @Getter
    private KeybindManager keybindManager;
    private Engine scriptEngine;
    private KeyBinding scriptViewKeyBinding;

    @Override
    public void onInitializeClient() {
        initMineGui();
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
        this.initKeybindings();

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

    private void initMineGui() {
        MineGuiInitializationOptions options = MineGuiInitializationOptions.builder(Main.MOD_ID)
                .defaultCursorPolicyId(CursorPolicies.clickToLockId())
                .build();
        MineGuiCore.init(options);

        this.scriptView = new ScriptView();
        UIManager.get(Main.MOD_ID).register(this.scriptView);
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

    private void initKeybindings() {
        this.scriptViewKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_TOGGLE_SCRIPT_VIEW,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                KEY_CATEGORY
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (this.scriptViewKeyBinding.wasPressed()) {
                this.scriptView.toggleVisibility();
            }
        });
    }
}

package net.me.scripting.engine;

import net.me.event.EventManager;
import net.me.hooking.HookManager;
import net.me.keybinds.KeybindManager;
import net.me.scripting.ConfigManager;
import net.me.scripting.commands.CommandAPIService;
import net.me.scripting.module.RunningScript;

public class ScriptLifecycleManager {

    private final ConfigManager configManager;
    private final EventManager eventManager;
    private final HookManager hookManager;
    private final KeybindManager keybindManager;
    private final CommandAPIService commandApiService;
    private final ScriptContextManager contextManager;

    public ScriptLifecycleManager(ConfigManager configManager, EventManager eventManager, HookManager hookManager, KeybindManager keybindManager, CommandAPIService commandApiService, ScriptContextManager contextManager) {
        this.configManager = configManager;
        this.eventManager = eventManager;
        this.hookManager = hookManager;
        this.keybindManager = keybindManager;
        this.commandApiService = commandApiService;
        this.contextManager = contextManager;
    }

    public void enable(RunningScript script) {
        script.onEnable();
        configManager.setEnabledState(script.getId(), true);
        configManager.saveConfig(script);
    }

    public void disable(RunningScript script) {
        script.onDisable();

        eventManager.unregisterAll(script);
        commandApiService.unregisterAllFor(script);
        hookManager.unhookAllForScript(script);
        keybindManager.unregister(script);

        configManager.setEnabledState(script.getId(), false);
        configManager.saveConfig(script);
        configManager.unloadConfig(script);

        contextManager.returnContext(script.getContext());
        script.invalidate();
    }
}
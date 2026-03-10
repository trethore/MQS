/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Réthoré
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

package net.me.scripting.engine;

import net.me.event.EventManager;
import net.me.hooking.HookManager;
import net.me.keybinds.KeybindManager;
import net.me.scripting.ConfigManager;
import net.me.scripting.commands.CommandAPIService;
import net.me.scripting.script.RunningScript;
import net.me.utils.ScriptScheduler;

public class ScriptLifecycleManager {

    private final ConfigManager configManager;
    private final EventManager eventManager;
    private final HookManager hookManager;
    private final KeybindManager keybindManager;
    private final CommandAPIService commandApiService;
    private final ScriptScheduler scheduler;
    private final ScriptContextManager contextManager;

    public ScriptLifecycleManager(ConfigManager configManager, EventManager eventManager, HookManager hookManager, KeybindManager keybindManager, CommandAPIService commandApiService, ScriptScheduler scheduler, ScriptContextManager contextManager) {
        this.configManager = configManager;
        this.eventManager = eventManager;
        this.hookManager = hookManager;
        this.keybindManager = keybindManager;
        this.commandApiService = commandApiService;
        this.scheduler = scheduler;
        this.contextManager = contextManager;
    }

    public void enable(RunningScript script) {
        script.onEnable();
        configManager.setEnabledState(script.getId(), true);
        configManager.saveConfig(script);
    }

    public void disable(RunningScript script) {
        script.onDisable();

        scheduler.cancelAllFor(script);
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

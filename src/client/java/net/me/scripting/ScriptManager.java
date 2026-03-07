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

package net.me.scripting;

import lombok.Getter;
import net.me.Main;
import net.me.config.GlobalConfigManager;
import net.me.event.EventManager;
import net.me.hooking.HookManager;
import net.me.keybinds.KeybindManager;
import net.me.scripting.commands.CommandAPIService;
import net.me.scripting.engine.*;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.module.ScriptDescriptor;
import net.me.utils.ScriptScheduler;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

import java.util.*;

public class ScriptManager {

    private final Map<String, ScriptDescriptor> availableScripts = new HashMap<>();
    private final Map<String, RunningScript> runningScripts = new HashMap<>();
    private final ThreadLocal<RunningScript> currentScriptContext = new InheritableThreadLocal<>();

    private ConfigManager configManager;
    @Getter
    private ScriptingClassResolver classResolver;
    private ScriptDiscoverer scriptDiscoverer;
    private ScriptContextManager contextManager;
    private ScriptLifecycleManager lifecycleManager;
    @Getter
    private CommandAPIService commandApiService;

    public ScriptManager() {
        // 2-step initialization
    }

    public void init(Engine scriptEngine, MappingsManager mappingsManager, ConfigManager configManager, EventManager eventManager, HookManager hookManager, KeybindManager keybindManager, GlobalConfigManager globalConfigManager) {
        this.configManager = configManager;

        this.classResolver = ScriptingClassResolver.create(mappingsManager, this);

        this.commandApiService = new CommandAPIService();
        this.commandApiService.init();

        ScriptScheduler scheduler = new ScriptScheduler(this);

        ScriptContextFactory contextFactory = new ScriptContextFactory(classResolver, scriptEngine, this, eventManager, configManager, this.commandApiService, hookManager, keybindManager, scheduler, globalConfigManager);
        this.contextManager = ScriptContextManager.create(contextFactory);
        this.lifecycleManager = new ScriptLifecycleManager(configManager, eventManager, hookManager, keybindManager, this.commandApiService, scheduler, contextManager);

        this.scriptDiscoverer = new ScriptDiscoverer(globalConfigManager);

        discoverScripts();
    }

    public void loadAndEnableScriptsFromConfig() {
        Main.LOGGER.info("Checking configs to auto-enable scripts...");
        for (ScriptDescriptor descriptor : availableScripts.values()) {
            if (configManager.getEnabledState(descriptor.getId())) {
                Main.LOGGER.info("Auto-enabling script '{}' as per config.", descriptor.moduleName());
                enableScript(descriptor.getId());
            }
        }
    }

    public void enableScript(String scriptId) {
        if (isRunning(scriptId)) {
            Main.LOGGER.warn("Script '{}' is already running.", scriptId);
            return;
        }

        ScriptDescriptor descriptor = availableScripts.get(scriptId);
        if (descriptor == null) {
            Main.LOGGER.error("Cannot enable unknown script '{}'", scriptId);
            return;
        }

        Context scriptContext = null;
        try {
            scriptContext = contextManager.getContext();
            Map<String, Value> fileExports = ScriptLoader.loadModules(descriptor.path(), scriptContext, contextManager.getPerFileExports());

            String mainClassName = descriptor.mainClass();
            Value scriptClass = fileExports.get(mainClassName);

            if (scriptClass == null || !scriptClass.canInstantiate()) {
                throw new IllegalStateException("Could not find an instantiable, exported class '" + mainClassName + "' in " + descriptor.path().getFileName() + ".");
            }

            Value jsInstance = scriptClass.newInstance();
            RunningScript runningScript = new RunningScript(descriptor, jsInstance, scriptContext);

            setCurrentScript(runningScript);
            lifecycleManager.enable(runningScript);
            runningScripts.put(scriptId, runningScript);

        } catch (Exception e) {
            Main.LOGGER.error("Failed to enable script '{}'. It may be in a broken state. Please disable it to ensure cleanup.", scriptId, e);
            if (scriptContext != null) {
                contextManager.returnContext(scriptContext);
            }
        } finally {
            clearCurrentScript();
        }
    }

    public void disableScript(String scriptId) {
        RunningScript script = runningScripts.remove(scriptId);
        if (script != null) {
            setCurrentScript(script);
            try {
                lifecycleManager.disable(script);
            } finally {
                clearCurrentScript();
            }
        }
    }

    public void refreshAndReenable() {
        Set<String> previouslyRunningIds = new HashSet<>(runningScripts.keySet());

        new ArrayList<>(previouslyRunningIds).forEach(this::disableScript);

        discoverScripts();

        previouslyRunningIds.forEach(scriptId -> {
            if (availableScripts.containsKey(scriptId)) {
                enableScript(scriptId);
            } else {
                Main.LOGGER.warn("Script '{}' was running, but is no longer available after refresh.", scriptId);
            }
        });
    }

    public void refresh() {
        new ArrayList<>(runningScripts.keySet()).forEach(this::disableScript);
        discoverScripts();
    }

    private void discoverScripts() {
        this.availableScripts.clear();
        this.availableScripts.putAll(scriptDiscoverer.discoverScripts());
    }

    public boolean isRunning(String scriptId) {
        return runningScripts.containsKey(scriptId);
    }

    public Collection<ScriptDescriptor> getAvailableScripts() {
        return Collections.unmodifiableCollection(availableScripts.values());
    }

    public Collection<RunningScript> getRunningScripts() {
        return Collections.unmodifiableCollection(runningScripts.values());
    }

    public RunningScript getCurrentScript() {
        return currentScriptContext.get();
    }

    public void setCurrentScript(RunningScript script) {
        currentScriptContext.set(script);
    }

    public void clearCurrentScript() {
        currentScriptContext.remove();
    }

}

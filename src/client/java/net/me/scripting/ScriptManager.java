package net.me.scripting;

import net.me.Main;
import net.me.event.EventManager;
import net.me.hooking.HookManager;
import net.me.keybinds.KeybindManager;
import net.me.scripting.commands.CommandAPIService;
import net.me.scripting.engine.*;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.module.ScriptDescriptor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

import java.util.*;

public class ScriptManager {

    private final Map<String, ScriptDescriptor> availableScripts = new HashMap<>();
    private final Map<String, RunningScript> runningScripts = new HashMap<>();
    private final ThreadLocal<Map<String, Value>> perFileExports = new ThreadLocal<>();
    private final ThreadLocal<RunningScript> currentScriptContext = new InheritableThreadLocal<>();

    private ConfigManager configManager;
    private ScriptingClassResolver classResolver;
    private ScriptDiscoverer scriptDiscoverer;
    private ScriptLoader scriptLoader;
    private ScriptContextManager contextManager;
    private ScriptLifecycleManager lifecycleManager;

    public ScriptManager() {
    }

    public void init(Engine scriptEngine, MappingsManager mappingsManager, ConfigManager configManager, EventManager eventManager, HookManager hookManager, KeybindManager keybindManager) {
        this.configManager = configManager;

        this.classResolver = new ScriptingClassResolver();
        classResolver.init(mappingsManager, this);

        CommandAPIService commandApiService = new CommandAPIService();
        commandApiService.init();

        ScriptContextFactory contextFactory = new ScriptContextFactory(classResolver, scriptEngine, this, eventManager, configManager, commandApiService, hookManager, keybindManager);
        this.contextManager = new ScriptContextManager(contextFactory, perFileExports);
        this.lifecycleManager = new ScriptLifecycleManager(configManager, eventManager, hookManager, keybindManager, commandApiService, contextManager);

        this.scriptDiscoverer = new ScriptDiscoverer();
        this.scriptLoader = new ScriptLoader();

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
            Map<String, Value> fileExports = scriptLoader.loadModules(descriptor.path(), scriptContext, perFileExports);

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

    public ScriptingClassResolver getClassResolver() {
        return classResolver;
    }
}
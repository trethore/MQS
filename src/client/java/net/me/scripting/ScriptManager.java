package net.me.scripting;

import net.me.Main;
import net.me.event.EventManager;
import net.me.hooking.HookManager;
import net.me.scripting.commands.CommandAPIService;
import net.me.scripting.engine.ScriptContextFactory;
import net.me.scripting.engine.ScriptLoader;
import net.me.scripting.engine.ScriptingClassResolver;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.module.ScriptDescriptor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ScriptManager {
    private final Map<String, ScriptDescriptor> availableScripts = new HashMap<>();
    private final Map<String, RunningScript> runningScripts = new HashMap<>();

    private ScriptContextFactory contextFactory;
    private ScriptLoader scriptLoader;

    private EventManager eventManager;
    private ConfigManager configManager;
    private HookManager hookManager;
    private final CommandAPIService commandApiService;

    private final ThreadLocal<Map<String, Value>> perFileExports = new ThreadLocal<>();
    private final ThreadLocal<RunningScript> currentScriptContext = new ThreadLocal<>();
    private final Queue<Context> contextPool = new ConcurrentLinkedQueue<>();

    private static final Pattern METADATA_PATTERN = Pattern.compile("^//\\s*@(\\w+)\\s+(.+)");

    public ScriptManager() {
        this.commandApiService = new CommandAPIService();
    }

    public void init(Engine scriptEngine, MappingsManager mappingsManager, ConfigManager configManager, EventManager eventManager, HookManager hookManager) {
        this.configManager = configManager;
        this.eventManager = eventManager;
        this.hookManager = hookManager;

        ensureScriptDirectory();
        ScriptingClassResolver classResolver = new ScriptingClassResolver();
        classResolver.init(mappingsManager);
        this.contextFactory = new ScriptContextFactory(classResolver, scriptEngine, this, this.eventManager, this.configManager, this.commandApiService, hookManager);
        this.scriptLoader = new ScriptLoader();
        prewarmContextPool();
        discoverScripts();
        loadAndEnableScriptsFromConfig();
    }

    private void loadAndEnableScriptsFromConfig() {
        Main.LOGGER.info("Checking configs to auto-enable scripts...");
        for (ScriptDescriptor descriptor : availableScripts.values()) {
            if (configManager.getEnabledState(descriptor.getId())) {
                Main.LOGGER.info("Auto-enabling script '{}' as per config.", descriptor.moduleName());
                enableScript(descriptor.getId());
            }
        }
    }


    private void prewarmContextPool() {
        Main.LOGGER.info("Pre-warming script context pool...");
        Context context = this.contextFactory.createContext(perFileExports);
        if (context != null) {
            contextPool.offer(context);
            Main.LOGGER.info("Context pool pre-warmed successfully.");
        } else {
            Main.LOGGER.error("Failed to create a context for the pre-warming pool.");
        }
    }

    public void enableAllScripts() {
        Main.LOGGER.info("Enabling all discovered scripts...");
        discoverScripts();
        for (String scriptId : availableScripts.keySet()) {
            enableScript(scriptId);
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

    private void ensureScriptDirectory() {
        Path p = Main.MOD_DIR.resolve("scripts");
        try {
            if (!Files.exists(p)) Files.createDirectories(p);
        } catch (IOException e) {
            Main.LOGGER.error("Failed create scripts dir: {}", p, e);
        }
    }

    private void discoverScripts() {
        availableScripts.clear();
        Path scriptsDir = Main.MOD_DIR.resolve("scripts");
        try (Stream<Path> paths = Files.walk(scriptsDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".js"))
                    .forEach(this::discoverModulesInFileByParsing);
        } catch (IOException e) {
            Main.LOGGER.error("Error discovering scripts in {}", scriptsDir, e);
        }
        Main.LOGGER.info("Discovered {} available script modules.", availableScripts.size());
    }

    private void discoverModulesInFileByParsing(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            Map<String, String> metadata = new HashMap<>();
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (!trimmedLine.startsWith("//")) {
                    if (metadata.containsKey("name")) break;
                    continue;
                }
                Matcher matcher = METADATA_PATTERN.matcher(trimmedLine);
                if (matcher.matches()) {
                    metadata.put(matcher.group(1).toLowerCase(), matcher.group(2).trim());
                }
            }

            String moduleName = metadata.get("name");
            if (moduleName == null || moduleName.isEmpty()) {
                return;
            }

            String version = metadata.getOrDefault("version", "N/A");
            ScriptDescriptor descriptor = new ScriptDescriptor(path, moduleName, version);
            availableScripts.put(descriptor.getId(), descriptor);

        } catch (IOException e) {
            Main.LOGGER.error("Could not read script file for metadata: {}", path, e);
        }
    }

    private Context getContextFromPool() {
        Context context = contextPool.poll();
        if (context == null) {
            Main.LOGGER.info("Context pool is empty. Creating a new context.");
            context = this.contextFactory.createContext(perFileExports);
        }
        return context;
    }

    private void returnContextToPool(Context context) {
        if (context != null) {
            contextFactory.resetContext(context);
            contextPool.offer(context);
        }
    }


    public void enableScript(String scriptId) {
        if (runningScripts.containsKey(scriptId)) {
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
            scriptContext = getContextFromPool();
            Map<String, Value> fileExports = scriptLoader.loadModules(descriptor.path(), scriptContext, perFileExports);

            if (fileExports.isEmpty()) {
                throw new IllegalStateException("No modules were exported from '" + descriptor.path().getFileName() + "'. Did you use exportModule()?");
            }

            Value scriptClass = fileExports.values().iterator().next();

            if (scriptClass == null || !scriptClass.canInstantiate()) {
                throw new IllegalStateException("The module exported from '" + descriptor.path().getFileName() + "' is not an instantiable class.");
            }

            Value jsInstance = scriptClass.newInstance();
            RunningScript runningScript = new RunningScript(descriptor, jsInstance, scriptContext);

            runningScripts.put(scriptId, runningScript);

            setCurrentScript(runningScript);
            try {
                runningScript.onEnable();
            } finally {
                clearCurrentScript();
            }

            Main.LOGGER.info("Enabled script: {}", runningScript.getName());
        } catch (Exception e) {
            returnContextToPool(scriptContext);
            Main.LOGGER.error("Failed to enable script '{}'. It may be in a broken state. Please disable it to ensure cleanup.", scriptId, e);
        }
    }

    public void disableScript(String scriptId) {
        RunningScript script = runningScripts.remove(scriptId);
        if (script != null) {
            setCurrentScript(script);
            try {
                script.onDisable();
            } finally {
                eventManager.unregister(script);
                commandApiService.unregisterAllFor(script);
                hookManager.unhookAll(script);
                configManager.saveConfig(script);
                configManager.unloadConfig(script);
                returnContextToPool(script.getContext());
                script.invalidate();
                clearCurrentScript();
            }
            Main.LOGGER.info("Disabled script: {}", script.getName());
        }
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
// path: java/net/me/scripting/ScriptManager.java

package net.me.scripting;

import net.me.Main;
import net.me.scripting.engine.ScriptContextFactory;
import net.me.scripting.engine.ScriptLoader;
import net.me.scripting.engine.ScriptingClassResolver;
import net.me.scripting.module.RunningScript;
import net.me.scripting.module.ScriptDescriptor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ScriptManager {
    private static ScriptManager instance;
    private final Map<String, ScriptDescriptor> availableScripts = new HashMap<>();
    private final Map<String, RunningScript> runningScripts = new HashMap<>();

    private ScriptContextFactory contextFactory;
    private ScriptLoader scriptLoader;
    private Context scriptContext;

    private final ThreadLocal<Map<String, Value>> perFileExports = new ThreadLocal<>();

    private ScriptManager() {
    }

    public static ScriptManager getInstance() {
        if (instance == null) instance = new ScriptManager();
        return instance;
    }

    public void init() {
        ensureScriptDirectory();
        ScriptingClassResolver classResolver = new ScriptingClassResolver();
        classResolver.init();
        this.contextFactory = new ScriptContextFactory(classResolver);
        this.scriptLoader = new ScriptLoader();
        this.scriptContext = this.contextFactory.createContext(perFileExports);
        discoverScripts();
    }

    private void refreshScriptContext() {
        this.scriptContext = this.contextFactory.createContext(perFileExports);
    }

    public void enableAllScripts() {
        Main.LOGGER.info("Enabling all discovered scripts...");
        for (String scriptId : availableScripts.keySet()) {
            enableScript(scriptId);
        }
    }

    public void refreshAndReenable() {
        refreshScriptContext();
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
                    .forEach(this::discoverModulesInFile);
        } catch (IOException e) {
            Main.LOGGER.error("Error discovering scripts in {}", scriptsDir, e);
        }
        Main.LOGGER.info("Discovered {} available script modules.", availableScripts.size());
    }

    private void discoverModulesInFile(Path path) {
        Map<String, Value> discoveredModules = scriptLoader.loadModules(path, this.scriptContext, perFileExports);
        for (Map.Entry<String, Value> entry : discoveredModules.entrySet()) {
            ScriptDescriptor descriptor = new ScriptDescriptor(path, entry.getKey());
            availableScripts.put(descriptor.getId(), descriptor);
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

        try {
            Map<String, Value> fileExports = scriptLoader.loadModules(descriptor.path(), this.scriptContext, perFileExports);
            Value scriptClass = fileExports.get(descriptor.moduleName());

            if (scriptClass == null || !scriptClass.canInstantiate()) {
                throw new IllegalStateException("Module '" + descriptor.moduleName() + "' was not found or is not an instantiable class after loading. Did you use exportModule()?");
            }
            Value jsInstance = scriptClass.newInstance();
            RunningScript runningScript = new RunningScript(descriptor, jsInstance);

            runningScripts.put(scriptId, runningScript);
            runningScript.onEnable();
            Main.LOGGER.info("Enabled script: {}", runningScript.getName());
        } catch (Exception e) {
            Main.LOGGER.error("Failed to enable script '{}'", scriptId, e);
        }
    }

    public void disableScript(String scriptId) {
        RunningScript script = runningScripts.remove(scriptId);
        if (script != null) {
            script.onDisable();
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
}
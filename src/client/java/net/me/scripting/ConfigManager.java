package net.me.scripting;

import net.me.Main;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
    private static final ConfigManager INSTANCE = new ConfigManager();
    private final Path configsDir = Main.MOD_DIR.resolve("configs");

    private final Map<String, Value> inMemoryConfigs = new ConcurrentHashMap<>();

    private ConfigManager() {
    }

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        try {
            if (!Files.exists(configsDir)) {
                Files.createDirectories(configsDir);
            }
        } catch (IOException e) {
            Main.LOGGER.error("Failed to create script configs directory", e);
        }
    }

    private Path getConfigFile(RunningScript script) {
        String safeFileName = script.getId().replace(":", "-").replaceAll("[^a-zA-Z0-9.-]", "_") + ".json";
        return configsDir.resolve(safeFileName);
    }

    public Value getConfigForScript(RunningScript script) {
        return inMemoryConfigs.computeIfAbsent(script.getId(), id -> loadConfig(script));
    }

    private Value loadConfig(RunningScript script) {
        Path configFile = getConfigFile(script);
        Context context = script.getJsInstance().getContext();

        if (Files.exists(configFile)) {
            try {
                String jsonContent = Files.readString(configFile);
                if (!jsonContent.isBlank()) {
                    Value JSON = context.getBindings("js").getMember("JSON");
                    return JSON.invokeMember("parse", jsonContent);
                }
            } catch (Exception e) {
                Main.LOGGER.error("Failed to load or parse config for script '{}': {}", script.getId(), e.getMessage());
            }
        }
        return context.eval("js", "({})");
    }

    public void saveConfig(RunningScript script) {
        String scriptId = script.getId();
        if (!inMemoryConfigs.containsKey(scriptId)) {
            return;
        }

        Value configValue = inMemoryConfigs.get(scriptId);
        Path configFile = getConfigFile(script);

        try {
            Value JSON = script.getJsInstance().getContext().getBindings("js").getMember("JSON");
            String jsonString = JSON.invokeMember("stringify", configValue, null, 2).asString();

            Files.writeString(configFile, jsonString);
            Main.LOGGER.info("Saved config for script '{}'.", script.getName());
        } catch (IOException e) {
            Main.LOGGER.error("Failed to save config for script '{}': {}", script.getId(), e.getMessage());
        } catch (Exception e) {
            Main.LOGGER.error("Failed to stringify config for script '{}': {}", script.getId(), e.getMessage());
        }
    }

    public void unloadConfig(RunningScript script) {
        inMemoryConfigs.remove(script.getId());
    }

    public int saveAllConfigs() {
        ScriptManager scriptManager = ScriptManager.getInstance();
        Collection<RunningScript> runningScripts = scriptManager.getRunningScripts();
        for (RunningScript script : runningScripts) {
            this.saveConfig(script);
        }
        return runningScripts.size();
    }
}
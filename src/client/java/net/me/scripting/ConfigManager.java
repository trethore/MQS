package net.me.scripting;

import net.me.Main;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
    private final Path configsDir = Main.MOD_DIR.resolve("configs");

    private final Map<String, Value> inMemoryConfigs = new ConcurrentHashMap<>();
    private Context configContext;


    public void init() {
        try {
            if (!Files.exists(configsDir)) {
                Files.createDirectories(configsDir);
            }
            this.configContext = Context.newBuilder("js")
                    .allowHostAccess(HostAccess.ALL)
                    .build();
        } catch (IOException e) {
            Main.LOGGER.error("Failed to create script configs directory", e);
        }
    }

    private Path getConfigFile(String scriptId) {
        String safeFileName = scriptId.replace(":", "-").replaceAll("[^a-zA-Z0-9.-]", "_") + ".json";
        return configsDir.resolve(safeFileName);
    }

    public Value getConfig(String scriptId) {
        return inMemoryConfigs.computeIfAbsent(scriptId, this::loadConfigFromFile);
    }

    public Value getConfigForScript(RunningScript script) {
        return getConfig(script.getId());
    }

    private Value loadConfigFromFile(String scriptId) {
        Path configFile = getConfigFile(scriptId);

        if (Files.exists(configFile)) {
            try {
                String jsonContent = Files.readString(configFile);
                if (!jsonContent.isBlank()) {
                    Value JSON = configContext.getBindings("js").getMember("JSON");
                    return JSON.invokeMember("parse", jsonContent);
                }
            } catch (Exception e) {
                Main.LOGGER.error("Failed to load or parse config for script ID '{}': {}", scriptId, e.getMessage());
            }
        }
        return configContext.eval("js", "({})");
    }

    /* <<<<<<<<<<<<<<  ✨ Windsurf Command ⭐ >>>>>>>>>>>>>>>> */

    /**
     * Sets the enabled state of a script's configuration.
     *
     * @param scriptId  the identifier of the script whose configuration is to be modified
     * @param isEnabled the new enabled state to set in the configuration
     */

    /* <<<<<<<<<<  b2411796-1816-4e23-a32d-f2d848ebc723  >>>>>>>>>>> */
    public void setEnabledState(String scriptId, boolean isEnabled) {
        Value config = getConfig(scriptId);
        config.putMember("enabled", isEnabled);
    }

    public boolean getEnabledState(String scriptId) {
        Value config = getConfig(scriptId);
        if (config.hasMember("enabled")) {
            Value enabled = config.getMember("enabled");
            return enabled != null && !enabled.isNull() && enabled.isBoolean() && enabled.asBoolean();
        }
        return false;
    }

    public void saveConfig(RunningScript script) {
        saveConfig(script.getId());
    }

    public void saveConfig(String scriptId) {
        if (!inMemoryConfigs.containsKey(scriptId)) {
            return;
        }

        Value configValue = inMemoryConfigs.get(scriptId);
        Path configFile = getConfigFile(scriptId);

        boolean isEnabled = getEnabledState(scriptId);
        long keyCount = configValue.getMemberKeys().size();

        if (!isEnabled && keyCount <= 1) {
            if (Files.exists(configFile)) {
                try {
                    Files.delete(configFile);
                    Main.LOGGER.info("Deleted empty config for disabled script '{}'.", scriptId);
                } catch (IOException e) {
                    Main.LOGGER.error("Failed to delete config for script '{}'", scriptId, e);
                }
            }
            inMemoryConfigs.remove(scriptId);
            return;
        }

        try {
            Value JSON = configContext.getBindings("js").getMember("JSON");
            String jsonString = JSON.invokeMember("stringify", configValue, null, 2).asString();

            Files.writeString(configFile, jsonString);
        } catch (IOException e) {
            Main.LOGGER.error("Failed to save config for script '{}': {}", scriptId, e.getMessage());
        } catch (Exception e) {
            Main.LOGGER.error("Failed to stringify config for script '{}': {}", scriptId, e.getMessage());
        }
    }


    public void unloadConfig(RunningScript script) {
        inMemoryConfigs.remove(script.getId());
    }

    public int saveAllConfigs() {
        int count = inMemoryConfigs.size();
        if (count == 0) return 0;

        Main.LOGGER.info("Saving all modified script configurations...");
        for (String scriptId : inMemoryConfigs.keySet()) {
            saveConfig(scriptId);
        }
        return count;
    }
}
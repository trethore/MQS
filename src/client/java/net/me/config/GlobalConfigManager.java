package net.me.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.me.Main;
import net.me.console.ConsoleManager;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class GlobalConfigManager {
    private static final Path CONFIG_FILE = Main.MOD_DIR.resolve("mqs_config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final ConsoleManager consoleManager;
    private ConfigData data = new ConfigData();

    public GlobalConfigManager(ConsoleManager consoleManager) {
        this.consoleManager = consoleManager;
    }


    public void init() {
        load();
    }

    public void load() {
        if (!Files.exists(CONFIG_FILE)) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE.toFile())) {
            this.data = GSON.fromJson(reader, ConfigData.class);
            if (this.data == null) {
                this.data = new ConfigData();
            } else {
                consoleManager.setLogRedirect(this.data.logRedirect);
            }
        } catch (Exception e) {
            Main.LOGGER.error("Failed to load global MQS config, using defaults.", e);
            this.data = new ConfigData();
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE.toFile())) {
            GSON.toJson(this.data, writer);
        } catch (Exception e) {
            Main.LOGGER.error("Failed to save global MQS config.", e);
        }
    }

    public boolean isLogRedirectEnabled() {
        return data.logRedirect;
    }

    public void setLogRedirectEnabled(boolean enabled) {
        if (data.logRedirect != enabled) {
            consoleManager.setLogRedirect(enabled);
            data.logRedirect = enabled;
        }
    }

    public boolean areAllClassesAllowed() {
        return data.allowAllClasses;
    }

    public void setAllClassesAllowed(boolean allowed) {
        if (data.allowAllClasses != allowed) {
            data.allowAllClasses = allowed;
        }
    }

    private static class ConfigData {
        @SerializedName("logRedirect")
        boolean logRedirect = false;

        @SerializedName("allowAllClasses")
        boolean allowAllClasses = false;
    }
}
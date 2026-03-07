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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

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
            }
            this.data.ensureDefaults();
            consoleManager.setLogRedirect(this.data.logRedirect);
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
            save();
        }
    }

    public boolean areAllClassesAllowed() {
        return data.allowAllClasses;
    }

    public void setAllClassesAllowed(boolean allowed) {
        if (data.allowAllClasses != allowed) {
            data.allowAllClasses = allowed;
            save();
        }
    }

    public List<String> getAdditionalScriptDirectories() {
        data.ensureDefaults();
        return Collections.unmodifiableList(data.additionalScriptDirs);
    }

    public void setAdditionalScriptDirectories(List<String> directories) {
        data.ensureDefaults();
        List<String> sanitizedDirectories = sanitizeAdditionalScriptDirectories(directories);
        if (!sanitizedDirectories.equals(data.additionalScriptDirs)) {
            data.additionalScriptDirs = sanitizedDirectories;
            save();
        }
    }

    public String getDefaultIdeCommand() {
        data.ensureDefaults();
        return data.defaultIdeCommand;
    }

    public void setDefaultIdeCommand(String command) {
        String sanitized = command == null || command.isBlank() ? "code" : command.trim();
        if (!sanitized.equals(data.defaultIdeCommand)) {
            data.defaultIdeCommand = sanitized;
            save();
        }
    }

    public OptionsSnapshot getOptionsSnapshot() {
        data.ensureDefaults();
        return new OptionsSnapshot(
                data.logRedirect,
                data.allowAllClasses,
                List.copyOf(data.additionalScriptDirs),
                data.defaultIdeCommand
        );
    }

    private List<String> sanitizeAdditionalScriptDirectories(List<String> directories) {
        if (directories == null || directories.isEmpty()) {
            return new ArrayList<>();
        }

        LinkedHashSet<String> sanitizedDirectories = new LinkedHashSet<>();
        for (String directory : directories) {
            if (directory == null) {
                continue;
            }

            String trimmedDirectory = directory.trim();
            if (!trimmedDirectory.isEmpty()) {
                sanitizedDirectories.add(trimmedDirectory);
            }
        }

        return new ArrayList<>(sanitizedDirectories);
    }

    private static class ConfigData {
        @SerializedName(ConfigKeys.LOG_REDIRECT)
        boolean logRedirect = false;

        @SerializedName(ConfigKeys.ALLOW_ALL_CLASSES)
        boolean allowAllClasses = false;

        @SerializedName(ConfigKeys.ADDITIONAL_SCRIPT_DIRS)
        List<String> additionalScriptDirs = new ArrayList<>();

        @SerializedName(ConfigKeys.DEFAULT_IDE_COMMAND)
        String defaultIdeCommand = "code";

        void ensureDefaults() {
            if (additionalScriptDirs == null) {
                additionalScriptDirs = new ArrayList<>();
            }
            if (defaultIdeCommand == null || defaultIdeCommand.isBlank()) {
                defaultIdeCommand = "code";
            }
        }
    }

    public record OptionsSnapshot(
            boolean logRedirect,
            boolean allowAllClasses,
            List<String> additionalScriptDirs,
            String defaultIdeCommand
    ) {
    }
}

/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
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

    private static class ConfigData {
        @SerializedName(ConfigKeys.LOG_REDIRECT)
        boolean logRedirect = false;

        @SerializedName(ConfigKeys.ALLOW_ALL_CLASSES)
        boolean allowAllClasses = false;

    }
}

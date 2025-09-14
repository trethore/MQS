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

package net.me.scripting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.me.Main;
import net.me.config.ConfigKeys;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    private final Path configsDir = Main.MOD_DIR.resolve("configs");
    private final Map<String, Map<String, Object>> inMemoryConfigs = new ConcurrentHashMap<>();

    public void init() {
        try {
            if (!Files.exists(configsDir)) {
                Files.createDirectories(configsDir);
            }
        } catch (IOException e) {
            Main.LOGGER.error("Failed to create script configs directory", e);
        }
    }

    private Path getConfigFile(String scriptId) {
        String safeFileName = scriptId.replace(":", "-").replaceAll("[^a-zA-Z0-9.-]", "_") + ".json";
        return configsDir.resolve(safeFileName);
    }

    private Map<String, Object> getConfig(String scriptId) {
        return inMemoryConfigs.computeIfAbsent(scriptId, this::loadConfigFromFile);
    }

    public Value getConfigForScript(RunningScript script) {
        Map<String, Object> configMap = getConfig(script.getId());
        Context scriptContext = script.getContext();
        return scriptContext.asValue(configMap);
    }

    private Map<String, Object> loadConfigFromFile(String scriptId) {
        Path configFile = getConfigFile(scriptId);

        if (Files.exists(configFile)) {
            try (FileReader reader = new FileReader(configFile.toFile())) {
                Map<String, Object> loadedConfig = GSON.fromJson(reader, MAP_TYPE);
                return loadedConfig != null ? new ConcurrentHashMap<>(loadedConfig) : new ConcurrentHashMap<>();
            } catch (JsonSyntaxException e) {
                Main.LOGGER.error("Failed to parse config for script ID '{}' due to invalid JSON. A new config will be created.", scriptId, e);
            } catch (Exception e) {
                Main.LOGGER.error("Failed to load config for script ID '{}': {}", scriptId, e.getMessage());
            }
        }
        return new ConcurrentHashMap<>();
    }

    public void setEnabledState(String scriptId, boolean isEnabled) {
        Map<String, Object> config = getConfig(scriptId);
        config.put(ConfigKeys.ENABLED, isEnabled);
    }

    // Category IDs support removed with GUI cleanup.

    public boolean getEnabledState(String scriptId) {
        Object enabled = getConfig(scriptId).get(ConfigKeys.ENABLED);
        return enabled instanceof Boolean && (Boolean) enabled;
    }

    public Object get(String scriptId, String key) {
        return getConfig(scriptId).get(key);
    }

    @SuppressWarnings("unchecked")
    public Optional<Integer> getKeybind(String scriptId, String keybindName) {
        Map<String, Object> config = getConfig(scriptId);
        Object keybindsObj = config.get(ConfigKeys.KEYBINDS);

        if (keybindsObj instanceof Map) {
            Map<String, Object> keybinds = (Map<String, Object>) keybindsObj;
            Object keyObj = keybinds.get(keybindName);
            if (keyObj instanceof Number) {
                return Optional.of(((Number) keyObj).intValue());
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public void setKeybind(String scriptId, String keybindName, int keyCode) {
        Map<String, Object> config = getConfig(scriptId);
        Map<String, Object> keybinds = (Map<String, Object>) config.computeIfAbsent(ConfigKeys.KEYBINDS, k -> new ConcurrentHashMap<>());

        keybinds.put(keybindName, keyCode);
    }

    public void set(String scriptId, String key, Object value) {
        getConfig(scriptId).put(key, value);
    }

    public void saveConfig(RunningScript script) {
        saveConfig(script.getId());
    }

    public void saveConfig(String scriptId) {
        if (!inMemoryConfigs.containsKey(scriptId)) {
            return;
        }

        Map<String, Object> configMap = inMemoryConfigs.get(scriptId);
        Path configFile = getConfigFile(scriptId);

        boolean isEnabled = getEnabledState(scriptId);
        boolean shouldBeSaved = false;

        if (isEnabled) {
            shouldBeSaved = true;
        } else {
            if (configMap.size() > 1 || (configMap.size() == 1 && !configMap.containsKey(ConfigKeys.ENABLED))) {
                shouldBeSaved = true;
            }
        }

        if (configMap.isEmpty()) {
            shouldBeSaved = false;
        }

        if (shouldBeSaved) {
            try (FileWriter writer = new FileWriter(configFile.toFile())) {
                GSON.toJson(configMap, writer);
            } catch (Exception e) {
                Main.LOGGER.error("Failed to save config for script '{}': {}", scriptId, e.getMessage());
            }
        } else {
            if (Files.exists(configFile)) {
                try {
                    Files.delete(configFile);
                    Main.LOGGER.info("Deleted empty/default config for disabled script '{}'.", scriptId);
                } catch (IOException e) {
                    Main.LOGGER.error("Failed to delete empty config for script '{}'", scriptId, e);
                }
            }
            inMemoryConfigs.remove(scriptId);
        }
    }

    public void unloadConfig(RunningScript script) {
        inMemoryConfigs.remove(script.getId());
    }

    public int saveAllConfigs() {
        int count = inMemoryConfigs.size();
        if (count == 0) return 0;

        for (String scriptId : Set.copyOf(inMemoryConfigs.keySet())) {
            saveConfig(scriptId);
        }
        return count;
    }
}

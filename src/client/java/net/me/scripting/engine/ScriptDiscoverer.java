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

package net.me.scripting.engine;

import net.me.Main;
import net.me.config.ConfigKeys;
import net.me.config.GlobalConfigManager;
import net.me.scripting.module.ScriptDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ScriptDiscoverer {

    private static final Pattern MODULE_ANNOTATION_PATTERN = Pattern.compile("^//\\s*@module\\((.*)\\)");
    private final GlobalConfigManager globalConfigManager;
    private final Path defaultScriptsDir = Main.MOD_DIR.resolve("scripts");

    public ScriptDiscoverer(GlobalConfigManager globalConfigManager) {
        this.globalConfigManager = globalConfigManager;
        ensureDirectoryExists(defaultScriptsDir);
    }

    public Map<String, ScriptDescriptor> discoverScripts() {
        Map<String, ScriptDescriptor> availableScripts = new HashMap<>();
        List<Path> scriptDirectories = resolveScriptDirectories();

        for (Path directory : scriptDirectories) {
            discoverScriptsInDirectory(directory, availableScripts);
        }

        Main.LOGGER.info("Discovered {} available script modules from {} directories.", availableScripts.size(), scriptDirectories.size());
        return availableScripts;
    }

    private void discoverScriptsInDirectory(Path directory, Map<String, ScriptDescriptor> availableScripts) {
        if (!Files.isDirectory(directory)) {
            Main.LOGGER.warn("Configured script directory '{}' is missing or not a directory. Skipping.", directory);
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".js"))
                    .forEach(path -> discoverModulesInFile(path, availableScripts));
        } catch (IOException e) {
            Main.LOGGER.error("Error discovering scripts in {}", directory, e);
        }
    }

    private void discoverModulesInFile(Path path, Map<String, ScriptDescriptor> availableScripts) {
        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                Matcher matcher = MODULE_ANNOTATION_PATTERN.matcher(line.trim());
                if (matcher.matches()) {
                    String content = matcher.group(1);
                    Map<String, String> metadata = parseModuleMetadata(content);

                    String mainClass = metadata.get(ConfigKeys.SCRIPT_META_MAIN);
                    String moduleName = metadata.get(ConfigKeys.SCRIPT_META_NAME);

                    if (mainClass == null || moduleName == null) {
                        Main.LOGGER.warn("Skipping malformed @module in {}: 'main' and 'name' properties are required. Found: {}", path.getFileName(), line);
                        continue;
                    }

                    String version = metadata.getOrDefault(ConfigKeys.SCRIPT_META_VERSION, "N/A");
                    ScriptDescriptor descriptor = new ScriptDescriptor(path, moduleName, version, mainClass);

                    if (availableScripts.containsKey(descriptor.getId())) {
                        Main.LOGGER.warn("Duplicate script ID found in {}: {}. The last one found will be used.", path.getFileName(), descriptor.getId());
                    }
                    availableScripts.put(descriptor.getId(), descriptor);
                }
            }
        } catch (IOException e) {
            Main.LOGGER.error("Could not read script file for metadata: {}", path, e);
        }
    }

    private Map<String, String> parseModuleMetadata(String content) {
        Map<String, String> metadata = new HashMap<>();
        String[] pairs = content.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                if (value.startsWith("'") && value.endsWith("'") || value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                metadata.put(key, value);
            }
        }
        return metadata;
    }

    private void ensureDirectoryExists(Path directory) {
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }
        } catch (IOException e) {
            Main.LOGGER.error("Failed to create scripts directory: {}", directory, e);
        }
    }

    private List<Path> resolveScriptDirectories() {
        List<Path> directories = new ArrayList<>();
        directories.add(defaultScriptsDir);

        if (globalConfigManager != null) {
            List<String> configured = globalConfigManager.getAdditionalScriptDirectories();
            for (String entry : configured) {
                Path resolved = resolveConfiguredPath(entry);
                if (resolved != null) {
                    directories.add(resolved);
                }
            }
        }

        Set<Path> unique = new LinkedHashSet<>();
        for (Path directory : directories) {
            unique.add(directory.normalize());
        }
        return new ArrayList<>(unique);
    }

    private Path resolveConfiguredPath(String entry) {
        if (entry == null) {
            return null;
        }
        String trimmed = entry.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String expanded = expandHomeDirectory(trimmed);
        Path candidate;
        try {
            candidate = Path.of(expanded);
        } catch (InvalidPathException e) {
            Main.LOGGER.warn("Ignoring invalid script directory '{}'.", entry, e);
            return null;
        }

        if (!candidate.isAbsolute()) {
            candidate = Main.MOD_DIR.resolve(candidate);
        }
        return candidate.normalize();
    }

    private String expandHomeDirectory(String path) {
        if (path.startsWith("~")) {
            String home = System.getProperty("user.home");
            if (home != null && !home.isBlank()) {
                if (path.equals("~")) {
                    return home;
                }
                if (path.startsWith("~" + File.separator) || path.startsWith("~/") || path.startsWith("~\\")) {
                    return home + path.substring(1);
                }
            }
        }
        return path;
    }
}

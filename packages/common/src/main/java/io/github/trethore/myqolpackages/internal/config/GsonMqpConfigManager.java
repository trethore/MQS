/*
 * My QOL Packages - Client-side Minecraft modding at runtime
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
package io.github.trethore.myqolpackages.internal.config;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.github.trethore.myqolpackages.api.config.MqpConfig;
import io.github.trethore.myqolpackages.api.config.MqpPermissionsConfig;
import io.github.trethore.myqolpackages.api.config.PackagePermissionOverrides;
import io.github.trethore.myqolpackages.api.packages.PackageDiagnostic;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class GsonMqpConfigManager {
  private static final String CONFIG_DIAGNOSTIC_ID = "config";
  private static final String CONFIG_FILE_NAME = "config.json";

  private final Path configPath;
  private final Gson gson;
  private final AtomicReference<MqpConfig> config = new AtomicReference<>(MqpConfig.defaults());
  private boolean configLoadedSuccessfully;

  public GsonMqpConfigManager(Path mqpDirectory) {
    configPath = mqpDirectory.resolve(CONFIG_FILE_NAME);
    gson = MqpGson.newBuilder().setPrettyPrinting().create();
  }

  public synchronized MqpConfigLoadResult load() {
    try {
      Files.createDirectories(configPath.getParent());
      if (Files.notExists(configPath)) {
        writeDefaultConfig();
      }

      MqpConfig loadedConfig = readConfig();
      config.set(loadedConfig);
      configLoadedSuccessfully = true;
      return new MqpConfigLoadResult(loadedConfig, List.of());
    } catch (IOException | RuntimeException exception) {
      MqpConfig defaultConfig = MqpConfig.defaults();
      config.set(defaultConfig);
      configLoadedSuccessfully = false;
      PackageDiagnostic diagnostic =
          new PackageDiagnostic(
              CONFIG_DIAGNOSTIC_ID,
              configPath,
              "Could not load config.json: " + exception.getMessage());
      return new MqpConfigLoadResult(defaultConfig, List.of(diagnostic));
    }
  }

  public MqpConfig getConfig() {
    return config.get();
  }

  public Path getConfigPath() {
    return configPath;
  }

  public synchronized void addEnabledPackage(String packageId) throws IOException {
    List<String> enabledPackages = new ArrayList<>(config.get().enabledPackages());
    if (enabledPackages.contains(packageId)) {
      return;
    }
    enabledPackages.add(packageId);
    saveEnabledPackages(enabledPackages);
  }

  public synchronized void removeEnabledPackage(String packageId) throws IOException {
    List<String> enabledPackages = new ArrayList<>(config.get().enabledPackages());
    if (!enabledPackages.remove(packageId)) {
      return;
    }
    saveEnabledPackages(enabledPackages);
  }

  public synchronized void setGlobalPermissions(PackagePermissionOverrides permissions)
      throws IOException {
    Objects.requireNonNull(permissions, "permissions");
    MqpConfig currentConfig = config.get();
    MqpPermissionsConfig updatedPermissions =
        new MqpPermissionsConfig(permissions, currentConfig.permissions().packages());
    saveConfig(
        new MqpConfig(
            currentConfig.additionalPackageRoots(),
            currentConfig.enabledPackages(),
            updatedPermissions));
  }

  private MqpConfig readConfig() throws IOException {
    try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
      MqpConfig loadedConfig = gson.fromJson(reader, MqpConfig.class);
      if (loadedConfig == null) {
        throw new JsonSyntaxException("Configuration must contain a JSON object");
      }
      return loadedConfig;
    }
  }

  private void writeDefaultConfig() throws IOException {
    writeConfig(MqpConfig.defaults());
  }

  private void saveEnabledPackages(List<String> enabledPackages) throws IOException {
    MqpConfig currentConfig = config.get();
    saveConfig(
        new MqpConfig(
            currentConfig.additionalPackageRoots(), enabledPackages, currentConfig.permissions()));
  }

  private void saveConfig(MqpConfig updatedConfig) throws IOException {
    ensureConfigCanBeSaved();
    writeConfig(updatedConfig);
    config.set(updatedConfig);
  }

  private void ensureConfigCanBeSaved() throws IOException {
    if (!configLoadedSuccessfully) {
      throw new IOException("Configuration cannot be saved because config.json failed to load");
    }
  }

  private void writeConfig(MqpConfig updatedConfig) throws IOException {
    Files.createDirectories(configPath.getParent());
    Path temporaryConfig =
        Files.createTempFile(configPath.getParent(), CONFIG_FILE_NAME + ".", ".tmp");
    try {
      try (Writer writer = Files.newBufferedWriter(temporaryConfig, StandardCharsets.UTF_8)) {
        gson.toJson(updatedConfig, writer);
        writer.write(System.lineSeparator());
      }
      moveConfigIntoPlace(temporaryConfig);
    } finally {
      Files.deleteIfExists(temporaryConfig);
    }
  }

  private void moveConfigIntoPlace(Path temporaryConfig) throws IOException {
    try {
      Files.move(
          temporaryConfig,
          configPath,
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException exception) {
      Files.move(temporaryConfig, configPath, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}

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
package io.github.trethore.myqolpackages.api;

import io.github.trethore.myqolpackages.api.config.MqpConfig;
import io.github.trethore.myqolpackages.api.packages.PackageDiagnostic;
import io.github.trethore.myqolpackages.api.packages.PackageDiscoveryResult;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.internal.config.ConfiguredPackageRootProvider;
import io.github.trethore.myqolpackages.internal.config.GsonMqpConfigManager;
import io.github.trethore.myqolpackages.internal.packages.DefaultPackageManager;
import io.github.trethore.myqolpackages.internal.packages.FileSystemPackageDiscovery;
import io.github.trethore.myqolpackages.internal.runtime.GraalPackageContextFactory;
import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MqpRuntime {
  private static final Logger LOGGER = LoggerFactory.getLogger(MqpRuntime.class);

  private final GsonMqpConfigManager configManager;
  private final PackageManager packageManager;

  private MqpRuntime(GsonMqpConfigManager configManager, PackageManager packageManager) {
    this.configManager = configManager;
    this.packageManager = packageManager;
  }

  public static MqpRuntime create(Path mqpDirectory, String mqpVersion) {
    return create(
        mqpDirectory,
        mqpVersion,
        MqpRuntimeEnvironment.identity(MqpRuntime.class.getClassLoader()));
  }

  public static MqpRuntime create(
      Path mqpDirectory, String mqpVersion, MqpRuntimeEnvironment environment) {
    Objects.requireNonNull(mqpDirectory, "mqpDirectory");
    Objects.requireNonNull(mqpVersion, "mqpVersion");
    Objects.requireNonNull(environment, "environment");
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(mqpDirectory);
    PackageManager packageManager =
        new DefaultPackageManager(
            new ConfiguredPackageRootProvider(mqpDirectory, configManager),
            new FileSystemPackageDiscovery(),
            configManager,
            new GraalPackageContextFactory(mqpDirectory, mqpVersion, environment));
    return new MqpRuntime(configManager, packageManager);
  }

  @SuppressWarnings("UnusedReturnValue")
  public PackageDiscoveryResult start() {
    PackageDiscoveryResult result = packageManager.reload();
    LOGGER.info(
        "Discovered {} package(s) with {} diagnostic(s)",
        result.packages().size(),
        result.diagnostics().size());
    for (PackageDiagnostic diagnostic : result.diagnostics()) {
      LOGGER.warn(
          "MQP diagnostic for {} at {}: {}",
          diagnostic.packageId(),
          diagnostic.packageDirectory(),
          diagnostic.message());
    }
    return result;
  }

  public void stop() {
    packageManager.close();
  }

  public void tick() {
    packageManager.tick();
  }

  public PackageManager getPackageManager() {
    return packageManager;
  }

  public MqpConfig getConfig() {
    return configManager.getConfig();
  }
}

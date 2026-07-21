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

import io.github.trethore.myqolpackages.api.packages.PackageDiagnostic;
import io.github.trethore.myqolpackages.api.packages.PackageDiscoveryResult;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.internal.packages.DefaultPackageManager;
import io.github.trethore.myqolpackages.internal.packages.FileSystemPackageDiscovery;
import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MqpRuntime {
  private static final Logger LOGGER = LoggerFactory.getLogger(MqpRuntime.class);

  private final PackageManager packageManager;

  private MqpRuntime(PackageManager packageManager) {
    this.packageManager = packageManager;
  }

  public static MqpRuntime create(Path packageDirectory) {
    Objects.requireNonNull(packageDirectory, "packageDirectory");
    PackageManager packageManager =
        new DefaultPackageManager(packageDirectory, new FileSystemPackageDiscovery());
    return new MqpRuntime(packageManager);
  }

  public PackageDiscoveryResult start() {
    PackageDiscoveryResult result = packageManager.refresh();
    LOGGER.info(
        "Discovered {} package(s) with {} diagnostic(s)",
        result.packages().size(),
        result.diagnostics().size());
    for (PackageDiagnostic diagnostic : result.diagnostics()) {
      LOGGER.warn(
          "Could not discover package {} at {}: {}",
          diagnostic.packageId(),
          diagnostic.packageDirectory(),
          diagnostic.message());
    }
    return result;
  }

  public PackageManager getPackageManager() {
    return packageManager;
  }
}

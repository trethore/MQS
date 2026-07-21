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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.myqolpackages.internal.packages.PackageRootResolution;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfiguredPackageRootProviderTest {
  @TempDir Path temporaryDirectory;

  @Test
  void resolvesRelativeAndAbsolutePackageRoots() throws IOException {
    Path mqpDirectory = temporaryDirectory.resolve("minecraft/myqolpackages");
    Path relativeRoot = temporaryDirectory.resolve("minecraft/shared-packages");
    Path absoluteRoot = temporaryDirectory.resolve("absolute-packages");
    Files.createDirectories(mqpDirectory);
    Files.createDirectories(relativeRoot);
    Files.createDirectories(absoluteRoot);
    Files.writeString(
        mqpDirectory.resolve("config.json"),
        """
        {
          "additionalPackageRoots": [
            "../shared-packages",
            "%s"
          ]
        }
        """
            .formatted(escapeJson(absoluteRoot.toString())));
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(mqpDirectory);
    ConfiguredPackageRootProvider rootProvider =
        new ConfiguredPackageRootProvider(mqpDirectory, configManager);

    PackageRootResolution result = rootProvider.resolvePackageRoots();

    assertEquals(
        List.of(mqpDirectory.toRealPath(), relativeRoot.toRealPath(), absoluteRoot.toRealPath()),
        result.packageRoots());
    assertTrue(result.diagnostics().isEmpty());
  }

  @Test
  void reportsMissingAdditionalPackageRoot() throws IOException {
    Path mqpDirectory = temporaryDirectory.resolve("myqolpackages");
    Files.createDirectories(mqpDirectory);
    Files.writeString(
        mqpDirectory.resolve("config.json"),
        """
        {
          "additionalPackageRoots": ["../missing"]
        }
        """);
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(mqpDirectory);
    ConfiguredPackageRootProvider rootProvider =
        new ConfiguredPackageRootProvider(mqpDirectory, configManager);

    PackageRootResolution result = rootProvider.resolvePackageRoots();

    assertEquals(List.of(mqpDirectory.toRealPath()), result.packageRoots());
    assertEquals(1, result.diagnostics().size());
    assertEquals("config", result.diagnostics().getFirst().packageId());
  }

  private static String escapeJson(String value) {
    return value.replace("\\", "\\\\");
  }
}

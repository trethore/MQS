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
package io.github.trethore.myqolpackages.internal.packages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.myqolpackages.api.packages.PackageDiscoveryResult;
import io.github.trethore.myqolpackages.api.packages.PackageInfo;
import io.github.trethore.myqolpackages.internal.config.ConfiguredPackageRootProvider;
import io.github.trethore.myqolpackages.internal.config.GsonMqpConfigManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultPackageManagerTest {
  @TempDir Path temporaryDirectory;

  @Test
  void discoversPackagesFromMultipleRoots() throws IOException {
    Path defaultRoot = temporaryDirectory.resolve("default");
    Path additionalRoot = temporaryDirectory.resolve("additional");
    createPackage(defaultRoot, "default-package", "Default Package");
    createPackage(additionalRoot, "additional-package", "Additional Package");
    DefaultPackageManager packageManager = createPackageManager(defaultRoot, additionalRoot);

    PackageDiscoveryResult result = packageManager.refresh();

    assertEquals(List.of("default-package", "additional-package"), packageIds(result.packages()));
    assertTrue(result.diagnostics().isEmpty());
    assertEquals(
        additionalRoot.resolve("additional-package"),
        packageManager.findPackage("additional-package").orElseThrow().packageDirectory());
  }

  @Test
  void keepsFirstPackageWhenIdsAreDuplicated() throws IOException {
    Path defaultRoot = temporaryDirectory.resolve("default");
    Path additionalRoot = temporaryDirectory.resolve("additional");
    createPackage(defaultRoot, "duplicate", "Default Package");
    createPackage(additionalRoot, "duplicate", "Additional Package");
    DefaultPackageManager packageManager = createPackageManager(defaultRoot, additionalRoot);

    PackageDiscoveryResult result = packageManager.refresh();

    assertEquals(1, result.packages().size());
    assertEquals("Default Package", result.packages().getFirst().name());
    assertEquals(1, result.diagnostics().size());
    assertTrue(result.diagnostics().getFirst().message().startsWith("Duplicate package ID;"));
  }

  @Test
  void refreshReloadsConfiguredPackageRoots() throws IOException {
    Path mqpDirectory = temporaryDirectory.resolve("myqolpackages");
    Path additionalRoot = temporaryDirectory.resolve("additional");
    createPackage(mqpDirectory, "default-package", "Default Package");
    createPackage(additionalRoot, "additional-package", "Additional Package");
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(mqpDirectory);
    DefaultPackageManager packageManager =
        new DefaultPackageManager(
            new ConfiguredPackageRootProvider(mqpDirectory, configManager),
            new FileSystemPackageDiscovery());

    PackageDiscoveryResult initialResult = packageManager.refresh();
    Files.writeString(
        mqpDirectory.resolve("config.json"),
        """
        {
          "additionalPackageRoots": ["%s"]
        }
        """
            .formatted(escapeJson(additionalRoot.toString())));
    PackageDiscoveryResult refreshedResult = packageManager.refresh();

    assertEquals(List.of("default-package"), packageIds(initialResult.packages()));
    assertEquals(
        List.of("default-package", "additional-package"), packageIds(refreshedResult.packages()));
  }

  private static DefaultPackageManager createPackageManager(Path... packageRoots) {
    PackageRootProvider rootProvider =
        () -> new PackageRootResolution(List.of(packageRoots), List.of());
    return new DefaultPackageManager(rootProvider, new FileSystemPackageDiscovery());
  }

  private static List<String> packageIds(List<PackageInfo> packages) {
    return packages.stream().map(PackageInfo::id).toList();
  }

  private static void createPackage(Path root, String id, String name) throws IOException {
    Path packageDirectory = root.resolve(id);
    Files.createDirectories(packageDirectory.resolve("src"));
    Files.writeString(
        packageDirectory.resolve("manifest.json"),
        """
        {
          "name": "%s",
          "description": "A test package.",
          "version": "1.0.0",
          "entrypoint": "src/index.js"
        }
        """
            .formatted(name));
    Files.writeString(packageDirectory.resolve("src/index.js"), "");
  }

  private static String escapeJson(String value) {
    return value.replace("\\", "\\\\");
  }
}

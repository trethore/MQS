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

import io.github.trethore.myqolpackages.api.packages.PackageDiagnostic;
import io.github.trethore.myqolpackages.api.packages.PackageDiscoveryResult;
import io.github.trethore.myqolpackages.api.packages.PackageInfo;
import io.github.trethore.myqolpackages.internal.config.ConfiguredPackageRootProvider;
import io.github.trethore.myqolpackages.internal.config.GsonMqpConfigManager;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextFactory;
import io.github.trethore.myqolpackages.internal.runtime.PackageScriptContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultPackageManagerTest {
  private static final PackageContextFactory TEST_CONTEXT_FACTORY =
      spec -> new TestPackageScriptContext();

  @TempDir Path temporaryDirectory;

  @Test
  void discoversPackagesFromMultipleRoots() throws IOException {
    Path defaultRoot = temporaryDirectory.resolve("default");
    Path additionalRoot = temporaryDirectory.resolve("additional");
    createPackage(defaultRoot, "default-package", "Default Package");
    createPackage(additionalRoot, "additional-package", "Additional Package");
    try (DefaultPackageManager packageManager = createPackageManager(defaultRoot, additionalRoot)) {
      PackageDiscoveryResult result = packageManager.refresh();

      assertEquals(List.of("default-package", "additional-package"), packageIds(result.packages()));
      assertTrue(result.diagnostics().isEmpty());
      assertEquals(
          additionalRoot.resolve("additional-package"),
          packageManager.findPackage("additional-package").orElseThrow().packageDirectory());
    }
  }

  @Test
  void rejectsAllPackagesWhenIdsAreDuplicated() throws IOException {
    Path defaultRoot = temporaryDirectory.resolve("default");
    Path additionalRoot = temporaryDirectory.resolve("additional");
    createPackage(defaultRoot, "first-directory", "Default Package", "duplicate");
    createPackage(additionalRoot, "second-directory", "Additional Package", "duplicate");
    try (DefaultPackageManager packageManager = createPackageManager(defaultRoot, additionalRoot)) {
      PackageDiscoveryResult result = packageManager.refresh();

      assertTrue(result.packages().isEmpty());
      assertTrue(packageManager.findPackage("duplicate").isEmpty());
      assertEquals(2, result.diagnostics().size());
      assertEquals(
          List.of(
              defaultRoot.resolve("first-directory"), additionalRoot.resolve("second-directory")),
          result.diagnostics().stream().map(PackageDiagnostic::packageDirectory).toList());
      assertTrue(
          result.diagnostics().stream()
              .allMatch(
                  diagnostic ->
                      diagnostic
                          .message()
                          .equals("Duplicate package ID; all packages with this ID were ignored")));
    }
  }

  @Test
  void refreshReloadsConfiguredPackageRoots() throws IOException {
    Path mqpDirectory = temporaryDirectory.resolve("myqolpackages");
    Path additionalRoot = temporaryDirectory.resolve("additional");
    createPackage(mqpDirectory, "default-package", "Default Package");
    createPackage(additionalRoot, "additional-package", "Additional Package");
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(mqpDirectory);
    try (DefaultPackageManager packageManager =
        new DefaultPackageManager(
            new ConfiguredPackageRootProvider(mqpDirectory, configManager),
            new FileSystemPackageDiscovery(),
            configManager,
            TEST_CONTEXT_FACTORY)) {
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
  }

  private DefaultPackageManager createPackageManager(Path... packageRoots) {
    PackageRootProvider rootProvider =
        () -> new PackageRootResolution(List.of(packageRoots), List.of());
    GsonMqpConfigManager configManager =
        new GsonMqpConfigManager(temporaryDirectory.resolve("config"));
    configManager.load();
    return new DefaultPackageManager(
        rootProvider, new FileSystemPackageDiscovery(), configManager, TEST_CONTEXT_FACTORY);
  }

  private static List<String> packageIds(List<PackageInfo> packages) {
    return packages.stream().map(PackageInfo::id).toList();
  }

  private static void createPackage(Path root, String id, String name) throws IOException {
    createPackage(root, id, name, null);
  }

  private static void createPackage(Path root, String directoryName, String name, String id)
      throws IOException {
    Path packageDirectory = root.resolve(directoryName);
    Files.createDirectories(packageDirectory.resolve("src"));
    String idField = id == null ? "" : "\"id\": \"%s\",%n".formatted(id);
    Files.writeString(
        packageDirectory.resolve("manifest.json"),
        """
        {
          %s
          "name": "%s",
          "description": "A test package.",
          "version": "1.0.0",
          "entrypoint": "src/index.js"
        }
        """
            .formatted(idField, name));
    Files.writeString(packageDirectory.resolve("src/index.js"), "");
  }

  private static String escapeJson(String value) {
    return value.replace("\\", "\\\\");
  }

  private static final class TestPackageScriptContext implements PackageScriptContext {
    private boolean closed;

    @Override
    public void invokeEnable() {
      ensureOpen();
    }

    @Override
    public void invokeDisable() {
      ensureOpen();
    }

    @Override
    public void close() {
      closed = true;
    }

    private void ensureOpen() {
      if (closed) {
        throw new IllegalStateException("Test package context is closed");
      }
    }
  }
}

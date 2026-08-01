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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.myqolpackages.api.packages.PackageDiscoveryResult;
import io.github.trethore.myqolpackages.api.packages.PackageOperationResult;
import io.github.trethore.myqolpackages.api.packages.PackageState;
import io.github.trethore.myqolpackages.internal.config.ConfiguredPackageRootProvider;
import io.github.trethore.myqolpackages.internal.config.GsonMqpConfigManager;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextFactory;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextSpec;
import io.github.trethore.myqolpackages.internal.runtime.PackageScriptContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultPackageManagerRuntimeTest {
  @TempDir Path temporaryDirectory;

  @Test
  void reloadEnablesConfiguredPackages() throws IOException {
    createPackage("example-package");
    writeEnabledPackages("example-package");
    RecordingContextFactory contextFactory = new RecordingContextFactory();
    try (DefaultPackageManager packageManager = createPackageManager(contextFactory)) {
      PackageDiscoveryResult result = packageManager.reload();

      assertTrue(result.diagnostics().isEmpty());
      assertEquals(
          PackageState.ENABLED,
          packageManager.findPackage("example-package").orElseThrow().state());
      assertEquals(List.of("enable:example-package"), contextFactory.events);
    }
  }

  @Test
  void refreshDoesNotRestartEnabledPackages() throws IOException {
    createPackage("example-package");
    writeEnabledPackages("example-package");
    RecordingContextFactory contextFactory = new RecordingContextFactory();
    try (DefaultPackageManager packageManager = createPackageManager(contextFactory)) {
      packageManager.reload();
      contextFactory.events.clear();

      packageManager.refresh();

      assertTrue(contextFactory.events.isEmpty());
      assertEquals(
          PackageState.ENABLED,
          packageManager.findPackage("example-package").orElseThrow().state());
    }
  }

  @Test
  void reloadDisablesAndReenablesConfiguredPackages() throws IOException {
    createPackage("first-package");
    createPackage("second-package");
    writeEnabledPackages("first-package", "second-package");
    RecordingContextFactory contextFactory = new RecordingContextFactory();
    try (DefaultPackageManager packageManager = createPackageManager(contextFactory)) {
      packageManager.reload();
      contextFactory.events.clear();

      packageManager.reload();

      assertEquals(
          List.of(
              "disable:second-package",
              "close:second-package",
              "disable:first-package",
              "close:first-package",
              "enable:first-package",
              "enable:second-package"),
          contextFactory.events);
    }
  }

  @Test
  void retainsMissingConfiguredPackageUntilExplicitlyDisabled() throws IOException {
    writeEnabledPackages("missing-package");
    RecordingContextFactory contextFactory = new RecordingContextFactory();
    try (DefaultPackageManager packageManager = createPackageManager(contextFactory)) {
      PackageDiscoveryResult result = packageManager.reload();

      assertFalse(result.diagnostics().isEmpty());
      assertEquals(List.of("missing-package"), packageManager.getConfiguredEnabledPackageIds());

      assertTrue(packageManager.disablePackage("missing-package").successful());
      assertTrue(packageManager.getConfiguredEnabledPackageIds().isEmpty());
    }
  }

  @Test
  void rejectsUnknownPackageDisable() {
    RecordingContextFactory contextFactory = new RecordingContextFactory();
    try (DefaultPackageManager packageManager = createPackageManager(contextFactory)) {
      packageManager.refresh();

      assertFalse(packageManager.disablePackage("unknown-package").successful());
      assertTrue(contextFactory.events.isEmpty());
    }
  }

  @Test
  void keepsMissingEnabledPackageRunningAfterRefresh() throws IOException {
    Path packageDirectory = createPackage("example-package");
    writeEnabledPackages("example-package");
    RecordingContextFactory contextFactory = new RecordingContextFactory();
    try (DefaultPackageManager packageManager = createPackageManager(contextFactory)) {
      packageManager.reload();
      deleteDirectory(packageDirectory);
      contextFactory.events.clear();

      PackageDiscoveryResult result = packageManager.refresh();

      assertFalse(result.diagnostics().isEmpty());
      assertEquals(
          PackageState.ENABLED,
          packageManager.findPackage("example-package").orElseThrow().state());
      assertTrue(contextFactory.events.isEmpty());

      packageManager.disablePackage("example-package");
      assertTrue(packageManager.findPackage("example-package").isEmpty());
      assertEquals(
          List.of("disable:example-package", "close:example-package"), contextFactory.events);
    }
  }

  @Test
  void explicitEnableAndDisableUpdateConfiguration() throws IOException {
    createPackage("example-package");
    RecordingContextFactory contextFactory = new RecordingContextFactory();
    try (DefaultPackageManager packageManager = createPackageManager(contextFactory)) {
      packageManager.refresh();

      assertTrue(packageManager.enablePackage("example-package").successful());
      assertEquals(List.of("example-package"), packageManager.getConfiguredEnabledPackageIds());

      assertTrue(packageManager.disablePackage("example-package").successful());
      assertTrue(packageManager.getConfiguredEnabledPackageIds().isEmpty());
      assertEquals(
          List.of("enable:example-package", "disable:example-package", "close:example-package"),
          contextFactory.events);
    }
  }

  @Test
  void rejectsRepeatedEnableAndDisableOperations() throws IOException {
    createPackage("example-package");
    RecordingContextFactory contextFactory = new RecordingContextFactory();
    try (DefaultPackageManager packageManager = createPackageManager(contextFactory)) {
      packageManager.refresh();

      assertTrue(packageManager.enablePackage("example-package").successful());
      PackageOperationResult repeatedEnable = packageManager.enablePackage("example-package");

      assertFalse(repeatedEnable.successful());
      assertEquals("Already enabled", repeatedEnable.diagnostics().getFirst().message());
      assertEquals(List.of("example-package"), packageManager.getConfiguredEnabledPackageIds());
      assertEquals(List.of("enable:example-package"), contextFactory.events);

      assertTrue(packageManager.disablePackage("example-package").successful());
      PackageOperationResult repeatedDisable = packageManager.disablePackage("example-package");

      assertFalse(repeatedDisable.successful());
      assertEquals("Already disabled", repeatedDisable.diagnostics().getFirst().message());
      assertTrue(packageManager.getConfiguredEnabledPackageIds().isEmpty());
      assertEquals(
          List.of("enable:example-package", "disable:example-package", "close:example-package"),
          contextFactory.events);
    }
  }

  @Test
  void explicitEnableRejectsPermissionsAboveUserGrant() throws IOException {
    createPermissionPackage();
    RecordingContextFactory contextFactory = new RecordingContextFactory();
    try (DefaultPackageManager packageManager = createPackageManager(contextFactory)) {
      packageManager.refresh();

      assertFalse(packageManager.enablePackage("example-package").successful());
      assertEquals(
          PackageState.DISABLED,
          packageManager.findPackage("example-package").orElseThrow().state());
      assertTrue(contextFactory.events.isEmpty());
    }
  }

  @Test
  void reloadChecksConfiguredPermissionGrants() throws IOException {
    createPermissionPackage();
    Files.writeString(
        temporaryDirectory.resolve("config.json"),
        """
        {
          "enabledPackages": ["example-package"],
          "permissions": {
            "packages": {
              "example-package": {
                "hostAccess": "full",
                "hostClassLookup": "minecraft",
                "filesystem": {
                  "read": "package",
                  "write": "data"
                }
              }
            }
          }
        }
        """);
    RecordingContextFactory contextFactory = new RecordingContextFactory();
    try (DefaultPackageManager packageManager = createPackageManager(contextFactory)) {
      PackageDiscoveryResult result = packageManager.reload();

      assertTrue(result.diagnostics().isEmpty());
      assertEquals(
          PackageState.ENABLED,
          packageManager.findPackage("example-package").orElseThrow().state());
      assertEquals(List.of("enable:example-package"), contextFactory.events);
    }
  }

  private DefaultPackageManager createPackageManager(PackageContextFactory contextFactory) {
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(temporaryDirectory);
    return new DefaultPackageManager(
        new ConfiguredPackageRootProvider(temporaryDirectory, configManager),
        new FileSystemPackageDiscovery(),
        configManager,
        contextFactory);
  }

  private Path createPackage(String packageId) throws IOException {
    return createPackage(packageId, false);
  }

  private Path createPackage(String packageId, boolean includePermissions) throws IOException {
    Path packageDirectory = temporaryDirectory.resolve(packageId);
    Files.createDirectories(packageDirectory.resolve("src"));
    String runtimeFields =
        includePermissions
            ? """
              "entrypoint": "src/index.js",
              "permissions": {
                "hostAccess": "full",
                "hostClassLookup": "minecraft",
                "filesystem": {
                  "read": "package",
                  "write": "data"
                }
              }
              """
            : """
              "entrypoint": "src/index.js"
              """;
    Files.writeString(
        packageDirectory.resolve("manifest.json"),
        """
        {
          "id": "%s",
          "name": "%s",
          "description": "A test package.",
          "version": "1.0.0",
          %s
        }
        """
            .formatted(packageId, packageId, runtimeFields));
    Files.writeString(packageDirectory.resolve("src/index.js"), "");
    return packageDirectory;
  }

  private void createPermissionPackage() throws IOException {
    createPackage("example-package", true);
  }

  private void writeEnabledPackages(String... packageIds) throws IOException {
    Files.createDirectories(temporaryDirectory);
    String enabledPackages =
        Stream.of(packageIds).map(id -> "\"" + id + "\"").collect(Collectors.joining(", "));
    Files.writeString(
        temporaryDirectory.resolve("config.json"),
        """
        {
          "additionalPackageRoots": [],
          "enabledPackages": [%s]
        }
        """
            .formatted(enabledPackages));
  }

  private void deleteDirectory(Path directory) throws IOException {
    try (Stream<Path> paths = Files.walk(directory)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.delete(path);
      }
    }
  }

  private static final class RecordingContextFactory implements PackageContextFactory {
    private final List<String> events = new ArrayList<>();

    @Override
    public PackageScriptContext create(PackageContextSpec spec) {
      return new PackageScriptContext() {
        @Override
        public void invokeEnable() {
          events.add("enable:" + spec.packageId());
        }

        @Override
        public void invokeDisable() {
          events.add("disable:" + spec.packageId());
        }

        @Override
        public void close() {
          events.add("close:" + spec.packageId());
        }
      };
    }
  }
}

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

import io.github.trethore.myqolpackages.api.config.FileSystemPermissionOverrides;
import io.github.trethore.myqolpackages.api.config.FileSystemReadPermission;
import io.github.trethore.myqolpackages.api.config.FileSystemWritePermission;
import io.github.trethore.myqolpackages.api.config.HostAccessPermission;
import io.github.trethore.myqolpackages.api.config.HostClassLookupPermission;
import io.github.trethore.myqolpackages.api.config.InternetAccessPermission;
import io.github.trethore.myqolpackages.api.config.MqpConfig;
import io.github.trethore.myqolpackages.api.config.PackagePermissionOverrides;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GsonMqpConfigManagerTest {
  @TempDir Path temporaryDirectory;

  @Test
  void createsDefaultConfiguration() throws IOException {
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(temporaryDirectory);

    MqpConfigLoadResult result = configManager.load();

    assertTrue(result.config().additionalPackageRoots().isEmpty());
    assertTrue(result.config().enabledPackages().isEmpty());
    assertTrue(result.diagnostics().isEmpty());
    assertTrue(Files.isRegularFile(temporaryDirectory.resolve("config.json")));
    assertTrue(
        Files.readString(temporaryDirectory.resolve("config.json"))
            .contains("\"additionalPackageRoots\""));
    assertTrue(
        Files.readString(temporaryDirectory.resolve("config.json"))
            .contains("\"enabledPackages\""));
  }

  @Test
  void keepsInvalidConfigurationAndUsesDefaults() throws IOException {
    Path configPath = temporaryDirectory.resolve("config.json");
    Files.writeString(configPath, "{");
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(temporaryDirectory);

    MqpConfigLoadResult result = configManager.load();

    assertTrue(result.config().additionalPackageRoots().isEmpty());
    assertTrue(result.config().enabledPackages().isEmpty());
    assertEquals(1, result.diagnostics().size());
    assertEquals("{", Files.readString(configPath));
  }

  @Test
  void treatsMissingAdditionalPackageRootsAsEmpty() throws IOException {
    Files.writeString(temporaryDirectory.resolve("config.json"), "{}");
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(temporaryDirectory);

    MqpConfigLoadResult result = configManager.load();

    assertTrue(result.config().additionalPackageRoots().isEmpty());
    assertTrue(result.config().enabledPackages().isEmpty());
    assertTrue(result.diagnostics().isEmpty());
  }

  @Test
  void savesEnabledPackages() throws IOException {
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(temporaryDirectory);
    configManager.load();

    configManager.addEnabledPackage("first-package");
    configManager.addEnabledPackage("second-package");
    configManager.removeEnabledPackage("first-package");

    GsonMqpConfigManager reloadedManager = new GsonMqpConfigManager(temporaryDirectory);
    MqpConfigLoadResult result = reloadedManager.load();
    assertEquals(List.of("second-package"), result.config().enabledPackages());
  }

  @Test
  void loadsAndPreservesPermissionConfiguration() throws IOException {
    Files.writeString(
        temporaryDirectory.resolve("config.json"),
        """
        {
          "permissions": {
            "defaults": {
              "filesystem": {
                "read": "package"
              },
              "internet": {
                "access": "domains",
                "domains": ["api.example.com"]
              }
            },
            "packages": {
              "example-package": {
                "hostAccess": "full",
                "hostClassLookup": "minecraft",
                "filesystem": {
                  "write": "data"
                }
              }
            }
          }
        }
        """);
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(temporaryDirectory);

    MqpConfigLoadResult result = configManager.load();
    configManager.addEnabledPackage("example-package");

    assertEquals(
        FileSystemReadPermission.PACKAGE,
        result.config().permissions().defaults().filesystem().read());
    assertEquals(
        InternetAccessPermission.DOMAINS,
        result.config().permissions().defaults().internet().access());
    assertEquals(
        List.of("api.example.com"), result.config().permissions().defaults().internet().domains());
    assertEquals(
        HostAccessPermission.FULL,
        result.config().permissions().packages().get("example-package").hostAccess());
    assertEquals(
        HostClassLookupPermission.MINECRAFT,
        result.config().permissions().packages().get("example-package").hostClassLookup());
    assertEquals(
        FileSystemWritePermission.DATA,
        result.config().permissions().packages().get("example-package").filesystem().write());
    assertTrue(Files.readString(temporaryDirectory.resolve("config.json")).contains("permissions"));
  }

  @Test
  void savesGlobalPermissionsAndPreservesPackageOverrides() throws IOException {
    Files.writeString(
        temporaryDirectory.resolve("config.json"),
        """
        {
          "additionalPackageRoots": ["/example/root"],
          "enabledPackages": ["example-package"],
          "permissions": {
            "packages": {
              "example-package": {
                "hostAccess": "full"
              }
            }
          }
        }
        """);
    MqpConfigLoadResult result =
        saveGlobalPermissionsAndReload(
            new PackagePermissionOverrides(
                HostAccessPermission.FULL,
                HostClassLookupPermission.MINECRAFT,
                new FileSystemPermissionOverrides(
                    FileSystemReadPermission.PACKAGE, FileSystemWritePermission.DATA)));

    MqpConfig config = result.config();
    PackagePermissionOverrides defaultPermissions = config.permissions().defaults();
    FileSystemPermissionOverrides defaultFilesystem = defaultPermissions.filesystem();
    assertEquals(List.of("/example/root"), config.additionalPackageRoots());
    assertEquals(List.of("example-package"), config.enabledPackages());
    assertEquals(HostAccessPermission.FULL, defaultPermissions.hostAccess());
    assertEquals(HostClassLookupPermission.MINECRAFT, defaultPermissions.hostClassLookup());
    assertEquals(FileSystemReadPermission.PACKAGE, defaultFilesystem.read());
    assertEquals(FileSystemWritePermission.DATA, defaultFilesystem.write());
    assertEquals(
        HostAccessPermission.FULL,
        config.permissions().packages().get("example-package").hostAccess());
  }

  @Test
  void rejectsUnknownPermissionValue() throws IOException {
    Files.writeString(
        temporaryDirectory.resolve("config.json"),
        """
        {
          "permissions": {
            "defaults": {
              "hostAccess": "trusted"
            }
          }
        }
        """);
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(temporaryDirectory);

    MqpConfigLoadResult result = configManager.load();

    assertEquals(1, result.diagnostics().size());
    assertTrue(result.diagnostics().getFirst().message().contains("trusted"));
  }

  private MqpConfigLoadResult saveGlobalPermissionsAndReload(PackagePermissionOverrides permissions)
      throws IOException {
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(temporaryDirectory);
    configManager.load();
    configManager.setGlobalPermissions(permissions);
    return new GsonMqpConfigManager(temporaryDirectory).load();
  }
}

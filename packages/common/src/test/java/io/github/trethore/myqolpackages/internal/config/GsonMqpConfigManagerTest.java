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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    String config = Files.readString(temporaryDirectory.resolve("config.json"));
    assertTrue(config.contains("\"additionalPackageRoots\""));
    assertTrue(config.contains("\"enabledPackages\""));
    assertFalse(config.contains("permissions"));
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
  void treatsMissingFieldsAsEmpty() throws IOException {
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

    MqpConfigLoadResult result = new GsonMqpConfigManager(temporaryDirectory).load();
    assertEquals(List.of("second-package"), result.config().enabledPackages());
  }

  @Test
  void ignoresAndRemovesLegacyPermissionConfiguration() throws IOException {
    Path configPath = temporaryDirectory.resolve("config.json");
    Files.writeString(
        configPath,
        """
        {
          "additionalPackageRoots": ["/example/root"],
          "enabledPackages": ["example-package"],
          "permissions": {
            "defaults": { "hostAccess": "full" },
            "packages": { "example-package": { "internet": { "access": "full" } } }
          }
        }
        """);
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(temporaryDirectory);

    MqpConfigLoadResult result = configManager.load();
    configManager.addEnabledPackage("second-package");

    assertEquals(List.of("/example/root"), result.config().additionalPackageRoots());
    assertEquals(List.of("example-package"), result.config().enabledPackages());
    assertTrue(result.diagnostics().isEmpty());
    assertFalse(Files.readString(configPath).contains("permissions"));
  }
}

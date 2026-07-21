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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GsonMqpConfigManagerTest {
  @TempDir Path temporaryDirectory;

  @Test
  void createsDefaultConfiguration() throws IOException {
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(temporaryDirectory);

    MqpConfigLoadResult result = configManager.load();

    assertTrue(result.config().additionalPackageRoots().isEmpty());
    assertTrue(result.diagnostics().isEmpty());
    assertTrue(Files.isRegularFile(temporaryDirectory.resolve("config.json")));
    assertTrue(
        Files.readString(temporaryDirectory.resolve("config.json"))
            .contains("\"additionalPackageRoots\""));
  }

  @Test
  void keepsInvalidConfigurationAndUsesDefaults() throws IOException {
    Path configPath = temporaryDirectory.resolve("config.json");
    Files.writeString(configPath, "{");
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(temporaryDirectory);

    MqpConfigLoadResult result = configManager.load();

    assertTrue(result.config().additionalPackageRoots().isEmpty());
    assertEquals(1, result.diagnostics().size());
    assertEquals("{", Files.readString(configPath));
  }

  @Test
  void treatsMissingAdditionalPackageRootsAsEmpty() throws IOException {
    Files.writeString(temporaryDirectory.resolve("config.json"), "{}");
    GsonMqpConfigManager configManager = new GsonMqpConfigManager(temporaryDirectory);

    MqpConfigLoadResult result = configManager.load();

    assertTrue(result.config().additionalPackageRoots().isEmpty());
    assertTrue(result.diagnostics().isEmpty());
  }
}

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemPackageDiscoveryTest {
  @TempDir Path temporaryDirectory;

  @Test
  void discoversValidPackage() throws IOException {
    createPackage(
        "example-package",
        """
        {
          "name": "Example Package",
          "description": "An example package.",
          "version": "1.0.0",
          "entrypoint": "src/index.js"
        }
        """);

    PackageDiscoverySnapshot result = new FileSystemPackageDiscovery().discover(temporaryDirectory);

    assertEquals(1, result.packages().size());
    assertTrue(result.diagnostics().isEmpty());
    PackageDescriptor descriptor = result.packages().getFirst();
    assertEquals("example-package", descriptor.id());
    assertEquals("Example Package", descriptor.manifest().name());
    assertEquals("src/index.js", descriptor.manifest().entrypoint());
  }

  @Test
  void continuesAfterInvalidPackage() throws IOException {
    createPackage(
        "valid-package",
        """
        {
          "name": "Valid Package",
          "description": "A valid package.",
          "version": "1.0.0",
          "entrypoint": "src/index.js"
        }
        """);
    Files.createDirectories(temporaryDirectory.resolve("missing-manifest"));

    PackageDiscoverySnapshot result = new FileSystemPackageDiscovery().discover(temporaryDirectory);

    assertEquals(1, result.packages().size());
    assertEquals(1, result.diagnostics().size());
    assertEquals("missing-manifest", result.diagnostics().getFirst().packageId());
    assertEquals("Missing manifest.json", result.diagnostics().getFirst().message());
  }

  @Test
  void reportsInvalidJson() throws IOException {
    Path packageDirectory = temporaryDirectory.resolve("invalid-json");
    Files.createDirectories(packageDirectory);
    Files.writeString(packageDirectory.resolve("manifest.json"), "{");

    PackageDiscoverySnapshot result = new FileSystemPackageDiscovery().discover(temporaryDirectory);

    assertTrue(result.packages().isEmpty());
    assertEquals(1, result.diagnostics().size());
    assertTrue(result.diagnostics().getFirst().message().startsWith("Invalid manifest.json:"));
  }

  @Test
  void reportsMissingManifestField() throws IOException {
    createPackage(
        "missing-version",
        """
        {
          "name": "Missing Version",
          "description": "A package without a version.",
          "entrypoint": "src/index.js"
        }
        """);

    PackageDiscoverySnapshot result = new FileSystemPackageDiscovery().discover(temporaryDirectory);

    assertTrue(result.packages().isEmpty());
    assertEquals(
        "Missing or empty manifest field: version", result.diagnostics().getFirst().message());
  }

  @Test
  void rejectsEntrypointOutsidePackageDirectory() throws IOException {
    Files.writeString(temporaryDirectory.resolve("outside.js"), "");
    Path packageDirectory = temporaryDirectory.resolve("escaping-entrypoint");
    Files.createDirectories(packageDirectory);
    Files.writeString(
        packageDirectory.resolve("manifest.json"),
        """
        {
          "name": "Escaping Entrypoint",
          "description": "A package with an invalid entrypoint.",
          "version": "1.0.0",
          "entrypoint": "../outside.js"
        }
        """);

    PackageDiscoverySnapshot result = new FileSystemPackageDiscovery().discover(temporaryDirectory);

    assertTrue(result.packages().isEmpty());
    assertEquals(
        "Entrypoint must be inside the package directory",
        result.diagnostics().getFirst().message());
  }

  private void createPackage(String id, String manifest) throws IOException {
    Path packageDirectory = temporaryDirectory.resolve(id);
    Files.createDirectories(packageDirectory.resolve("src"));
    Files.writeString(packageDirectory.resolve("manifest.json"), manifest);
    Files.writeString(packageDirectory.resolve("src/index.js"), "");
  }
}

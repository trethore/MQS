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
package io.github.trethore.myqolpackages.internal.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GraalPackageContextFactoryTest {
  @TempDir Path temporaryDirectory;

  @Test
  void invokesRequiredLifecycleExports() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            let enabled = false;

            export function onEnable() {
              if (enabled) throw new Error("already enabled");
              enabled = true;
            }

            export function onDisable() {
              if (!enabled) throw new Error("not enabled");
              enabled = false;
            }
            """);
    try (GraalPackageContextFactory contextFactory = new GraalPackageContextFactory()) {
      try (PackageScriptContext context = contextFactory.create("example-package", entrypoint)) {
        assertDoesNotThrow(context::invokeEnable);
        assertDoesNotThrow(context::invokeDisable);
      }
    }
  }

  @Test
  void rejectsMissingLifecycleExport() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            export function onEnable() {}
            """);

    PackageLifecycleException exception;
    try (GraalPackageContextFactory contextFactory = new GraalPackageContextFactory()) {
      exception =
          assertThrows(
              PackageLifecycleException.class,
              () -> contextFactory.create("example-package", entrypoint));
    }

    assertTrue(exception.getMessage().contains("onDisable"));
  }

  @Test
  void rejectsAsynchronousLifecycleHook() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            export async function onEnable() {}
            export function onDisable() {}
            """);
    try (GraalPackageContextFactory contextFactory = new GraalPackageContextFactory()) {
      try (PackageScriptContext context = contextFactory.create("example-package", entrypoint)) {
        PackageLifecycleException exception =
            assertThrows(PackageLifecycleException.class, context::invokeEnable);

        assertTrue(exception.getMessage().contains("asynchronous lifecycle hooks"));
      }
    }
  }

  @Test
  void rejectsContextCreationAfterClose() throws PackageLifecycleException {
    GraalPackageContextFactory contextFactory = new GraalPackageContextFactory();
    contextFactory.close();

    PackageLifecycleException exception =
        assertThrows(
            PackageLifecycleException.class,
            () ->
                contextFactory.create(
                    "example-package", temporaryDirectory.resolve("src/index.js")));

    assertTrue(exception.getMessage().contains("already closed"));
  }

  private Path createEntrypoint(String source) throws IOException {
    Path packageDirectory = temporaryDirectory.resolve("example-package");
    Path entrypoint = packageDirectory.resolve("src/index.js");
    Files.createDirectories(entrypoint.getParent());
    Files.writeString(entrypoint, source);
    return entrypoint;
  }
}

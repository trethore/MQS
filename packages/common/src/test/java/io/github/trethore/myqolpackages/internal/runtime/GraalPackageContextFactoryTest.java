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

import io.github.trethore.myqolpackages.api.config.FileSystemPermissions;
import io.github.trethore.myqolpackages.api.config.FileSystemReadPermission;
import io.github.trethore.myqolpackages.api.config.FileSystemWritePermission;
import io.github.trethore.myqolpackages.api.config.HostAccessPermission;
import io.github.trethore.myqolpackages.api.config.HostClassLookupPermission;
import io.github.trethore.myqolpackages.api.config.PackagePermissions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    try (GraalPackageContextFactory contextFactory = createContextFactory()) {
      try (PackageScriptContext context = contextFactory.create(createSpec(entrypoint))) {
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
    try (GraalPackageContextFactory contextFactory = createContextFactory()) {
      exception =
          assertThrows(
              PackageLifecycleException.class, () -> contextFactory.create(createSpec(entrypoint)));
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
    try (GraalPackageContextFactory contextFactory = createContextFactory()) {
      try (PackageScriptContext context = contextFactory.create(createSpec(entrypoint))) {
        PackageLifecycleException exception =
            assertThrows(PackageLifecycleException.class, context::invokeEnable);

        assertTrue(exception.getMessage().contains("asynchronous lifecycle hooks"));
      }
    }
  }

  @Test
  void rejectsContextCreationAfterClose() throws PackageLifecycleException {
    GraalPackageContextFactory contextFactory = createContextFactory();
    contextFactory.close();

    PackageLifecycleException exception =
        assertThrows(
            PackageLifecycleException.class,
            () -> contextFactory.create(createSpec(temporaryDirectory.resolve("src/index.js"))));

    assertTrue(exception.getMessage().contains("already closed"));
  }

  @Test
  void exposesImmutableMqpMetadata() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            if (mqp.version !== "0.0.1") throw new Error("unexpected MQP version");
            if (mqp.package.id !== "example-package") throw new Error("unexpected package ID");
            if (mqp.permissions.hostAccess !== "none") throw new Error("unexpected host access");
            if (mqp.permissions.filesystem.read !== "none") throw new Error("unexpected read access");
            if (mqp.permissions.has("hostAccess.full")) throw new Error("unexpected full access");
            try { mqp.version = "changed"; } catch (error) {}
            if (mqp.version !== "0.0.1") throw new Error("mutable MQP API");

            export function onEnable() {}
            export function onDisable() {}
            """);

    try (GraalPackageContextFactory contextFactory = createContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint))) {
      assertDoesNotThrow(context::invokeEnable);
    }
  }

  @Test
  void loadsModulesWithPackageReadPermission() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            import { value } from "./value.js";
            if (value !== 42) throw new Error("module was not loaded");

            export function onEnable() {}
            export function onDisable() {}
            """);
    Files.writeString(entrypoint.resolveSibling("value.js"), "export const value = 42;");
    PackagePermissions permissions =
        new PackagePermissions(
            HostAccessPermission.NONE,
            HostClassLookupPermission.NONE,
            new FileSystemPermissions(
                FileSystemReadPermission.PACKAGE, FileSystemWritePermission.NONE));

    try (GraalPackageContextFactory contextFactory = createContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint, permissions))) {
      assertDoesNotThrow(context::invokeEnable);
    }
  }

  @Test
  void rejectsModuleLoadingWithoutReadPermission() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            import "./value.js";
            export function onEnable() {}
            export function onDisable() {}
            """);
    Files.writeString(entrypoint.resolveSibling("value.js"), "export const value = 42;");

    try (GraalPackageContextFactory contextFactory = createContextFactory()) {
      assertThrows(
          PackageLifecycleException.class, () -> contextFactory.create(createSpec(entrypoint)));
    }
  }

  @Test
  void permitsAllHostClassLookupWhenGranted() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            const HostString = Java.type("java.lang.String");
            if (HostString.valueOf(42) !== "42") throw new Error("host lookup failed");

            export function onEnable() {}
            export function onDisable() {}
            """);
    PackagePermissions permissions =
        new PackagePermissions(
            HostAccessPermission.FULL, HostClassLookupPermission.ALL, FileSystemPermissions.none());

    try (GraalPackageContextFactory contextFactory = createContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint, permissions))) {
      assertDoesNotThrow(context::invokeEnable);
    }
  }

  @Test
  void minecraftHostClassLookupRejectsJavaClasses() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            Java.type("java.lang.String");
            export function onEnable() {}
            export function onDisable() {}
            """);
    PackagePermissions permissions =
        new PackagePermissions(
            HostAccessPermission.FULL,
            HostClassLookupPermission.MINECRAFT,
            FileSystemPermissions.none());

    try (GraalPackageContextFactory contextFactory = createContextFactory()) {
      assertThrows(
          PackageLifecycleException.class,
          () -> contextFactory.create(createSpec(entrypoint, permissions)));
    }
  }

  @Test
  void createsContextWithDataOnlyWritePermission() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            if (!mqp.permissions.has("filesystem.read.data")) throw new Error("data is not readable");
            if (!mqp.permissions.has("filesystem.write.data")) throw new Error("data is not writable");

            export function onEnable() {}
            export function onDisable() {}
            """);
    PackagePermissions permissions =
        new PackagePermissions(
            HostAccessPermission.NONE,
            HostClassLookupPermission.NONE,
            new FileSystemPermissions(
                FileSystemReadPermission.NONE, FileSystemWritePermission.DATA));

    try (GraalPackageContextFactory contextFactory = createContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint, permissions))) {
      assertDoesNotThrow(context::invokeEnable);
      assertTrue(Files.isDirectory(temporaryDirectory.resolve(".data/example-package")));
    }
  }

  private Path createEntrypoint(String source) throws IOException {
    Path packageDirectory = temporaryDirectory.resolve("example-package");
    Path entrypoint = packageDirectory.resolve("src/index.js");
    Files.createDirectories(entrypoint.getParent());
    Files.writeString(entrypoint, source);
    return entrypoint;
  }

  private GraalPackageContextFactory createContextFactory() {
    return new GraalPackageContextFactory(temporaryDirectory, "0.0.1");
  }

  private PackageContextSpec createSpec(Path entrypoint) {
    return createSpec(entrypoint, PackagePermissions.none());
  }

  private PackageContextSpec createSpec(Path entrypoint, PackagePermissions permissions) {
    return new PackageContextSpec(
        "example-package",
        entrypoint.getParent().getParent(),
        entrypoint,
        permissions,
        List.of(temporaryDirectory));
  }
}

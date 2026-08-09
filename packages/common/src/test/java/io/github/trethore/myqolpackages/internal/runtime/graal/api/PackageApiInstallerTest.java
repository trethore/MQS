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
package io.github.trethore.myqolpackages.internal.runtime.graal.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.trethore.myqolpackages.internal.runtime.PackageContextSpec;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackageApiInstallerTest {
  @TempDir Path temporaryDirectory;

  @Test
  void installsTicksAndClosesModulesInLifecycleOrder() {
    List<String> events = new ArrayList<>();
    PackageApiInstaller installer =
        new PackageApiInstaller(
            List.of(recordingModule("first", events), recordingModule("second", events)));

    try (Context context = createContext();
        PackageApiSession session = installer.install(context, createSpec())) {
      session.tick();
    }

    assertEquals(
        List.of(
            "install:first",
            "install:second",
            "tick:first",
            "tick:second",
            "close:second",
            "close:first"),
        events);
  }

  @Test
  void closesInstalledModulesWhenInstallationFails() {
    List<String> events = new ArrayList<>();
    PackageApiModule failingModule =
        (bridge, spec) -> {
          events.add("install:failing");
          throw new IllegalStateException("failed");
        };
    PackageApiInstaller installer =
        new PackageApiInstaller(List.of(recordingModule("first", events), failingModule));
    PackageContextSpec spec = createSpec();

    try (Context context = createContext()) {
      assertThrows(IllegalStateException.class, () -> installer.install(context, spec));
    }

    assertEquals(List.of("install:first", "install:failing", "close:first"), events);
  }

  private PackageContextSpec createSpec() {
    return new PackageContextSpec(
        "test-package",
        temporaryDirectory,
        temporaryDirectory.resolve("index.js"),
        temporaryDirectory.resolve("data"));
  }

  private static Context createContext() {
    return Context.newBuilder("js").option("js.esm-eval-returns-exports", "true").build();
  }

  private static PackageApiModule recordingModule(String name, List<String> events) {
    return (bridge, spec) -> {
      events.add("install:" + name);
      return new PackageApiSession() {
        @Override
        public void tick() {
          events.add("tick:" + name);
        }

        @Override
        public void close() {
          events.add("close:" + name);
        }
      };
    };
  }
}

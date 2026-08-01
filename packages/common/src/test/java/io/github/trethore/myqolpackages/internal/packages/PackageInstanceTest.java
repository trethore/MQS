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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.myqolpackages.api.config.PackagePermissions;
import io.github.trethore.myqolpackages.api.packages.PackageState;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextFactory;
import io.github.trethore.myqolpackages.internal.runtime.PackageLifecycleException;
import io.github.trethore.myqolpackages.internal.runtime.PackageScriptContext;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackageInstanceTest {
  @TempDir Path temporaryDirectory;

  @Test
  void closesContextWhenEnableFails() {
    try (RecordingScriptContext scriptContext = new RecordingScriptContext()) {
      scriptContext.failEnable = true;
      PackageInstance packageInstance = createInstance(spec -> scriptContext);

      assertThrows(
          PackageLifecycleException.class,
          () -> packageInstance.enable(PackagePermissions.none(), List.of(temporaryDirectory)));

      assertEquals(PackageState.ERROR, packageInstance.getState());
      assertTrue(scriptContext.closed);
    }
  }

  @Test
  void closesContextWhenDisableFails() throws PackageLifecycleException {
    try (RecordingScriptContext scriptContext = new RecordingScriptContext()) {
      scriptContext.failDisable = true;
      PackageInstance packageInstance = createInstance(spec -> scriptContext);
      packageInstance.enable(PackagePermissions.none(), List.of(temporaryDirectory));

      assertThrows(PackageLifecycleException.class, packageInstance::disable);

      assertEquals(PackageState.DISABLED, packageInstance.getState());
      assertTrue(scriptContext.closed);
    }
  }

  private PackageInstance createInstance(PackageContextFactory contextFactory) {
    Path packageDirectory = temporaryDirectory.resolve("example-package");
    PackageManifest manifest =
        new PackageManifest(
            "example-package",
            "Example Package",
            "A test package.",
            "1.0.0",
            "src/index.js",
            PackagePermissions.none());
    PackageDescriptor descriptor =
        new PackageDescriptor(
            "example-package",
            packageDirectory,
            packageDirectory.resolve("src/index.js"),
            manifest);
    return new PackageInstance(descriptor, contextFactory);
  }

  private static final class RecordingScriptContext implements PackageScriptContext {
    private boolean closed;
    private boolean failDisable;
    private boolean failEnable;

    @Override
    public void invokeEnable() throws PackageLifecycleException {
      if (failEnable) {
        throw new PackageLifecycleException("enable failed");
      }
    }

    @Override
    public void invokeDisable() throws PackageLifecycleException {
      if (failDisable) {
        throw new PackageLifecycleException("disable failed");
      }
    }

    @Override
    public void close() {
      closed = true;
    }
  }
}

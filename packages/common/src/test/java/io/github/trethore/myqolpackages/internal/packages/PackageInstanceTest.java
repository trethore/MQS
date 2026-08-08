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

import io.github.trethore.myqolpackages.api.packages.PackageState;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextFactory;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextSpec;
import io.github.trethore.myqolpackages.internal.runtime.PackageLifecycleException;
import io.github.trethore.myqolpackages.internal.runtime.PackageScriptContext;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackageInstanceTest {
  @TempDir Path temporaryDirectory;

  @Test
  void closesContextWhenEnableFails() {
    try (RecordingScriptContext scriptContext = new RecordingScriptContext()) {
      scriptContext.failEnable = true;
      PackageInstance packageInstance = createInstance(spec -> scriptContext);

      assertThrows(PackageLifecycleException.class, packageInstance::enable);

      assertEquals(PackageState.ERROR, packageInstance.getState());
      assertTrue(scriptContext.closed);
    }
  }

  @Test
  void closesContextWhenDisableFails() throws PackageLifecycleException {
    try (RecordingScriptContext scriptContext = new RecordingScriptContext()) {
      scriptContext.failDisable = true;
      PackageInstance packageInstance = createInstance(spec -> scriptContext);
      packageInstance.enable();

      assertThrows(PackageLifecycleException.class, packageInstance::disable);

      assertEquals(PackageState.DISABLED, packageInstance.getState());
      assertTrue(scriptContext.closed);
    }
  }

  @Test
  void resolvesDataDirectoryUnderOwningPackageRoot() throws PackageLifecycleException {
    Path additionalRoot = temporaryDirectory.resolve("additional-root");
    RecordingContextFactory contextFactory = new RecordingContextFactory();
    PackageInstance packageInstance = createInstance(contextFactory, additionalRoot);

    packageInstance.enable();
    packageInstance.disable();

    assertEquals(
        additionalRoot.resolve(".package-data/example-package").toAbsolutePath().normalize(),
        contextFactory.dataDirectory);
  }

  private PackageInstance createInstance(PackageContextFactory contextFactory) {
    return createInstance(contextFactory, temporaryDirectory);
  }

  private PackageInstance createInstance(PackageContextFactory contextFactory, Path packageRoot) {
    Path packageDirectory = packageRoot.resolve("example-package");
    PackageManifest manifest =
        new PackageManifest(
            "example-package", "Example Package", "A test package.", "1.0.0", "src/index.js");
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
    public void tick() {
      if (closed) {
        throw new IllegalStateException("Script context is closed");
      }
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  private static final class RecordingContextFactory implements PackageContextFactory {
    private Path dataDirectory;

    @Override
    public PackageScriptContext create(PackageContextSpec spec) {
      dataDirectory = spec.dataDirectory();
      return new RecordingScriptContext();
    }
  }
}

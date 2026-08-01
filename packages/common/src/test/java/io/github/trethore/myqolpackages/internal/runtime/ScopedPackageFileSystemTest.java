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
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScopedPackageFileSystemTest {
  @TempDir Path temporaryDirectory;

  @Test
  void enforcesReadAndWriteRoots() throws IOException {
    Path packageDirectory = temporaryDirectory.resolve("package");
    Path dataDirectory = temporaryDirectory.resolve("data");
    Path outsideDirectory = temporaryDirectory.resolve("outside");
    Files.createDirectories(packageDirectory);
    Files.createDirectories(dataDirectory);
    Files.createDirectories(outsideDirectory);
    Path packageFile = Files.writeString(packageDirectory.resolve("index.js"), "source");
    Path outsideFile = Files.writeString(outsideDirectory.resolve("secret.txt"), "secret");
    ScopedPackageFileSystem fileSystem =
        new ScopedPackageFileSystem(List.of(packageDirectory), List.of(dataDirectory), false);

    assertDoesNotThrow(() -> openChannel(fileSystem, packageFile, Set.of(StandardOpenOption.READ)));
    assertThrows(
        AccessDeniedException.class,
        () -> openChannel(fileSystem, packageFile, Set.of(StandardOpenOption.WRITE)));
    assertDoesNotThrow(
        () ->
            openChannel(
                fileSystem,
                dataDirectory.resolve("state.json"),
                Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE)));
    assertThrows(
        AccessDeniedException.class,
        () -> openChannel(fileSystem, outsideFile, Set.of(StandardOpenOption.READ)));
  }

  @Test
  void rejectsSymlinkEscapes() throws IOException {
    Path packageDirectory = temporaryDirectory.resolve("package");
    Path outsideDirectory = temporaryDirectory.resolve("outside");
    Files.createDirectories(packageDirectory);
    Files.createDirectories(outsideDirectory);
    Path outsideFile = Files.writeString(outsideDirectory.resolve("secret.txt"), "secret");
    Path link = packageDirectory.resolve("secret-link.txt");
    Files.createSymbolicLink(link, outsideFile);
    ScopedPackageFileSystem fileSystem =
        new ScopedPackageFileSystem(List.of(packageDirectory), List.of(), false);

    assertThrows(
        AccessDeniedException.class,
        () -> openChannel(fileSystem, link, Set.of(StandardOpenOption.READ)));
  }

  private static void openChannel(
      ScopedPackageFileSystem fileSystem, Path path, Set<? extends OpenOption> options)
      throws IOException {
    try (SeekableByteChannel channel = fileSystem.newByteChannel(path, options)) {
      assertTrue(channel.isOpen());
    }
  }
}

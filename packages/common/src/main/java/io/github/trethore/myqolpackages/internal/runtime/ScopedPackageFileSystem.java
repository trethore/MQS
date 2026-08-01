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

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.graalvm.polyglot.io.FileSystem;

final class ScopedPackageFileSystem implements FileSystem {
  private final FileSystem delegate = FileSystem.newDefaultFileSystem();
  private final List<Path> readableRoots;
  private final List<Path> writeRoots;
  private final boolean readAll;

  ScopedPackageFileSystem(List<Path> readRoots, List<Path> writeRoots, boolean readAll)
      throws IOException {
    this.writeRoots = canonicalizeRoots(writeRoots);
    readableRoots = combineRoots(canonicalizeRoots(readRoots), this.writeRoots);
    this.readAll = readAll;
  }

  @Override
  public Path parsePath(URI uri) {
    return delegate.parsePath(uri);
  }

  @Override
  public Path parsePath(String path) {
    return delegate.parsePath(path);
  }

  @Override
  public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions)
      throws IOException {
    boolean write = modes.contains(AccessMode.WRITE);
    authorize(path, write);
    delegate.checkAccess(path, modes, linkOptions);
  }

  @Override
  public void createDirectory(Path directory, FileAttribute<?>... attributes) throws IOException {
    authorize(directory, true);
    delegate.createDirectory(directory, attributes);
  }

  @Override
  public void delete(Path path) throws IOException {
    authorize(path, true);
    delegate.delete(path);
  }

  @Override
  public SeekableByteChannel newByteChannel(
      Path path, Set<? extends OpenOption> options, FileAttribute<?>... attributes)
      throws IOException {
    authorize(path, isWriteOperation(options));
    return delegate.newByteChannel(path, options, attributes);
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream(
      Path directory, DirectoryStream.Filter<? super Path> filter) throws IOException {
    authorize(directory, false);
    return delegate.newDirectoryStream(directory, filter);
  }

  @Override
  public Path toAbsolutePath(Path path) {
    return delegate.toAbsolutePath(path);
  }

  @Override
  public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
    authorize(path, false);
    Path realPath = delegate.toRealPath(path, linkOptions);
    authorize(realPath, false);
    return realPath;
  }

  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options)
      throws IOException {
    authorize(path, false);
    return delegate.readAttributes(path, attributes, options);
  }

  @Override
  public void setAttribute(Path path, String attribute, Object value, LinkOption... options)
      throws IOException {
    authorize(path, true);
    delegate.setAttribute(path, attribute, value, options);
  }

  @Override
  public void copy(Path source, Path target, CopyOption... options) throws IOException {
    authorize(source, false);
    authorize(target, true);
    delegate.copy(source, target, options);
  }

  @Override
  public void move(Path source, Path target, CopyOption... options) throws IOException {
    authorize(source, true);
    authorize(target, true);
    delegate.move(source, target, options);
  }

  @Override
  public void createLink(Path link, Path existing) throws IOException {
    throw new AccessDeniedException(link.toString(), existing.toString(), "Hard links are denied");
  }

  @Override
  public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attributes)
      throws IOException {
    throw new AccessDeniedException(
        link.toString(), target.toString(), "Symbolic links are denied");
  }

  @Override
  public Path readSymbolicLink(Path link) throws IOException {
    authorize(link, false);
    return delegate.readSymbolicLink(link);
  }

  @Override
  public void setCurrentWorkingDirectory(Path currentWorkingDirectory) {
    try {
      authorize(currentWorkingDirectory, false);
    } catch (IOException exception) {
      throw new IllegalArgumentException(exception.getMessage(), exception);
    }
    delegate.setCurrentWorkingDirectory(currentWorkingDirectory);
  }

  @Override
  public Path getTempDirectory() {
    return delegate.getTempDirectory();
  }

  @Override
  public boolean isSameFile(Path firstPath, Path secondPath, LinkOption... options)
      throws IOException {
    authorize(firstPath, false);
    authorize(secondPath, false);
    return delegate.isSameFile(firstPath, secondPath, options);
  }

  private static List<Path> canonicalizeRoots(List<Path> roots) throws IOException {
    Set<Path> canonicalRoots = new LinkedHashSet<>();
    for (Path root : roots) {
      canonicalRoots.add(root.toRealPath());
    }
    return List.copyOf(canonicalRoots);
  }

  private static boolean isWriteOperation(Set<? extends OpenOption> options) {
    return options.contains(StandardOpenOption.WRITE)
        || options.contains(StandardOpenOption.APPEND)
        || options.contains(StandardOpenOption.CREATE)
        || options.contains(StandardOpenOption.CREATE_NEW)
        || options.contains(StandardOpenOption.TRUNCATE_EXISTING)
        || options.contains(StandardOpenOption.DELETE_ON_CLOSE);
  }

  private void authorize(Path path, boolean write) throws IOException {
    if (!write && readAll) {
      return;
    }
    Path resolvedPath = resolveForAuthorization(path);
    List<Path> allowedRoots = write ? writeRoots : readableRoots;
    if (allowedRoots.stream().noneMatch(resolvedPath::startsWith)) {
      throw new AccessDeniedException(path.toString());
    }
  }

  private static List<Path> combineRoots(List<Path> readRoots, List<Path> writeRoots) {
    List<Path> roots = new ArrayList<>(readRoots);
    for (Path writeRoot : writeRoots) {
      if (!roots.contains(writeRoot)) {
        roots.add(writeRoot);
      }
    }
    return List.copyOf(roots);
  }

  private Path resolveForAuthorization(Path path) throws IOException {
    Path absolutePath = delegate.toAbsolutePath(path).normalize();
    Path existingPath = absolutePath;
    while (existingPath != null && Files.notExists(existingPath, LinkOption.NOFOLLOW_LINKS)) {
      existingPath = existingPath.getParent();
    }
    if (existingPath == null) {
      return absolutePath;
    }
    Path realExistingPath = existingPath.toRealPath();
    return realExistingPath.resolve(existingPath.relativize(absolutePath)).normalize();
  }
}

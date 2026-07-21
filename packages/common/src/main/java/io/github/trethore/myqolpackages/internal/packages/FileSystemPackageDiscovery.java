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

import io.github.trethore.myqolpackages.api.packages.PackageDiagnostic;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class FileSystemPackageDiscovery {
  private static final String ENTRYPOINT_EXTENSION = ".js";
  private static final String MANIFEST_FILE_NAME = "manifest.json";

  private final PackageManifestReader manifestReader;

  public FileSystemPackageDiscovery() {
    this(new PackageManifestReader());
  }

  FileSystemPackageDiscovery(PackageManifestReader manifestReader) {
    this.manifestReader = manifestReader;
  }

  PackageDiscoverySnapshot discover(Path packageDirectory) {
    List<PackageDescriptor> packages = new ArrayList<>();
    List<PackageDiagnostic> diagnostics = new ArrayList<>();

    try {
      if (!Files.isDirectory(packageDirectory)) {
        throw new IOException("Package root does not exist or is not a directory");
      }
      for (Path candidate : listPackageDirectories(packageDirectory)) {
        discoverPackage(candidate, packages, diagnostics);
      }
    } catch (IOException exception) {
      diagnostics.add(
          new PackageDiagnostic(
              packageDirectory.getFileName().toString(),
              packageDirectory,
              "Could not read package directory: " + exception.getMessage()));
    }

    return new PackageDiscoverySnapshot(packages, diagnostics);
  }

  private List<Path> listPackageDirectories(Path packageDirectory) throws IOException {
    try (Stream<Path> children = Files.list(packageDirectory)) {
      return children
          .filter(Files::isDirectory)
          .sorted(Comparator.comparing(path -> path.getFileName().toString()))
          .toList();
    }
  }

  private void discoverPackage(
      Path packageDirectory,
      List<PackageDescriptor> packages,
      List<PackageDiagnostic> diagnostics) {
    String packageId = packageDirectory.getFileName().toString();
    try {
      Path manifestPath = packageDirectory.resolve(MANIFEST_FILE_NAME);
      if (!Files.isRegularFile(manifestPath)) {
        throw new PackageValidationException("Missing manifest.json");
      }

      PackageManifest manifest = manifestReader.read(manifestPath);
      validateManifest(manifest);
      Path entrypoint = resolveEntrypoint(packageDirectory, manifest.entrypoint());
      packages.add(new PackageDescriptor(packageId, packageDirectory, entrypoint, manifest));
    } catch (IOException | PackageValidationException exception) {
      diagnostics.add(new PackageDiagnostic(packageId, packageDirectory, exception.getMessage()));
    }
  }

  private void validateManifest(PackageManifest manifest) throws PackageValidationException {
    requireValue(manifest.name(), "name");
    requireValue(manifest.description(), "description");
    requireValue(manifest.version(), "version");
    requireValue(manifest.entrypoint(), "entrypoint");
  }

  private void requireValue(String value, String fieldName) throws PackageValidationException {
    if (value == null || value.isBlank()) {
      throw new PackageValidationException("Missing or empty manifest field: " + fieldName);
    }
  }

  private Path resolveEntrypoint(Path packageDirectory, String entrypoint)
      throws IOException, PackageValidationException {
    if (!entrypoint.endsWith(ENTRYPOINT_EXTENSION)) {
      throw new PackageValidationException("Entrypoint must be a .js file");
    }

    Path normalizedPackageDirectory = packageDirectory.toAbsolutePath().normalize();
    Path resolvedEntrypoint = normalizedPackageDirectory.resolve(entrypoint).normalize();
    if (!resolvedEntrypoint.startsWith(normalizedPackageDirectory)) {
      throw new PackageValidationException("Entrypoint must be inside the package directory");
    }
    if (!Files.isRegularFile(resolvedEntrypoint)) {
      throw new PackageValidationException("Entrypoint file does not exist: " + entrypoint);
    }

    Path realPackageDirectory = normalizedPackageDirectory.toRealPath();
    Path realEntrypoint = resolvedEntrypoint.toRealPath();
    if (!realEntrypoint.startsWith(realPackageDirectory)) {
      throw new PackageValidationException("Entrypoint must be inside the package directory");
    }
    return realEntrypoint;
  }
}

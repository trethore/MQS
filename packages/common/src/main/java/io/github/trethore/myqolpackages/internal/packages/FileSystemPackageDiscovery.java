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
import io.github.trethore.myqolpackages.internal.trust.SemanticVersion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
            diagnostics.add(new PackageDiagnostic(
                    packageDirectory.getFileName().toString(),
                    packageDirectory,
                    "Could not read package directory: " + exception.getMessage()));
        }

        return new PackageDiscoverySnapshot(packages, diagnostics);
    }

    private List<Path> listPackageDirectories(Path packageDirectory) throws IOException {
        try (Stream<Path> children = Files.list(packageDirectory)) {
            return children.filter(Files::isDirectory)
                    .filter(path -> !path.getFileName().toString().equals(PackageDirectories.DATA_DIRECTORY_NAME))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    private void discoverPackage(
            Path packageDirectory, List<PackageDescriptor> packages, List<PackageDiagnostic> diagnostics) {
        String diagnosticId = packageDirectory.getFileName().toString();
        try {
            Path manifestPath = packageDirectory.resolve(MANIFEST_FILE_NAME);
            if (!Files.isRegularFile(manifestPath)) {
                throw new PackageValidationException("Missing manifest.json");
            }

            PackageManifest manifest = manifestReader.read(manifestPath);
            validateManifest(manifest);
            SemanticVersion semanticVersion = parseVersion(manifest.version());
            String packageId = resolvePackageId(manifest);
            Path entrypoint = resolveEntrypoint(packageDirectory, manifest.entrypoint());
            packages.add(new PackageDescriptor(packageId, packageDirectory, entrypoint, manifest, semanticVersion));
        } catch (IOException | PackageValidationException exception) {
            diagnostics.add(new PackageDiagnostic(diagnosticId, packageDirectory, exception.getMessage()));
        }
    }

    private void validateManifest(PackageManifest manifest) throws PackageValidationException {
        requireValue(manifest.name(), "name");
        requireValue(manifest.description(), "description");
        requireValue(manifest.version(), "version");
        requireValue(manifest.entrypoint(), "entrypoint");
    }

    private String resolvePackageId(PackageManifest manifest) throws PackageValidationException {
        String packageId = manifest.id();
        if (packageId == null) {
            packageId = derivePackageId(manifest.name());
        }
        if (!isValidPackageId(packageId)) {
            throw new PackageValidationException(
                    "Package ID must contain lowercase letters, numbers, and single hyphens between words");
        }
        return packageId;
    }

    private static String derivePackageId(String packageName) {
        String normalizedName = packageName.toLowerCase(Locale.ROOT);
        StringBuilder packageId = new StringBuilder(normalizedName.length());
        for (int index = 0; index < normalizedName.length(); index++) {
            char character = normalizedName.charAt(index);
            if (isPackageIdWordCharacter(character)) {
                packageId.append(character);
            } else if (!packageId.isEmpty() && packageId.charAt(packageId.length() - 1) != '-') {
                packageId.append('-');
            }
        }

        if (!packageId.isEmpty() && packageId.charAt(packageId.length() - 1) == '-') {
            packageId.deleteCharAt(packageId.length() - 1);
        }
        return packageId.toString();
    }

    private static boolean isValidPackageId(String packageId) {
        if (packageId.isEmpty()) {
            return false;
        }

        for (int index = 0; index < packageId.length(); index++) {
            char character = packageId.charAt(index);
            if (isPackageIdWordCharacter(character)) {
                continue;
            }
            if (character != '-'
                    || index == 0
                    || index == packageId.length() - 1
                    || packageId.charAt(index - 1) == '-') {
                return false;
            }
        }
        return true;
    }

    private static boolean isPackageIdWordCharacter(char character) {
        boolean isLowercaseLetter = character >= 'a' && character <= 'z';
        boolean isNumber = character >= '0' && character <= '9';
        return isLowercaseLetter || isNumber;
    }

    private void requireValue(String value, String fieldName) throws PackageValidationException {
        if (value == null || value.isBlank()) {
            throw new PackageValidationException("Missing or empty manifest field: " + fieldName);
        }
    }

    private SemanticVersion parseVersion(String version) throws PackageValidationException {
        try {
            return SemanticVersion.parse(version);
        } catch (IllegalArgumentException exception) {
            throw new PackageValidationException(exception.getMessage());
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

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

import io.github.trethore.myqolpackages.api.config.MqpConfig;
import io.github.trethore.myqolpackages.api.packages.PackageDiagnostic;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ConfiguredPackageRootResolver implements PackageRootResolver {
    private static final String CONFIG_DIAGNOSTIC_ID = "config";

    private final Path configPath;
    private final Path mqpDirectory;

    public ConfiguredPackageRootResolver(Path mqpDirectory, Path configPath) {
        this.mqpDirectory = mqpDirectory.toAbsolutePath().normalize();
        this.configPath = configPath;
    }

    @Override
    public PackageRootResolution resolvePackageRoots(MqpConfig config) {
        List<PackageDiagnostic> diagnostics = new ArrayList<>();
        Set<Path> packageRoots = new LinkedHashSet<>();
        addDefaultPackageRoot(packageRoots, diagnostics);

        for (String configuredRoot : config.additionalPackageRoots()) {
            addConfiguredPackageRoot(configuredRoot, packageRoots, diagnostics);
        }

        return new PackageRootResolution(List.copyOf(packageRoots), diagnostics);
    }

    private void addDefaultPackageRoot(Set<Path> packageRoots, List<PackageDiagnostic> diagnostics) {
        try {
            Files.createDirectories(mqpDirectory);
            packageRoots.add(mqpDirectory.toRealPath());
        } catch (IOException exception) {
            diagnostics.add(new PackageDiagnostic(
                    CONFIG_DIAGNOSTIC_ID,
                    mqpDirectory,
                    "Could not prepare the default package root: " + exception.getMessage()));
        }
    }

    private void addConfiguredPackageRoot(
            String configuredRoot, Set<Path> packageRoots, List<PackageDiagnostic> diagnostics) {
        if (configuredRoot == null || configuredRoot.isBlank()) {
            diagnostics.add(new PackageDiagnostic(
                    CONFIG_DIAGNOSTIC_ID, configPath, "Additional package root must not be empty"));
            return;
        }

        try {
            Path packageRoot = Path.of(configuredRoot);
            if (!packageRoot.isAbsolute()) {
                packageRoot = mqpDirectory.resolve(packageRoot);
            }
            packageRoot = packageRoot.toAbsolutePath().normalize();
            if (!Files.isDirectory(packageRoot)) {
                diagnostics.add(new PackageDiagnostic(
                        CONFIG_DIAGNOSTIC_ID,
                        packageRoot,
                        "Additional package root does not exist or is not a directory"));
                return;
            }
            packageRoots.add(packageRoot.toRealPath());
        } catch (InvalidPathException exception) {
            diagnostics.add(new PackageDiagnostic(
                    CONFIG_DIAGNOSTIC_ID,
                    configPath,
                    "Invalid additional package root '" + configuredRoot + "': " + exception.getMessage()));
        } catch (IOException exception) {
            diagnostics.add(new PackageDiagnostic(
                    CONFIG_DIAGNOSTIC_ID,
                    configPath,
                    "Could not resolve additional package root '" + configuredRoot + "': " + exception.getMessage()));
        }
    }
}

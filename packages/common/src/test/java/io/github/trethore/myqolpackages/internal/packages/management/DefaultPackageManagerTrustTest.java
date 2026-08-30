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
package io.github.trethore.myqolpackages.internal.packages.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.myqolpackages.api.packages.PackageState;
import io.github.trethore.myqolpackages.api.packages.management.PackageManager;
import io.github.trethore.myqolpackages.api.packages.management.PackageOperationCode;
import io.github.trethore.myqolpackages.api.packages.management.PackageOperationResult;
import io.github.trethore.myqolpackages.api.packages.trust.FingerprintMismatchBehavior;
import io.github.trethore.myqolpackages.api.packages.trust.PackageTrustRequest;
import io.github.trethore.myqolpackages.api.packages.trust.PackageTrustSnapshot;
import io.github.trethore.myqolpackages.api.packages.trust.TrustVersionScope;
import io.github.trethore.myqolpackages.internal.config.FileMqpConfigStore;
import io.github.trethore.myqolpackages.internal.packages.discovery.FileSystemPackageDiscovery;
import io.github.trethore.myqolpackages.internal.packages.discovery.PackageDiscoveryService;
import io.github.trethore.myqolpackages.internal.packages.root.ConfiguredPackageRootResolver;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextFactory;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextSpec;
import io.github.trethore.myqolpackages.internal.runtime.PackageScriptContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultPackageManagerTrustTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void blocksUntrustedPackagesBeforeContextCreation() throws IOException {
        createPackage();
        RecordingContextFactory contextFactory = new RecordingContextFactory();
        try (PackageManager packageManager = createManager(contextFactory)) {
            packageManager.refresh();

            PackageOperationResult result = packageManager.enablePackage("example-package");

            assertFalse(result.successful());
            assertEquals(PackageOperationCode.TRUST_REQUIRED, result.code());
            assertTrue(contextFactory.events.isEmpty());
        }
    }

    @Test
    void commitsTrustThenEnablesAndUntrustsPackage() throws IOException {
        createPackage();
        RecordingContextFactory contextFactory = new RecordingContextFactory();
        try (PackageManager packageManager = createManager(contextFactory)) {
            packageManager.refresh();
            PackageTrustSnapshot snapshot = packageManager
                    .getTrustManager()
                    .captureTrustSnapshot("example-package")
                    .orElseThrow();

            assertTrue(packageManager
                    .getTrustManager()
                    .trustPackage(new PackageTrustRequest(
                            snapshot, TrustVersionScope.EXACT, false, FingerprintMismatchBehavior.BLOCK))
                    .successful());
            assertTrue(packageManager.enablePackage("example-package").successful());
            assertEquals(List.of("enable:example-package"), contextFactory.events);

            assertTrue(packageManager
                    .getTrustManager()
                    .untrustPackage("example-package")
                    .successful());
            assertTrue(packageManager.getConfiguredEnabledPackageIds().isEmpty());
            assertTrue(packageManager.getTrustManager().getTrustedPackageIds().isEmpty());
            assertEquals(
                    List.of("enable:example-package", "disable:example-package", "close:example-package"),
                    contextFactory.events);
        }
    }

    @Test
    void blocksChangedFingerprintWithoutCreatingContext() throws IOException {
        Path packageDirectory = createPackage();
        RecordingContextFactory contextFactory = new RecordingContextFactory();
        try (PackageManager packageManager = createManager(contextFactory)) {
            packageManager.refresh();
            PackageTrustSnapshot snapshot = packageManager
                    .getTrustManager()
                    .captureTrustSnapshot("example-package")
                    .orElseThrow();
            assertTrue(packageManager
                    .getTrustManager()
                    .trustPackage(new PackageTrustRequest(
                            snapshot, TrustVersionScope.ALL_VERSIONS, true, FingerprintMismatchBehavior.BLOCK))
                    .successful());
            Files.writeString(packageDirectory.resolve("src/index.js"), "changed");

            PackageOperationResult result = packageManager.enablePackage("example-package");

            assertFalse(result.successful());
            assertEquals(PackageOperationCode.FINGERPRINT_REVIEW_REQUIRED, result.code());
            assertTrue(contextFactory.events.isEmpty());
        }
    }

    @Test
    void chatWarningRunsWithVisibleWarning() throws IOException {
        Path packageDirectory = createPackage();
        RecordingContextFactory contextFactory = new RecordingContextFactory();
        try (PackageManager packageManager = createManager(contextFactory)) {
            packageManager.refresh();
            PackageTrustSnapshot snapshot = packageManager
                    .getTrustManager()
                    .captureTrustSnapshot("example-package")
                    .orElseThrow();
            packageManager
                    .getTrustManager()
                    .trustPackage(new PackageTrustRequest(
                            snapshot, TrustVersionScope.ALL_VERSIONS, true, FingerprintMismatchBehavior.CHAT_WARNING));
            Files.writeString(packageDirectory.resolve("src/index.js"), "changed");

            PackageOperationResult result = packageManager.enablePackage("example-package");

            assertTrue(result.successful());
            assertEquals(1, result.diagnostics().size());
            assertTrue(result.diagnostics().getFirst().chatVisible());
            assertEquals(List.of("enable:example-package"), contextFactory.events);
        }
    }

    @Test
    void refreshStopsBlockedPackageAndKeepsEnabledIntent() throws IOException {
        Path packageDirectory = createPackage();
        RecordingContextFactory contextFactory = new RecordingContextFactory();
        try (PackageManager packageManager = createManager(contextFactory)) {
            packageManager.refresh();
            PackageTrustSnapshot snapshot = packageManager
                    .getTrustManager()
                    .captureTrustSnapshot("example-package")
                    .orElseThrow();
            packageManager
                    .getTrustManager()
                    .trustPackage(new PackageTrustRequest(
                            snapshot, TrustVersionScope.ALL_VERSIONS, true, FingerprintMismatchBehavior.BLOCK));
            assertTrue(packageManager.enablePackage("example-package").successful());
            Files.writeString(packageDirectory.resolve("src/index.js"), "changed");

            packageManager.refresh();

            assertEquals(
                    PackageState.DISABLED,
                    packageManager.findPackage("example-package").orElseThrow().state());
            assertEquals(List.of("example-package"), packageManager.getConfiguredEnabledPackageIds());
            assertEquals(
                    List.of("enable:example-package", "disable:example-package", "close:example-package"),
                    contextFactory.events);
        }
    }

    private DefaultPackageManager createManager(PackageContextFactory contextFactory) {
        FileMqpConfigStore configManager = new FileMqpConfigStore(temporaryDirectory);
        return new DefaultPackageManager(
                new PackageDiscoveryService(
                        new ConfiguredPackageRootResolver(temporaryDirectory, configManager.getConfigPath()),
                        new FileSystemPackageDiscovery()),
                configManager,
                contextFactory);
    }

    private Path createPackage() throws IOException {
        Path packageDirectory = temporaryDirectory.resolve("example-package");
        Files.createDirectories(packageDirectory.resolve("src"));
        Files.writeString(packageDirectory.resolve("manifest.json"), """
        {
          "id": "example-package",
          "name": "Example Package",
          "description": "A test package.",
          "version": "1.0.0",
          "entrypoint": "src/index.js"
        }
        """);
        Files.writeString(packageDirectory.resolve("src/index.js"), "source");
        return packageDirectory;
    }

    private static final class RecordingContextFactory implements PackageContextFactory {
        private final List<String> events = new ArrayList<>();

        @Override
        public PackageScriptContext create(PackageContextSpec spec) {
            return new PackageScriptContext() {
                @Override
                public void invokeEnable() {
                    events.add("enable:" + spec.packageId());
                }

                @Override
                public void invokeDisable() {
                    events.add("disable:" + spec.packageId());
                }

                @Override
                public void tick() {
                    events.add("tick:" + spec.packageId());
                }

                @Override
                public void close() {
                    events.add("close:" + spec.packageId());
                }
            };
        }
    }
}

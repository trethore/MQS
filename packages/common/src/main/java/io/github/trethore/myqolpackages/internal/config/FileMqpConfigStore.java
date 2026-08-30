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
package io.github.trethore.myqolpackages.internal.config;

import io.github.trethore.myqolpackages.api.MqpDiagnostic;
import io.github.trethore.myqolpackages.api.config.MqpConfig;
import io.github.trethore.myqolpackages.api.config.PackageFingerprintConfig;
import io.github.trethore.myqolpackages.api.config.PackageTrustConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class FileMqpConfigStore implements MqpConfigStore {
    private static final String CONFIG_DIAGNOSTIC_ID = "config";

    private final AtomicReference<MqpConfig> config = new AtomicReference<>(MqpConfig.defaults());
    private final MqpConfigFile configFile;
    private boolean configLoadedSuccessfully;

    public FileMqpConfigStore(Path mqpDirectory) {
        configFile = new MqpConfigFile(mqpDirectory, new MqpConfigCodec());
    }

    @Override
    public synchronized MqpConfigLoadResult load() {
        try {
            Files.createDirectories(configFile.path().getParent());
            if (!configFile.exists()) {
                configFile.write(MqpConfig.defaults());
            }
            MqpConfig loadedConfig = configFile.read();
            config.set(loadedConfig);
            configLoadedSuccessfully = true;
            return new MqpConfigLoadResult(loadedConfig, List.of());
        } catch (IOException | RuntimeException exception) {
            MqpConfig defaultConfig = MqpConfig.defaults();
            config.set(defaultConfig);
            configLoadedSuccessfully = false;
            MqpDiagnostic diagnostic = new MqpDiagnostic(
                    CONFIG_DIAGNOSTIC_ID, configFile.path(), "Could not load config.json: " + exception.getMessage());
            return new MqpConfigLoadResult(defaultConfig, List.of(diagnostic));
        }
    }

    @Override
    public MqpConfig getConfig() {
        return config.get();
    }

    @Override
    public Path getConfigPath() {
        return configFile.path();
    }

    @Override
    public synchronized void addEnabledPackage(String packageId) throws IOException {
        List<String> enabledPackages = new ArrayList<>(config.get().enabledPackages());
        if (enabledPackages.contains(packageId)) {
            return;
        }
        enabledPackages.add(packageId);
        saveEnabledPackages(enabledPackages);
    }

    @Override
    public synchronized void removeEnabledPackage(String packageId) throws IOException {
        List<String> enabledPackages = new ArrayList<>(config.get().enabledPackages());
        if (!enabledPackages.remove(packageId)) {
            return;
        }
        saveEnabledPackages(enabledPackages);
    }

    @Override
    public synchronized void putTrustedPackage(String packageId, PackageTrustConfig packageTrustConfig)
            throws IOException {
        MqpConfig currentConfig = config.get();
        Map<String, PackageTrustConfig> trustedPackages =
                new LinkedHashMap<>(currentConfig.trust().packages());
        trustedPackages.put(packageId, packageTrustConfig);
        saveConfig(currentConfig.withTrust(currentConfig.trust().withPackages(trustedPackages)));
    }

    @Override
    public synchronized void updatePackageFingerprint(String packageId, String fingerprint) throws IOException {
        MqpConfig currentConfig = config.get();
        PackageTrustConfig currentPackageTrust =
                currentConfig.trust().packages().get(packageId);
        if (currentPackageTrust == null) {
            throw new IOException("Package is not trusted");
        }
        PackageFingerprintConfig currentFingerprint = currentPackageTrust.fingerprint();
        PackageFingerprintConfig updatedFingerprint = currentFingerprint == null
                ? new PackageFingerprintConfig(null, null, fingerprint)
                : currentFingerprint.withDigest(fingerprint);
        putTrustedPackage(packageId, new PackageTrustConfig(currentPackageTrust.versions(), updatedFingerprint));
    }

    @Override
    public synchronized void removeTrustedAndEnabledPackage(String packageId) throws IOException {
        MqpConfig currentConfig = config.get();
        List<String> enabledPackages = new ArrayList<>(currentConfig.enabledPackages());
        enabledPackages.remove(packageId);
        Map<String, PackageTrustConfig> trustedPackages =
                new LinkedHashMap<>(currentConfig.trust().packages());
        trustedPackages.remove(packageId);
        saveConfig(currentConfig
                .withEnabledPackages(enabledPackages)
                .withTrust(currentConfig.trust().withPackages(trustedPackages)));
    }

    private void saveEnabledPackages(List<String> enabledPackages) throws IOException {
        MqpConfig currentConfig = config.get();
        saveConfig(currentConfig.withEnabledPackages(enabledPackages));
    }

    private void saveConfig(MqpConfig updatedConfig) throws IOException {
        if (!configLoadedSuccessfully) {
            throw new IOException("Configuration cannot be saved because config.json failed to load");
        }
        configFile.write(updatedConfig);
        config.set(updatedConfig);
    }
}

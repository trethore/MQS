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

import io.github.trethore.myqolpackages.api.packages.PackageDiagnostic;
import io.github.trethore.myqolpackages.api.packages.PackageDiscoveryResult;
import io.github.trethore.myqolpackages.api.packages.PackageInfo;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.api.packages.PackageOperationCode;
import io.github.trethore.myqolpackages.api.packages.PackageOperationResult;
import io.github.trethore.myqolpackages.api.packages.PackageState;
import io.github.trethore.myqolpackages.api.packages.PackageTrustRequest;
import io.github.trethore.myqolpackages.api.packages.PackageTrustSnapshot;
import io.github.trethore.myqolpackages.internal.config.MqpConfigLoadResult;
import io.github.trethore.myqolpackages.internal.config.MqpConfigStore;
import io.github.trethore.myqolpackages.internal.packages.discovery.PackageDiscoveryService;
import io.github.trethore.myqolpackages.internal.packages.model.PackageDescriptor;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextFactory;
import io.github.trethore.myqolpackages.internal.runtime.PackageLifecycleException;
import io.github.trethore.myqolpackages.internal.trust.PackageFingerprintService;
import io.github.trethore.myqolpackages.internal.trust.PackageTrustEvaluation;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultPackageManager implements PackageManager {
    private static final String UNKNOWN_PACKAGE_MESSAGE = "Unknown package";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPackageManager.class);

    private final MqpConfigStore configStore;
    private final PackageContextFactory contextFactory;
    private final PackageDiscoveryService discoveryService;
    private final PackageTrustService trustService;

    private final LinkedHashSet<String> enabledPackageOrder = new LinkedHashSet<>();
    private final Map<String, PackageInstance> packages = new LinkedHashMap<>();

    public DefaultPackageManager(
            PackageDiscoveryService discoveryService,
            MqpConfigStore configStore,
            PackageContextFactory contextFactory) {
        this(discoveryService, configStore, contextFactory, new PackageFingerprintService());
    }

    public DefaultPackageManager(
            PackageDiscoveryService discoveryService,
            MqpConfigStore configStore,
            PackageContextFactory contextFactory,
            PackageFingerprintService fingerprintService) {
        this.discoveryService = Objects.requireNonNull(discoveryService, "discoveryService");
        this.configStore = Objects.requireNonNull(configStore, "configStore");
        this.contextFactory = Objects.requireNonNull(contextFactory, "contextFactory");
        trustService = new PackageTrustService(configStore, fingerprintService);
    }

    @Override
    public synchronized PackageDiscoveryResult refresh() {
        MqpConfigLoadResult configResult = configStore.load();
        PackageDiscoveryService.Result discovery = discoveryService.discover(configResult.config());
        List<PackageDiagnostic> diagnostics = new ArrayList<>(discovery.diagnostics());
        diagnostics.addAll(0, configResult.diagnostics());
        reconcilePackages(discovery.packages(), diagnostics);
        enforceTrustForActivePackages(diagnostics);
        updateTrustInfoForInactivePackages();
        List<PackageInfo> discoveredPackageInfo = discovery.packages().keySet().stream()
                .map(packages::get)
                .map(PackageInstance::getInfo)
                .toList();
        return new PackageDiscoveryResult(discoveredPackageInfo, diagnostics);
    }

    @Override
    public synchronized PackageDiscoveryResult reload() {
        List<PackageDiagnostic> diagnostics = disableAllPackages();
        PackageDiscoveryResult discoveryResult = refresh();
        diagnostics.addAll(discoveryResult.diagnostics());
        for (String packageId : configStore.getConfig().enabledPackages()) {
            enableConfiguredPackage(packageId, diagnostics);
        }
        return new PackageDiscoveryResult(getPackages(), diagnostics);
    }

    @Override
    public synchronized PackageOperationResult reloadPackage(String id) {
        if (!configStore.getConfig().enabledPackages().contains(id)) {
            return failedOperation(id, configStore.getConfigPath(), "Package is not configured as enabled");
        }

        List<PackageDiagnostic> diagnostics = new ArrayList<>();
        PackageInstance packageInstance = packages.get(id);
        Path previousPackageDirectory =
                packageInstance == null ? null : packageInstance.getDescriptor().packageDirectory();
        if (packageInstance != null) {
            try {
                packageInstance.disable();
            } catch (PackageLifecycleException exception) {
                diagnostics.add(createLifecycleDiagnostic(packageInstance, exception));
            }
        }
        enabledPackageOrder.remove(id);

        MqpConfigLoadResult configResult = configStore.load();
        PackageDiscoveryService.Result discovery = discoveryService.discover(configResult.config());
        diagnostics.addAll(configResult.diagnostics());
        diagnostics.addAll(getPackageDiagnostics(id, previousPackageDirectory, discovery.diagnostics()));
        PackageDescriptor descriptor = discovery.packages().get(id);
        if (descriptor == null) {
            packages.remove(id);
            diagnostics.add(new PackageDiagnostic(
                    id, configStore.getConfigPath(), "Configured enabled package could not be found"));
            return new PackageOperationResult(PackageOperationCode.FAILED, diagnostics);
        }
        if (packageInstance == null) {
            packageInstance = new PackageInstance(descriptor, contextFactory);
            packages.put(id, packageInstance);
        } else {
            packageInstance.updateDescriptor(descriptor);
        }

        PackageOperationResult enableResult = enablePackageInstance(packageInstance);
        diagnostics.addAll(enableResult.diagnostics());
        if (enableResult.successful()) {
            enabledPackageOrder.add(id);
        }
        rebuildEnabledPackageOrder();
        boolean successful = enableResult.successful() && diagnostics.stream().noneMatch(PackageDiagnostic::error);
        return new PackageOperationResult(successful ? enableResult.code() : PackageOperationCode.FAILED, diagnostics);
    }

    @Override
    public synchronized PackageOperationResult enablePackage(String id) {
        PackageInstance packageInstance = packages.get(id);
        if (packageInstance == null || !packageInstance.isAvailable()) {
            return failedOperation(id, configStore.getConfigPath(), UNKNOWN_PACKAGE_MESSAGE);
        }
        if (packageInstance.getState() == PackageState.ENABLED) {
            return failedOperation(packageInstance, "Already enabled");
        }

        PackageOperationResult enableResult = enablePackageInstance(packageInstance);
        if (!enableResult.successful()) {
            return enableResult;
        }

        try {
            configStore.addEnabledPackage(id);
            enabledPackageOrder.add(id);
            return enableResult;
        } catch (IOException exception) {
            List<PackageDiagnostic> diagnostics = new ArrayList<>(enableResult.diagnostics());
            diagnostics.add(new PackageDiagnostic(
                    id,
                    configStore.getConfigPath(),
                    "Could not save enabled package state: " + exception.getMessage()));
            try {
                packageInstance.disable();
            } catch (PackageLifecycleException disableException) {
                diagnostics.add(createLifecycleDiagnostic(packageInstance, disableException));
            }
            enabledPackageOrder.remove(id);
            return new PackageOperationResult(PackageOperationCode.FAILED, diagnostics);
        }
    }

    @Override
    public synchronized PackageOperationResult disablePackage(String id) {
        List<PackageDiagnostic> diagnostics = new ArrayList<>();
        PackageInstance packageInstance = packages.get(id);
        boolean configuredEnabled = configStore.getConfig().enabledPackages().contains(id);
        if (packageInstance == null && !configuredEnabled) {
            return failedOperation(id, configStore.getConfigPath(), UNKNOWN_PACKAGE_MESSAGE);
        }
        if (packageInstance != null && packageInstance.getState() == PackageState.DISABLED && !configuredEnabled) {
            return failedOperation(packageInstance, "Already disabled");
        }

        try {
            configStore.removeEnabledPackage(id);
        } catch (IOException exception) {
            return failedOperation(
                    id,
                    configStore.getConfigPath(),
                    "Could not save disabled package state: " + exception.getMessage());
        }

        if (packageInstance != null) {
            try {
                packageInstance.disable();
            } catch (PackageLifecycleException exception) {
                diagnostics.add(createLifecycleDiagnostic(packageInstance, exception));
            }
            enabledPackageOrder.remove(id);
            if (!packageInstance.isAvailable()) {
                packages.remove(id);
            }
        }
        return new PackageOperationResult(
                diagnostics.isEmpty() ? PackageOperationCode.SUCCESS : PackageOperationCode.FAILED, diagnostics);
    }

    @Override
    public synchronized PackageOperationResult trustPackage(PackageTrustRequest request) {
        Objects.requireNonNull(request, "request");
        PackageTrustSnapshot expectedPackage = Objects.requireNonNull(request.expectedPackage(), "expectedPackage");
        PackageInstance packageInstance = packages.get(expectedPackage.id());
        if (packageInstance == null || !packageInstance.isAvailable()) {
            return failedOperation(expectedPackage.id(), configStore.getConfigPath(), UNKNOWN_PACKAGE_MESSAGE);
        }
        return trustService.trustPackage(packageInstance, request);
    }

    @Override
    public synchronized PackageOperationResult untrustPackage(String id) {
        PackageInstance packageInstance = packages.get(id);
        if (packageInstance == null
                && !configStore.getConfig().trust().packages().containsKey(id)) {
            return failedOperation(id, configStore.getConfigPath(), UNKNOWN_PACKAGE_MESSAGE);
        }
        try {
            configStore.removeTrustedAndEnabledPackage(id);
        } catch (IOException exception) {
            return failedOperation(
                    id, configStore.getConfigPath(), "Could not save package trust: " + exception.getMessage());
        }

        List<PackageDiagnostic> diagnostics = new ArrayList<>();
        enabledPackageOrder.remove(id);
        if (packageInstance != null) {
            try {
                packageInstance.disable();
            } catch (PackageLifecycleException exception) {
                diagnostics.add(createLifecycleDiagnostic(packageInstance, exception));
            }
            trustService.updateTrustInfo(packageInstance);
        }
        return new PackageOperationResult(
                diagnostics.isEmpty() ? PackageOperationCode.SUCCESS : PackageOperationCode.FAILED, diagnostics);
    }

    @Override
    public synchronized PackageOperationResult acceptPackageFingerprint(String id, String expectedFingerprint) {
        PackageInstance packageInstance = packages.get(id);
        if (packageInstance == null || !packageInstance.isAvailable()) {
            return failedOperation(id, configStore.getConfigPath(), UNKNOWN_PACKAGE_MESSAGE);
        }
        return trustService.acceptFingerprint(packageInstance, expectedFingerprint);
    }

    @Override
    public synchronized List<PackageInfo> getPackages() {
        return packages.values().stream().map(PackageInstance::getInfo).toList();
    }

    @Override
    public synchronized Optional<PackageInfo> findPackage(String id) {
        PackageInstance packageInstance = packages.get(id);
        return packageInstance == null ? Optional.empty() : Optional.of(packageInstance.getInfo());
    }

    @Override
    public List<String> getConfiguredEnabledPackageIds() {
        return configStore.getConfig().enabledPackages();
    }

    @Override
    public List<String> getTrustedPackageIds() {
        return List.copyOf(configStore.getConfig().trust().packages().keySet());
    }

    @Override
    public synchronized Optional<PackageTrustSnapshot> captureTrustSnapshot(String id) {
        PackageInstance packageInstance = packages.get(id);
        if (packageInstance == null) {
            return Optional.empty();
        }
        return trustService.captureSnapshot(packageInstance);
    }

    @Override
    public synchronized void tick() {
        for (String packageId : enabledPackageOrder) {
            PackageInstance packageInstance = packages.get(packageId);
            if (packageInstance == null) {
                continue;
            }
            try {
                packageInstance.tick();
            } catch (PackageLifecycleException exception) {
                LOGGER.atError()
                        .setCause(exception)
                        .log("Could not process asynchronous work for package {}", packageId);
            }
        }
    }

    @Override
    public synchronized void close() {
        disableAllPackages();
        try {
            contextFactory.close();
        } catch (PackageLifecycleException exception) {
            LOGGER.warn("Could not close package context factory", exception);
        }
    }

    private void reconcilePackages(
            Map<String, PackageDescriptor> discoveredPackages, List<PackageDiagnostic> diagnostics) {
        Map<String, PackageInstance> reconciledPackages = new LinkedHashMap<>();
        for (PackageDescriptor descriptor : discoveredPackages.values()) {
            PackageInstance packageInstance = packages.get(descriptor.id());
            if (packageInstance == null) {
                packageInstance = new PackageInstance(descriptor, contextFactory);
            } else {
                packageInstance.updateDescriptor(descriptor);
            }
            reconciledPackages.put(descriptor.id(), packageInstance);
        }

        for (PackageInstance packageInstance : packages.values()) {
            if (discoveredPackages.containsKey(packageInstance.getId())) {
                continue;
            }
            if (packageInstance.getState() == PackageState.ENABLED) {
                packageInstance.markUnavailable();
                reconciledPackages.put(packageInstance.getId(), packageInstance);
                diagnostics.add(new PackageDiagnostic(
                        packageInstance.getId(),
                        packageInstance.getDescriptor().packageDirectory(),
                        "Enabled package is no longer discoverable and remains active until disabled or reloaded"));
            }
        }

        packages.clear();
        packages.putAll(reconciledPackages);
    }

    private void enforceTrustForActivePackages(List<PackageDiagnostic> diagnostics) {
        for (String packageId : List.copyOf(enabledPackageOrder)) {
            PackageInstance packageInstance = packages.get(packageId);
            if (packageInstance != null && packageInstance.getState() == PackageState.ENABLED) {
                PackageTrustEvaluation evaluation = trustService.evaluate(packageInstance);
                if (evaluation.allowed()) {
                    trustService.addWarning(packageInstance, evaluation, diagnostics);
                } else {
                    diagnostics.add(trustService.createBlockedDiagnostic(packageInstance, evaluation));
                    try {
                        packageInstance.disable();
                    } catch (PackageLifecycleException exception) {
                        diagnostics.add(createLifecycleDiagnostic(packageInstance, exception));
                    }
                    enabledPackageOrder.remove(packageId);
                }
            }
        }
    }

    private void updateTrustInfoForInactivePackages() {
        for (PackageInstance packageInstance : packages.values()) {
            if (packageInstance.getState() != PackageState.ENABLED) {
                trustService.updateTrustInfo(packageInstance);
            }
        }
    }

    private List<PackageDiagnostic> disableAllPackages() {
        List<PackageDiagnostic> diagnostics = new ArrayList<>();
        for (String packageId : enabledPackageOrder.reversed()) {
            PackageInstance packageInstance = packages.get(packageId);
            if (packageInstance == null) {
                continue;
            }
            try {
                packageInstance.disable();
            } catch (PackageLifecycleException exception) {
                diagnostics.add(createLifecycleDiagnostic(packageInstance, exception));
            }
        }
        enabledPackageOrder.clear();
        return diagnostics;
    }

    private void enableConfiguredPackage(String packageId, List<PackageDiagnostic> diagnostics) {
        PackageInstance packageInstance = packages.get(packageId);
        if (packageInstance == null || !packageInstance.isAvailable()) {
            diagnostics.add(new PackageDiagnostic(
                    packageId, configStore.getConfigPath(), "Configured enabled package could not be found"));
            return;
        }
        PackageOperationResult result = enablePackageInstance(packageInstance);
        diagnostics.addAll(result.diagnostics());
        if (result.successful()) {
            enabledPackageOrder.add(packageId);
        }
    }

    private PackageOperationResult enablePackageInstance(PackageInstance packageInstance) {
        PackageTrustEvaluation evaluation = trustService.evaluate(packageInstance);
        if (!evaluation.allowed()) {
            PackageOperationCode code = evaluation.info().state().requiresTrust()
                    ? PackageOperationCode.TRUST_REQUIRED
                    : PackageOperationCode.FINGERPRINT_REVIEW_REQUIRED;
            return new PackageOperationResult(
                    code, List.of(trustService.createBlockedDiagnostic(packageInstance, evaluation)));
        }

        List<PackageDiagnostic> diagnostics = new ArrayList<>();
        trustService.addWarning(packageInstance, evaluation, diagnostics);
        try {
            packageInstance.enable();
            return new PackageOperationResult(PackageOperationCode.SUCCESS, diagnostics);
        } catch (PackageLifecycleException exception) {
            diagnostics.add(createLifecycleDiagnostic(packageInstance, exception));
            return new PackageOperationResult(PackageOperationCode.FAILED, diagnostics);
        }
    }

    private void rebuildEnabledPackageOrder() {
        enabledPackageOrder.clear();
        for (String packageId : configStore.getConfig().enabledPackages()) {
            PackageInstance packageInstance = packages.get(packageId);
            if (packageInstance != null && packageInstance.getState() == PackageState.ENABLED) {
                enabledPackageOrder.add(packageId);
            }
        }
    }

    private List<PackageDiagnostic> getPackageDiagnostics(
            String packageId, Path previousPackageDirectory, List<PackageDiagnostic> diagnostics) {
        return diagnostics.stream()
                .filter(diagnostic -> diagnostic.packageId().equals(packageId)
                        || diagnostic.packageDirectory().equals(previousPackageDirectory))
                .toList();
    }

    private PackageOperationResult failedOperation(PackageInstance packageInstance, String message) {
        return failedOperation(
                packageInstance.getId(), packageInstance.getDescriptor().packageDirectory(), message);
    }

    private PackageOperationResult failedOperation(String id, Path path, String message) {
        return new PackageOperationResult(
                PackageOperationCode.FAILED, List.of(new PackageDiagnostic(id, path, message)));
    }

    private PackageDiagnostic createLifecycleDiagnostic(
            PackageInstance packageInstance, PackageLifecycleException exception) {
        LOGGER.atError().setCause(exception).log("Package {} lifecycle operation failed", packageInstance.getId());
        return new PackageDiagnostic(
                packageInstance.getId(), packageInstance.getDescriptor().packageDirectory(), exception.getMessage());
    }
}

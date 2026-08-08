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

import io.github.trethore.myqolpackages.api.config.PackageFingerprintConfig;
import io.github.trethore.myqolpackages.api.config.PackageTrustConfig;
import io.github.trethore.myqolpackages.api.packages.PackageDiagnostic;
import io.github.trethore.myqolpackages.api.packages.PackageDiagnosticCode;
import io.github.trethore.myqolpackages.api.packages.PackageDiscoveryResult;
import io.github.trethore.myqolpackages.api.packages.PackageInfo;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.api.packages.PackageOperationCode;
import io.github.trethore.myqolpackages.api.packages.PackageOperationResult;
import io.github.trethore.myqolpackages.api.packages.PackageState;
import io.github.trethore.myqolpackages.api.packages.PackageTrustRequest;
import io.github.trethore.myqolpackages.api.packages.PackageTrustSnapshot;
import io.github.trethore.myqolpackages.api.packages.PackageTrustState;
import io.github.trethore.myqolpackages.internal.config.GsonMqpConfigManager;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextFactory;
import io.github.trethore.myqolpackages.internal.runtime.PackageLifecycleException;
import io.github.trethore.myqolpackages.internal.trust.PackageFingerprintException;
import io.github.trethore.myqolpackages.internal.trust.PackageFingerprintService;
import io.github.trethore.myqolpackages.internal.trust.PackageTrustEvaluation;
import io.github.trethore.myqolpackages.internal.trust.PackageTrustEvaluator;
import io.github.trethore.myqolpackages.internal.trust.TrustedVersionRange;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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

  private final GsonMqpConfigManager configManager;
  private final PackageContextFactory contextFactory;
  private final PackageFingerprintService fingerprintService;
  private final FileSystemPackageDiscovery packageDiscovery;
  private final PackageRootProvider packageRootProvider;
  private final PackageTrustEvaluator trustEvaluator;

  private final LinkedHashSet<String> enabledPackageOrder = new LinkedHashSet<>();
  private final Map<String, PackageInstance> packages = new LinkedHashMap<>();

  public DefaultPackageManager(
      PackageRootProvider packageRootProvider,
      FileSystemPackageDiscovery packageDiscovery,
      GsonMqpConfigManager configManager,
      PackageContextFactory contextFactory) {
    this(
        packageRootProvider,
        packageDiscovery,
        configManager,
        contextFactory,
        new PackageFingerprintService());
  }

  public DefaultPackageManager(
      PackageRootProvider packageRootProvider,
      FileSystemPackageDiscovery packageDiscovery,
      GsonMqpConfigManager configManager,
      PackageContextFactory contextFactory,
      PackageFingerprintService fingerprintService) {
    this.packageRootProvider = Objects.requireNonNull(packageRootProvider, "packageRootProvider");
    this.packageDiscovery = Objects.requireNonNull(packageDiscovery, "packageDiscovery");
    this.configManager = Objects.requireNonNull(configManager, "configManager");
    this.contextFactory = Objects.requireNonNull(contextFactory, "contextFactory");
    this.fingerprintService = Objects.requireNonNull(fingerprintService, "fingerprintService");
    trustEvaluator = new PackageTrustEvaluator(fingerprintService);
  }

  @Override
  public synchronized PackageDiscoveryResult refresh() {
    DiscoveredPackages discovery = discoverPackages();
    List<PackageDiagnostic> diagnostics = new ArrayList<>(discovery.diagnostics());
    reconcilePackages(discovery.packages(), diagnostics);
    enforceTrustForActivePackages(diagnostics);
    updateTrustInfoForInactivePackages();
    List<PackageInfo> discoveredPackageInfo =
        discovery.packages().keySet().stream()
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
    for (String packageId : configManager.getConfig().enabledPackages()) {
      enableConfiguredPackage(packageId, diagnostics);
    }
    return new PackageDiscoveryResult(getPackages(), diagnostics);
  }

  @Override
  public synchronized PackageOperationResult reloadPackage(String id) {
    if (!configManager.getConfig().enabledPackages().contains(id)) {
      return failedOperation(
          id, configManager.getConfigPath(), "Package is not configured as enabled");
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

    DiscoveredPackages discovery = discoverPackages();
    diagnostics.addAll(
        getPackageDiagnostics(id, previousPackageDirectory, discovery.diagnostics()));
    PackageDescriptor descriptor = discovery.packages().get(id);
    if (descriptor == null) {
      packages.remove(id);
      diagnostics.add(
          new PackageDiagnostic(
              id, configManager.getConfigPath(), "Configured enabled package could not be found"));
      return new PackageOperationResult(false, PackageOperationCode.FAILED, diagnostics);
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
    boolean successful =
        enableResult.successful() && diagnostics.stream().noneMatch(PackageDiagnostic::error);
    return new PackageOperationResult(
        successful, successful ? enableResult.code() : PackageOperationCode.FAILED, diagnostics);
  }

  @Override
  public synchronized PackageOperationResult enablePackage(String id) {
    PackageInstance packageInstance = packages.get(id);
    if (packageInstance == null || !packageInstance.isAvailable()) {
      return failedOperation(id, configManager.getConfigPath(), UNKNOWN_PACKAGE_MESSAGE);
    }
    if (packageInstance.getState() == PackageState.ENABLED) {
      return failedOperation(packageInstance, "Already enabled");
    }

    PackageOperationResult enableResult = enablePackageInstance(packageInstance);
    if (!enableResult.successful()) {
      return enableResult;
    }

    try {
      configManager.addEnabledPackage(id);
      enabledPackageOrder.add(id);
      return enableResult;
    } catch (IOException exception) {
      List<PackageDiagnostic> diagnostics = new ArrayList<>(enableResult.diagnostics());
      diagnostics.add(
          new PackageDiagnostic(
              id,
              configManager.getConfigPath(),
              "Could not save enabled package state: " + exception.getMessage()));
      try {
        packageInstance.disable();
      } catch (PackageLifecycleException disableException) {
        diagnostics.add(createLifecycleDiagnostic(packageInstance, disableException));
      }
      enabledPackageOrder.remove(id);
      return new PackageOperationResult(false, PackageOperationCode.FAILED, diagnostics);
    }
  }

  @Override
  public synchronized PackageOperationResult disablePackage(String id) {
    List<PackageDiagnostic> diagnostics = new ArrayList<>();
    PackageInstance packageInstance = packages.get(id);
    boolean configuredEnabled = configManager.getConfig().enabledPackages().contains(id);
    if (packageInstance == null && !configuredEnabled) {
      return failedOperation(id, configManager.getConfigPath(), UNKNOWN_PACKAGE_MESSAGE);
    }
    if (packageInstance != null
        && packageInstance.getState() == PackageState.DISABLED
        && !configuredEnabled) {
      return failedOperation(packageInstance, "Already disabled");
    }

    try {
      configManager.removeEnabledPackage(id);
    } catch (IOException exception) {
      return failedOperation(
          id,
          configManager.getConfigPath(),
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
        diagnostics.isEmpty(),
        diagnostics.isEmpty() ? PackageOperationCode.SUCCESS : PackageOperationCode.FAILED,
        diagnostics);
  }

  @Override
  public synchronized PackageOperationResult trustPackage(PackageTrustRequest request) {
    Objects.requireNonNull(request, "request");
    PackageTrustSnapshot expectedPackage =
        Objects.requireNonNull(request.expectedPackage(), "expectedPackage");
    PackageInstance packageInstance = packages.get(expectedPackage.id());
    if (packageInstance == null || !packageInstance.isAvailable()) {
      return failedOperation(
          expectedPackage.id(), configManager.getConfigPath(), UNKNOWN_PACKAGE_MESSAGE);
    }

    PackageDescriptor descriptor = packageInstance.getDescriptor();
    if (!descriptor.packageDirectory().equals(expectedPackage.packageDirectory())
        || !descriptor.manifest().version().equals(expectedPackage.version())) {
      return failedOperation(packageInstance, "Package changed while trust was being reviewed");
    }

    String currentFingerprint = captureFingerprint(descriptor.packageDirectory());
    if (expectedPackage.fingerprint() != null
        && !expectedPackage.fingerprint().equals(currentFingerprint)) {
      return failedOperation(packageInstance, "Package changed while trust was being reviewed");
    }
    if (request.fingerprintEnabled() && currentFingerprint == null) {
      return failedOperation(packageInstance, "Could not fingerprint package");
    }

    String versions =
        TrustedVersionRange.create(request.versionScope(), descriptor.semanticVersion());
    PackageFingerprintConfig fingerprintConfig =
        new PackageFingerprintConfig(
            request.fingerprintEnabled(),
            request.mismatchBehavior(),
            request.fingerprintEnabled() ? currentFingerprint : null);
    try {
      configManager.putTrustedPackage(
          descriptor.id(), new PackageTrustConfig(versions, fingerprintConfig));
    } catch (IOException exception) {
      return failedOperation(
          descriptor.id(),
          configManager.getConfigPath(),
          "Could not save package trust: " + exception.getMessage());
    }
    updateTrustInfo(packageInstance);
    return new PackageOperationResult(true, PackageOperationCode.SUCCESS, List.of());
  }

  @Override
  public synchronized PackageOperationResult untrustPackage(String id) {
    PackageInstance packageInstance = packages.get(id);
    if (packageInstance == null && !configManager.getConfig().trust().packages().containsKey(id)) {
      return failedOperation(id, configManager.getConfigPath(), UNKNOWN_PACKAGE_MESSAGE);
    }
    try {
      configManager.removeTrustedAndEnabledPackage(id);
    } catch (IOException exception) {
      return failedOperation(
          id,
          configManager.getConfigPath(),
          "Could not save package trust: " + exception.getMessage());
    }

    List<PackageDiagnostic> diagnostics = new ArrayList<>();
    enabledPackageOrder.remove(id);
    if (packageInstance != null) {
      try {
        packageInstance.disable();
      } catch (PackageLifecycleException exception) {
        diagnostics.add(createLifecycleDiagnostic(packageInstance, exception));
      }
      updateTrustInfo(packageInstance);
    }
    return new PackageOperationResult(
        diagnostics.isEmpty(),
        diagnostics.isEmpty() ? PackageOperationCode.SUCCESS : PackageOperationCode.FAILED,
        diagnostics);
  }

  @Override
  public synchronized PackageOperationResult acceptPackageFingerprint(
      String id, String expectedFingerprint) {
    PackageInstance packageInstance = packages.get(id);
    if (packageInstance == null || !packageInstance.isAvailable()) {
      return failedOperation(id, configManager.getConfigPath(), UNKNOWN_PACKAGE_MESSAGE);
    }
    PackageTrustConfig packageTrustConfig = configManager.getConfig().trust().packages().get(id);
    if (packageTrustConfig == null) {
      return new PackageOperationResult(
          false,
          PackageOperationCode.TRUST_REQUIRED,
          List.of(createTrustRequiredDiagnostic(packageInstance, "Package is not trusted")));
    }
    if (!TrustedVersionRange.parse(packageTrustConfig.versions())
        .matches(packageInstance.getDescriptor().semanticVersion())) {
      return new PackageOperationResult(
          false,
          PackageOperationCode.TRUST_REQUIRED,
          List.of(
              createTrustRequiredDiagnostic(
                  packageInstance, "Package version is outside its trusted range")));
    }

    String currentFingerprint =
        captureFingerprint(packageInstance.getDescriptor().packageDirectory());
    if (currentFingerprint == null) {
      return failedOperation(packageInstance, "Could not fingerprint package");
    }
    if (expectedFingerprint != null && !expectedFingerprint.equals(currentFingerprint)) {
      return failedOperation(
          packageInstance, "Package changed before its fingerprint was accepted");
    }
    try {
      configManager.updatePackageFingerprint(id, currentFingerprint);
    } catch (IOException exception) {
      return failedOperation(
          id,
          configManager.getConfigPath(),
          "Could not save package fingerprint: " + exception.getMessage());
    }
    updateTrustInfo(packageInstance);
    return new PackageOperationResult(true, PackageOperationCode.SUCCESS, List.of());
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
    return configManager.getConfig().enabledPackages();
  }

  @Override
  public List<String> getTrustedPackageIds() {
    return List.copyOf(configManager.getConfig().trust().packages().keySet());
  }

  @Override
  public synchronized Optional<PackageTrustSnapshot> captureTrustSnapshot(String id) {
    PackageInstance packageInstance = packages.get(id);
    if (packageInstance == null || !packageInstance.isAvailable()) {
      return Optional.empty();
    }
    PackageDescriptor descriptor = packageInstance.getDescriptor();
    return Optional.of(
        new PackageTrustSnapshot(
            descriptor.id(),
            descriptor.manifest().name(),
            descriptor.manifest().version(),
            descriptor.packageDirectory(),
            captureFingerprint(descriptor.packageDirectory())));
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
        LOGGER
            .atError()
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

  private DiscoveredPackages discoverPackages() {
    PackageRootResolution rootResolution = packageRootProvider.resolvePackageRoots();
    List<PackageDiagnostic> diagnostics = new ArrayList<>(rootResolution.diagnostics());
    Map<String, List<PackageDescriptor>> packagesById = new LinkedHashMap<>();
    for (Path packageRoot : rootResolution.packageRoots()) {
      PackageDiscoverySnapshot snapshot = packageDiscovery.discover(packageRoot);
      diagnostics.addAll(snapshot.diagnostics());
      for (PackageDescriptor descriptor : snapshot.packages()) {
        packagesById.computeIfAbsent(descriptor.id(), ignored -> new ArrayList<>()).add(descriptor);
      }
    }

    Map<String, PackageDescriptor> discoveredPackages = new LinkedHashMap<>();
    for (Map.Entry<String, List<PackageDescriptor>> entry : packagesById.entrySet()) {
      List<PackageDescriptor> matchingPackages = entry.getValue();
      if (matchingPackages.size() == 1) {
        discoveredPackages.put(entry.getKey(), matchingPackages.getFirst());
        continue;
      }
      for (PackageDescriptor descriptor : matchingPackages) {
        diagnostics.add(
            new PackageDiagnostic(
                descriptor.id(),
                descriptor.packageDirectory(),
                "Duplicate package ID; all packages with this ID were ignored"));
      }
    }
    return new DiscoveredPackages(discoveredPackages, diagnostics);
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
        diagnostics.add(
            new PackageDiagnostic(
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
        PackageTrustEvaluation evaluation = evaluate(packageInstance);
        if (evaluation.allowed()) {
          addTrustWarning(packageInstance, evaluation, diagnostics);
        } else {
          diagnostics.add(createBlockedTrustDiagnostic(packageInstance, evaluation));
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
        updateTrustInfo(packageInstance);
      }
    }
  }

  private void updateTrustInfo(PackageInstance packageInstance) {
    packageInstance.setTrustInfo(evaluate(packageInstance).info());
  }

  private PackageTrustEvaluation evaluate(PackageInstance packageInstance) {
    PackageDescriptor descriptor = packageInstance.getDescriptor();
    PackageTrustEvaluation evaluation =
        trustEvaluator.evaluate(
            descriptor.id(),
            descriptor.semanticVersion(),
            descriptor.packageDirectory(),
            configManager.getConfig());
    packageInstance.setTrustInfo(evaluation.info());
    return evaluation;
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
      diagnostics.add(
          new PackageDiagnostic(
              packageId,
              configManager.getConfigPath(),
              "Configured enabled package could not be found"));
      return;
    }
    PackageOperationResult result = enablePackageInstance(packageInstance);
    diagnostics.addAll(result.diagnostics());
    if (result.successful()) {
      enabledPackageOrder.add(packageId);
    }
  }

  private PackageOperationResult enablePackageInstance(PackageInstance packageInstance) {
    PackageTrustEvaluation evaluation = evaluate(packageInstance);
    if (!evaluation.allowed()) {
      PackageOperationCode code =
          evaluation.info().state() == PackageTrustState.UNTRUSTED
                  || evaluation.info().state() == PackageTrustState.VERSION_NOT_TRUSTED
              ? PackageOperationCode.TRUST_REQUIRED
              : PackageOperationCode.FINGERPRINT_REVIEW_REQUIRED;
      return new PackageOperationResult(
          false, code, List.of(createBlockedTrustDiagnostic(packageInstance, evaluation)));
    }

    List<PackageDiagnostic> diagnostics = new ArrayList<>();
    addTrustWarning(packageInstance, evaluation, diagnostics);
    try {
      packageInstance.enable();
      return new PackageOperationResult(true, PackageOperationCode.SUCCESS, diagnostics);
    } catch (PackageLifecycleException exception) {
      diagnostics.add(createLifecycleDiagnostic(packageInstance, exception));
      return new PackageOperationResult(false, PackageOperationCode.FAILED, diagnostics);
    }
  }

  private void addTrustWarning(
      PackageInstance packageInstance,
      PackageTrustEvaluation evaluation,
      List<PackageDiagnostic> diagnostics) {
    if (!evaluation.warning()) {
      return;
    }
    LOGGER.warn(
        "Package {} fingerprint changed: expected {}, found {}",
        packageInstance.getId(),
        evaluation.info().expectedFingerprint(),
        evaluation.info().currentFingerprint());
    if (evaluation.chatVisible()) {
      diagnostics.add(
          new PackageDiagnostic(
              PackageDiagnosticCode.FINGERPRINT_WARNING,
              packageInstance.getId(),
              packageInstance.getDescriptor().packageDirectory(),
              evaluation.info().message(),
              true,
              false));
    }
  }

  private PackageDiagnostic createBlockedTrustDiagnostic(
      PackageInstance packageInstance, PackageTrustEvaluation evaluation) {
    LOGGER.warn("Blocked package {}: {}", packageInstance.getId(), evaluation.info().message());
    PackageDiagnosticCode code =
        evaluation.info().state() == PackageTrustState.UNTRUSTED
                || evaluation.info().state() == PackageTrustState.VERSION_NOT_TRUSTED
            ? PackageDiagnosticCode.TRUST_REQUIRED
            : PackageDiagnosticCode.FINGERPRINT_BLOCKED;
    return new PackageDiagnostic(
        code,
        packageInstance.getId(),
        packageInstance.getDescriptor().packageDirectory(),
        evaluation.info().message(),
        true,
        true);
  }

  private PackageDiagnostic createTrustRequiredDiagnostic(
      PackageInstance packageInstance, String message) {
    return new PackageDiagnostic(
        PackageDiagnosticCode.TRUST_REQUIRED,
        packageInstance.getId(),
        packageInstance.getDescriptor().packageDirectory(),
        message,
        true,
        true);
  }

  private String captureFingerprint(Path packageDirectory) {
    try {
      return fingerprintService.fingerprint(packageDirectory);
    } catch (PackageFingerprintException exception) {
      LOGGER.warn("Could not fingerprint package at {}", packageDirectory, exception);
      return null;
    }
  }

  private void rebuildEnabledPackageOrder() {
    enabledPackageOrder.clear();
    for (String packageId : configManager.getConfig().enabledPackages()) {
      PackageInstance packageInstance = packages.get(packageId);
      if (packageInstance != null && packageInstance.getState() == PackageState.ENABLED) {
        enabledPackageOrder.add(packageId);
      }
    }
  }

  private List<PackageDiagnostic> getPackageDiagnostics(
      String packageId, Path previousPackageDirectory, List<PackageDiagnostic> diagnostics) {
    return diagnostics.stream()
        .filter(
            diagnostic ->
                diagnostic.packageId().equals(packageId)
                    || diagnostic.packageDirectory().equals(previousPackageDirectory))
        .toList();
  }

  private PackageOperationResult failedOperation(PackageInstance packageInstance, String message) {
    return failedOperation(
        packageInstance.getId(), packageInstance.getDescriptor().packageDirectory(), message);
  }

  private PackageOperationResult failedOperation(String id, Path path, String message) {
    return new PackageOperationResult(
        false, PackageOperationCode.FAILED, List.of(new PackageDiagnostic(id, path, message)));
  }

  private PackageDiagnostic createLifecycleDiagnostic(
      PackageInstance packageInstance, PackageLifecycleException exception) {
    LOGGER
        .atError()
        .setCause(exception)
        .log("Package {} lifecycle operation failed", packageInstance.getId());
    return new PackageDiagnostic(
        packageInstance.getId(),
        packageInstance.getDescriptor().packageDirectory(),
        exception.getMessage());
  }

  private record DiscoveredPackages(
      Map<String, PackageDescriptor> packages, List<PackageDiagnostic> diagnostics) {
    private DiscoveredPackages {
      packages = Collections.unmodifiableMap(new LinkedHashMap<>(packages));
      diagnostics = List.copyOf(diagnostics);
    }
  }
}

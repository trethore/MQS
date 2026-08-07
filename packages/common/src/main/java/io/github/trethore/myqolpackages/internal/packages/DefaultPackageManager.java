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
import io.github.trethore.myqolpackages.api.packages.PackageDiscoveryResult;
import io.github.trethore.myqolpackages.api.packages.PackageInfo;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.api.packages.PackageOperationResult;
import io.github.trethore.myqolpackages.api.packages.PackageState;
import io.github.trethore.myqolpackages.internal.config.GsonMqpConfigManager;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextFactory;
import io.github.trethore.myqolpackages.internal.runtime.PackageLifecycleException;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPackageManager.class);

  private final GsonMqpConfigManager configManager;
  private final PackageContextFactory contextFactory;
  private final FileSystemPackageDiscovery packageDiscovery;
  private final PackageRootProvider packageRootProvider;

  private final LinkedHashSet<String> enabledPackageOrder = new LinkedHashSet<>();
  private final Map<String, PackageInstance> packages = new LinkedHashMap<>();

  public DefaultPackageManager(
      PackageRootProvider packageRootProvider,
      FileSystemPackageDiscovery packageDiscovery,
      GsonMqpConfigManager configManager,
      PackageContextFactory contextFactory) {
    this.packageRootProvider = Objects.requireNonNull(packageRootProvider, "packageRootProvider");
    this.packageDiscovery = Objects.requireNonNull(packageDiscovery, "packageDiscovery");
    this.configManager = Objects.requireNonNull(configManager, "configManager");
    this.contextFactory = Objects.requireNonNull(contextFactory, "contextFactory");
  }

  @Override
  public synchronized PackageDiscoveryResult refresh() {
    DiscoveredPackages discovery = discoverPackages();
    List<PackageDiagnostic> diagnostics = new ArrayList<>(discovery.diagnostics());
    reconcilePackages(discovery.packages(), diagnostics);
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
    } else if (packageInstance == null) {
      packageInstance = new PackageInstance(descriptor, contextFactory);
      packages.put(id, packageInstance);
    } else {
      packageInstance.updateDescriptor(descriptor);
    }

    enableConfiguredPackage(id, diagnostics);
    rebuildEnabledPackageOrder();
    return new PackageOperationResult(diagnostics.isEmpty(), diagnostics);
  }

  @Override
  public synchronized PackageOperationResult enablePackage(String id) {
    PackageInstance packageInstance = packages.get(id);
    if (packageInstance == null || !packageInstance.isAvailable()) {
      return failedOperation(id, configManager.getConfigPath(), "Unknown package");
    }
    if (packageInstance.getState() == PackageState.ENABLED) {
      return failedOperation(packageInstance, "Already enabled");
    }

    try {
      enablePackageInstance(packageInstance);
    } catch (PackageLifecycleException exception) {
      return failedOperation(packageInstance, exception.getMessage());
    }

    try {
      configManager.addEnabledPackage(id);
      enabledPackageOrder.add(id);
      return new PackageOperationResult(true, List.of());
    } catch (IOException exception) {
      List<PackageDiagnostic> diagnostics = new ArrayList<>();
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
      return new PackageOperationResult(false, diagnostics);
    }
  }

  @Override
  public synchronized PackageOperationResult disablePackage(String id) {
    List<PackageDiagnostic> diagnostics = new ArrayList<>();
    PackageInstance packageInstance = packages.get(id);
    boolean configuredEnabled = configManager.getConfig().enabledPackages().contains(id);
    if (packageInstance == null && !configuredEnabled) {
      return failedOperation(id, configManager.getConfigPath(), "Unknown package");
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
    return new PackageOperationResult(diagnostics.isEmpty(), diagnostics);
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
  public synchronized void tick() {
    for (String packageId : enabledPackageOrder) {
      PackageInstance packageInstance = packages.get(packageId);
      if (packageInstance == null) {
        continue;
      }
      try {
        packageInstance.tick();
      } catch (PackageLifecycleException exception) {
        LOGGER.warn("Could not process asynchronous work for package {}", packageId, exception);
      }
    }
  }

  @Override
  public synchronized void close() {
    for (PackageDiagnostic diagnostic : disableAllPackages()) {
      LOGGER.warn(
          "Could not stop package {} at {}: {}",
          diagnostic.packageId(),
          diagnostic.packageDirectory(),
          diagnostic.message());
    }
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
    try {
      enablePackageInstance(packageInstance);
      enabledPackageOrder.add(packageId);
    } catch (PackageLifecycleException exception) {
      diagnostics.add(createLifecycleDiagnostic(packageInstance, exception));
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

  private void enablePackageInstance(PackageInstance packageInstance)
      throws PackageLifecycleException {
    packageInstance.enable();
  }

  private PackageOperationResult failedOperation(PackageInstance packageInstance, String message) {
    return failedOperation(
        packageInstance.getId(), packageInstance.getDescriptor().packageDirectory(), message);
  }

  private PackageOperationResult failedOperation(String id, Path path, String message) {
    return new PackageOperationResult(false, List.of(new PackageDiagnostic(id, path, message)));
  }

  private PackageDiagnostic createLifecycleDiagnostic(
      PackageInstance packageInstance, PackageLifecycleException exception) {
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

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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PackageDiscoveryService {
  private final FileSystemPackageDiscovery fileSystemDiscovery;
  private final PackageRootResolver rootResolver;

  public PackageDiscoveryService(
      PackageRootResolver rootResolver, FileSystemPackageDiscovery fileSystemDiscovery) {
    this.rootResolver = Objects.requireNonNull(rootResolver, "rootResolver");
    this.fileSystemDiscovery = Objects.requireNonNull(fileSystemDiscovery, "fileSystemDiscovery");
  }

  Result discover(MqpConfig config) {
    PackageRootResolution rootResolution = rootResolver.resolvePackageRoots(config);
    List<PackageDiagnostic> diagnostics = new ArrayList<>(rootResolution.diagnostics());
    Map<String, List<PackageDescriptor>> packagesById = new LinkedHashMap<>();
    for (Path packageRoot : rootResolution.packageRoots()) {
      PackageDiscoverySnapshot snapshot = fileSystemDiscovery.discover(packageRoot);
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
    return new Result(discoveredPackages, diagnostics);
  }

  record Result(Map<String, PackageDescriptor> packages, List<PackageDiagnostic> diagnostics) {
    Result {
      packages = Collections.unmodifiableMap(new LinkedHashMap<>(packages));
      diagnostics = List.copyOf(diagnostics);
    }
  }
}

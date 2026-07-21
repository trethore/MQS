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

import io.github.trethore.myqolpackages.api.packages.PackageDiscoveryResult;
import io.github.trethore.myqolpackages.api.packages.PackageInfo;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class DefaultPackageManager implements PackageManager {
  private final Path packageDirectory;
  private final FileSystemPackageDiscovery packageDiscovery;

  private final AtomicReference<Map<String, PackageDescriptor>> packages =
      new AtomicReference<>(Map.of());

  public DefaultPackageManager(Path packageDirectory, FileSystemPackageDiscovery packageDiscovery) {
    this.packageDirectory = Objects.requireNonNull(packageDirectory, "packageDirectory");
    this.packageDiscovery = Objects.requireNonNull(packageDiscovery, "packageDiscovery");
  }

  @Override
  public synchronized PackageDiscoveryResult refresh() {
    PackageDiscoverySnapshot snapshot = packageDiscovery.discover(packageDirectory);
    Map<String, PackageDescriptor> discoveredPackages = new LinkedHashMap<>();
    for (PackageDescriptor descriptor : snapshot.packages()) {
      discoveredPackages.put(descriptor.id(), descriptor);
    }
    packages.set(Collections.unmodifiableMap(discoveredPackages));
    return new PackageDiscoveryResult(getPackages(), snapshot.diagnostics());
  }

  @Override
  public List<PackageInfo> getPackages() {
    return packages.get().values().stream().map(PackageDescriptor::toInfo).toList();
  }

  @Override
  public Optional<PackageInfo> findPackage(String id) {
    PackageDescriptor descriptor = packages.get().get(id);
    return descriptor == null ? Optional.empty() : Optional.of(descriptor.toInfo());
  }
}

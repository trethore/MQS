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
package io.github.trethore.myqolpackages.internal.runtime;

import io.github.trethore.myqolpackages.api.config.FileSystemPermissionOverrides;
import io.github.trethore.myqolpackages.api.config.FileSystemPermissions;
import io.github.trethore.myqolpackages.api.config.MqpPermissionsConfig;
import io.github.trethore.myqolpackages.api.config.PackagePermissionOverrides;
import io.github.trethore.myqolpackages.api.config.PackagePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PackagePermissionResolver {
  private PackagePermissionResolver() {}

  public static PackagePermissions resolve(
      String packageId, PackagePermissions requested, MqpPermissionsConfig configuration)
      throws PackageLifecycleException {
    PackagePermissions granted = resolveGrants(packageId, configuration);
    List<String> deniedPermissions = findDeniedPermissions(requested, granted);
    if (!deniedPermissions.isEmpty()) {
      throw new PackageLifecycleException(
          "Package permissions were not granted: " + String.join(", ", deniedPermissions));
    }
    return requested;
  }

  private static PackagePermissions resolveGrants(
      String packageId, MqpPermissionsConfig configuration) {
    PackagePermissions grants = applyOverrides(PackagePermissions.none(), configuration.defaults());
    PackagePermissionOverrides packageOverrides = configuration.packages().get(packageId);
    return packageOverrides == null ? grants : applyOverrides(grants, packageOverrides);
  }

  private static PackagePermissions applyOverrides(
      PackagePermissions permissions, PackagePermissionOverrides overrides) {
    FileSystemPermissions filesystem = permissions.filesystem();
    FileSystemPermissionOverrides filesystemOverrides = overrides.filesystem();
    if (filesystemOverrides != null) {
      filesystem =
          new FileSystemPermissions(
              filesystemOverrides.read() == null ? filesystem.read() : filesystemOverrides.read(),
              filesystemOverrides.write() == null
                  ? filesystem.write()
                  : filesystemOverrides.write());
    }
    return new PackagePermissions(
        overrides.hostAccess() == null ? permissions.hostAccess() : overrides.hostAccess(),
        overrides.hostClassLookup() == null
            ? permissions.hostClassLookup()
            : overrides.hostClassLookup(),
        filesystem);
  }

  private static List<String> findDeniedPermissions(
      PackagePermissions requested, PackagePermissions granted) {
    List<String> denied = new ArrayList<>();
    if (!granted.hostAccess().allows(requested.hostAccess())) {
      addDeniedPermission(denied, "hostAccess", requested.hostAccess(), granted.hostAccess());
    }
    if (!granted.hostClassLookup().allows(requested.hostClassLookup())) {
      addDeniedPermission(
          denied, "hostClassLookup", requested.hostClassLookup(), granted.hostClassLookup());
    }
    if (!granted.filesystem().allowsRead(requested.filesystem().read())) {
      addDeniedPermission(
          denied, "filesystem.read", requested.filesystem().read(), granted.filesystem().read());
    }
    if (!granted.filesystem().write().allows(requested.filesystem().write())) {
      addDeniedPermission(
          denied, "filesystem.write", requested.filesystem().write(), granted.filesystem().write());
    }
    return denied;
  }

  private static void addDeniedPermission(
      List<String> denied, String name, Enum<?> requested, Enum<?> granted) {
    denied.add(
        name + "=" + permissionName(requested) + " (granted " + permissionName(granted) + ")");
  }

  private static String permissionName(Enum<?> permission) {
    return permission.name().toLowerCase(Locale.ROOT);
  }
}

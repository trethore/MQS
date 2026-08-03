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
package io.github.trethore.myqolpackages.api.config;

public record PackagePermissions(
    HostAccessPermission hostAccess,
    HostClassLookupPermission hostClassLookup,
    FileSystemPermissions filesystem,
    InternetPermissions internet) {
  private static final PackagePermissions NONE =
      new PackagePermissions(
          HostAccessPermission.NONE,
          HostClassLookupPermission.NONE,
          FileSystemPermissions.none(),
          InternetPermissions.none());

  public PackagePermissions(
      HostAccessPermission hostAccess,
      HostClassLookupPermission hostClassLookup,
      FileSystemPermissions filesystem) {
    this(hostAccess, hostClassLookup, filesystem, InternetPermissions.none());
  }

  public PackagePermissions {
    hostAccess = hostAccess == null ? HostAccessPermission.NONE : hostAccess;
    hostClassLookup = hostClassLookup == null ? HostClassLookupPermission.NONE : hostClassLookup;
    filesystem = filesystem == null ? FileSystemPermissions.none() : filesystem;
    internet = internet == null ? InternetPermissions.none() : internet;
  }

  public static PackagePermissions none() {
    return NONE;
  }
}

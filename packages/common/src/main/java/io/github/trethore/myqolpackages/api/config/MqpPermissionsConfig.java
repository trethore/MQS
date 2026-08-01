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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record MqpPermissionsConfig(
    PackagePermissionOverrides defaults, Map<String, PackagePermissionOverrides> packages) {
  private static final MqpPermissionsConfig RESTRICTED =
      new MqpPermissionsConfig(
          new PackagePermissionOverrides(
              HostAccessPermission.NONE,
              HostClassLookupPermission.NONE,
              new FileSystemPermissionOverrides(
                  FileSystemReadPermission.NONE, FileSystemWritePermission.NONE)),
          Map.of());

  public MqpPermissionsConfig {
    defaults = defaults == null ? new PackagePermissionOverrides(null, null, null) : defaults;
    packages =
        packages == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(packages));
  }

  public static MqpPermissionsConfig restricted() {
    return RESTRICTED;
  }
}

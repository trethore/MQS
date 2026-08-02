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

import java.util.List;

public record MqpConfig(
    List<String> additionalPackageRoots,
    List<String> enabledPackages,
    MqpPermissionsConfig permissions) {
  public MqpConfig {
    additionalPackageRoots =
        additionalPackageRoots == null ? List.of() : additionalPackageRoots.stream().toList();
    enabledPackages =
        enabledPackages == null
            ? List.of()
            : enabledPackages.stream()
                .filter(packageId -> packageId != null && !packageId.isBlank())
                .distinct()
                .toList();
    permissions = permissions == null ? MqpPermissionsConfig.restricted() : permissions;
  }

  public MqpConfig(List<String> additionalPackageRoots, List<String> enabledPackages) {
    this(additionalPackageRoots, enabledPackages, MqpPermissionsConfig.restricted());
  }

  public static MqpConfig defaults() {
    return new MqpConfig(List.of(), List.of());
  }
}

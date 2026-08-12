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
        int configVersion, List<String> additionalPackageRoots, List<String> enabledPackages, TrustConfig trust) {
    public static final int CURRENT_CONFIG_VERSION = 2;

    public MqpConfig {
        if (configVersion == 0) {
            configVersion = CURRENT_CONFIG_VERSION;
        } else if (configVersion != CURRENT_CONFIG_VERSION) {
            throw new IllegalArgumentException("Unsupported config version: " + configVersion);
        }
        additionalPackageRoots = additionalPackageRoots == null
                ? List.of()
                : additionalPackageRoots.stream().toList();
        enabledPackages = enabledPackages == null
                ? List.of()
                : enabledPackages.stream()
                        .filter(packageId -> packageId != null && !packageId.isBlank())
                        .distinct()
                        .toList();
        trust = trust == null ? TrustConfig.defaults() : trust;
    }

    public static MqpConfig defaults() {
        return new MqpConfig(CURRENT_CONFIG_VERSION, List.of(), List.of(), TrustConfig.defaults());
    }

    public MqpConfig withEnabledPackages(List<String> updatedEnabledPackages) {
        return new MqpConfig(configVersion, additionalPackageRoots, updatedEnabledPackages, trust);
    }

    public MqpConfig withTrust(TrustConfig updatedTrust) {
        return new MqpConfig(configVersion, additionalPackageRoots, enabledPackages, updatedTrust);
    }
}

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
package io.github.trethore.myqolpackages.api.packages.management;

import io.github.trethore.myqolpackages.api.MqpDiagnostic;
import io.github.trethore.myqolpackages.api.packages.PackageInfo;
import java.util.List;

public record PackageDiscoveryResult(List<PackageInfo> packages, List<MqpDiagnostic> diagnostics) {
    public PackageDiscoveryResult {
        packages = List.copyOf(packages);
        diagnostics = List.copyOf(diagnostics);
    }
}

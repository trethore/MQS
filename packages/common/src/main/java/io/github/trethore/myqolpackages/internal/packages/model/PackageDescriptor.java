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
package io.github.trethore.myqolpackages.internal.packages.model;

import io.github.trethore.myqolpackages.api.packages.PackageInfo;
import io.github.trethore.myqolpackages.api.packages.PackageState;
import io.github.trethore.myqolpackages.api.packages.trust.PackageTrustInfo;
import io.github.trethore.myqolpackages.internal.trust.SemanticVersion;
import java.nio.file.Path;

public record PackageDescriptor(
        String id, Path packageDirectory, Path entrypoint, PackageManifest manifest, SemanticVersion semanticVersion) {
    public PackageDescriptor(String id, Path packageDirectory, Path entrypoint, PackageManifest manifest) {
        this(id, packageDirectory, entrypoint, manifest, SemanticVersion.parse(manifest.version()));
    }

    public Path dataDirectory() {
        return PackageDirectories.resolveDataDirectory(packageDirectory, id);
    }

    public PackageInfo toInfo(PackageState state, PackageTrustInfo trustInfo) {
        return new PackageInfo(
                id,
                manifest.name(),
                manifest.description(),
                manifest.version(),
                manifest.entrypoint(),
                packageDirectory,
                state,
                trustInfo);
    }
}

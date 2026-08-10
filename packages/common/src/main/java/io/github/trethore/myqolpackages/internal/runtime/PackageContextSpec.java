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

import java.nio.file.Path;
import java.util.Objects;

public record PackageContextSpec(String packageId, Path packageDirectory, Path entrypoint, Path dataDirectory) {
    public PackageContextSpec {
        Objects.requireNonNull(packageId, "packageId");
        Objects.requireNonNull(packageDirectory, "packageDirectory");
        Objects.requireNonNull(entrypoint, "entrypoint");
        Objects.requireNonNull(dataDirectory, "dataDirectory");
    }
}

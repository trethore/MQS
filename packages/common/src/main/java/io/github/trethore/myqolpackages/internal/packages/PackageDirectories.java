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

import java.nio.file.Path;

final class PackageDirectories {
  static final String DATA_DIRECTORY_NAME = "package-data";

  private PackageDirectories() {}

  static Path resolveDataDirectory(Path packageDirectory, String packageId) {
    Path normalizedPackageDirectory = packageDirectory.toAbsolutePath().normalize();
    Path packageRoot = normalizedPackageDirectory.getParent();
    if (packageRoot == null) {
      throw new IllegalArgumentException("Package directory must have a parent directory");
    }
    return packageRoot.resolve(DATA_DIRECTORY_NAME).resolve(packageId).normalize();
  }
}

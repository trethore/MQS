/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
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

package net.me.utils;

import net.me.Main;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public final class PathUtils {
    private PathUtils() {
    }

    public static String expandHomeDirectory(String path) {
        if (!path.startsWith("~")) {
            return path;
        }

        String homeDirectory = System.getProperty("user.home");
        if (homeDirectory == null || homeDirectory.isBlank()) {
            return path;
        }

        if (path.equals("~")) {
            return homeDirectory;
        }

        if (startsWithHomePrefix(path)) {
            return homeDirectory + path.substring(1);
        }

        return path;
    }

    private static boolean startsWithHomePrefix(String path) {
        return path.startsWith("~" + File.separator)
                || path.startsWith("~/")
                || path.startsWith("~\\");
    }

    public static String formatPathRelativeToModDir(Path path) {
        Path normalizedOutput = path.toAbsolutePath().normalize();
        Path normalizedModDir = Main.MOD_DIR.toAbsolutePath().normalize();
        if (normalizedOutput.startsWith(normalizedModDir)) {
            Path relativePath = normalizedModDir.relativize(normalizedOutput);
            return Main.MOD_ID + "/" + relativePath.toString().replace('\\', '/');
        }
        return normalizedOutput.toString();
    }

    public static Path resolvePathFromModDir(String path) {
        if (path == null) {
            throw new InvalidPathException("null", "Path cannot be null.");
        }

        Path resolvedPath = Path.of(expandHomeDirectory(path));
        if (!resolvedPath.isAbsolute()) {
            resolvedPath = Main.MOD_DIR.resolve(resolvedPath);
        }
        return resolvedPath.toAbsolutePath().normalize();
    }
}

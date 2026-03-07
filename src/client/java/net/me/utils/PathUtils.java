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

import java.io.File;

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
}

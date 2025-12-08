/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
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

package net.me.ui;

import net.me.Main;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Path;

public final class UIUtils {

    private UIUtils() {
    }

    public static void openScriptsFolder() {
        Path scriptsPath = Main.MOD_DIR.resolve("scripts");
        Util.getOperatingSystem().open(scriptsPath.toUri());
    }

    public static void openInIde(String command) {
        Path scriptsPath = Main.MOD_DIR.resolve("scripts");
        try {
            new ProcessBuilder(command, scriptsPath.toString()).start();
        } catch (IOException e) {
            if (Util.getOperatingSystem() == Util.OperatingSystem.WINDOWS) {
                try {
                    new ProcessBuilder("cmd", "/c", command, scriptsPath.toString()).start();
                    return;
                } catch (IOException ignored) {
                    // fall through to logging below
                }
            }
            Main.LOGGER.error("Failed to open scripts folder in IDE command '{}'", command, e);
        }
    }
}

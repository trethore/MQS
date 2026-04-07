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
import net.minecraft.client.Minecraft;

public final class VscodeWebUtils {
    public static final String CODE_EDITOR_URL = "https://vscode.dev/";

    private VscodeWebUtils() {
    }

    public static String getModDirPath() {
        return Main.MOD_DIR.toAbsolutePath().normalize().toString();
    }

    public static boolean copyModDirToClipboard(Minecraft minecraft) {
        try {
            minecraft.keyboardHandler.setClipboard(getModDirPath());
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}

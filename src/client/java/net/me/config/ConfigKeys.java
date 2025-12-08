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

package net.me.config;

public final class ConfigKeys {

    // Global Config (mqs_config.json)
    public static final String LOG_REDIRECT = "logRedirect";
    public static final String ALLOW_ALL_CLASSES = "allowAllClasses";
    public static final String ADDITIONAL_SCRIPT_DIRS = "additionalScriptDirs";
    public static final String DEFAULT_IDE_COMMAND = "defaultIdeCommand";

    // Per-script configs (in /configs/)
    public static final String ENABLED = "enabled";
    public static final String KEYBINDS = "keybinds";

    // Script Metadata (@module)
    public static final String SCRIPT_META_MAIN = "main";
    public static final String SCRIPT_META_NAME = "name";
    public static final String SCRIPT_META_VERSION = "version";


    private ConfigKeys() {
    }
}

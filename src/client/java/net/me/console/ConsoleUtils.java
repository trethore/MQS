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

package net.me.console;

import java.util.Optional;

public class ConsoleUtils {

    public static final String TRUE_STRING = "true";
    public static final String FALSE_STRING = "false";
    private ConsoleUtils() {
    }

    public static Optional<Boolean> parseBooleanArg(String arg) {
        if (TRUE_STRING.equalsIgnoreCase(arg)) return Optional.of(true);
        if (FALSE_STRING.equalsIgnoreCase(arg)) return Optional.of(false);
        return Optional.empty();
    }
}

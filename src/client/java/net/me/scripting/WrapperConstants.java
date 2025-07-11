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

package net.me.scripting;

public final class WrapperConstants {

    // Internal Properties
    public static final String SELF = "_self";
    public static final String SUPER = "_super";
    public static final String CLASS = "_class";

    // Internal Methods
    public static final String EQUALS = "equals";
    public static final String INSTANCE_OF = "_instanceof";

    // JS-specific keys
    public static final String PROTOTYPE = "prototype";
    public static final String HAS_INSTANCE_SYMBOL = "Symbol(Symbol.hasInstance)";

    // Suffixes
    public static final String FIELD_SUFFIX = "$";


    private WrapperConstants() {
    }
}
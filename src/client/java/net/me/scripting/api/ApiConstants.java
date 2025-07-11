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

package net.me.scripting.api;

public final class ApiConstants {

    public static final String REGISTER = "register";
    public static final String UNREGISTER = "unregister";
    public static final String UNREGISTER_ALL = "unregisterAll";
    public static final String BUILDER = "builder";
    // Commands API
    public static final String LITERAL = "literal";
    public static final String ARGUMENT = "argument";
    public static final String ARG_TYPE = "ArgType";
    // Config API
    public static final String GET = "get";
    public static final String SET = "set";
    public static final String HAS = "has";
    public static final String SAVE = "save";
    public static final String LOAD = "load";
    public static final String GET_ALL = "getAll";
    // Event API
    public static final String EVENTS = "Events";
    public static final String PHASE = "Phase";
    // Hook API
    public static final String HOOK = "hook";
    public static final String UNHOOK = "unhook";
    public static final String UNHOOK_ALL = "unhookAll";
    // Keybind API
    public static final String KEYS = "Keys";

    private ApiConstants() {
    }
}
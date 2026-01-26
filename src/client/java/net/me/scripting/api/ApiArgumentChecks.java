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

import org.graalvm.polyglot.Value;

@SuppressWarnings("SameParameterValue")
final class ApiArgumentChecks {
    private ApiArgumentChecks() {
    }

    static void requireArgCount(Value[] args, int expected, String message) {
        if (args.length != expected) {
            throw new IllegalArgumentException(message);
        }
    }

    static void requireArgCountAtLeast(Value[] args, int min, String message) {
        if (args.length < min) {
            throw new IllegalArgumentException(message);
        }
    }

    static void requireArgCountRange(Value[] args, int min, int max, String message) {
        if (args.length < min || args.length > max) {
            throw new IllegalArgumentException(message);
        }
    }

    static String requireString(Value[] args, int index, String message) {
        Value value = requirePresent(args, index, message);
        if (!value.isString()) {
            throw new IllegalArgumentException(message);
        }
        return value.asString();
    }

    static void requireNumber(Value[] args, int index, String message) {
        Value value = requirePresent(args, index, message);
        if (!value.isNumber()) {
            throw new IllegalArgumentException(message);
        }
    }

    static Value requireExecutable(Value[] args, int index, String message) {
        Value value = requirePresent(args, index, message);
        if (!value.canExecute()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    static <T> T requireHostObject(Value[] args, int index, Class<T> type, String message) {
        Value value = requirePresent(args, index, message);
        if (!value.isHostObject()) {
            throw new IllegalArgumentException(message);
        }
        Object hostObject = value.asHostObject();
        if (!type.isInstance(hostObject)) {
            throw new IllegalArgumentException(message);
        }
        return type.cast(hostObject);
    }

    private static Value requirePresent(Value[] args, int index, String message) {
        if (args.length <= index || args[index] == null) {
            throw new IllegalArgumentException(message);
        }
        return args[index];
    }
}

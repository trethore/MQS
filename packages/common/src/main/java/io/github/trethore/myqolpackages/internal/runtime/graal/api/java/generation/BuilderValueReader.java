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
package io.github.trethore.myqolpackages.internal.runtime.graal.api.java.generation;

import org.graalvm.polyglot.Value;

final class BuilderValueReader {
    private BuilderValueReader() {}

    static Value requireDefinition(Value[] arguments, String methodName) {
        requireArgumentCount(arguments, 1, methodName);
        Value definition = arguments[0];
        if (definition == null || definition.isNull() || !definition.hasMembers()) {
            throw new IllegalArgumentException(methodName + " requires a definition object");
        }
        return definition;
    }

    static Value requireMember(Value definition, String member) {
        if (!definition.hasMember(member)) {
            throw new IllegalArgumentException("Missing required member: " + member);
        }
        return definition.getMember(member);
    }

    static String requireString(Value definition, String member) {
        Value value = requireMember(definition, member);
        if (!value.isString() || value.asString().isBlank()) {
            throw new IllegalArgumentException(member + " must be a nonblank string");
        }
        return value.asString();
    }

    static Value requireFunction(Value definition, String member) {
        Value value = requireMember(definition, member);
        if (!value.canExecute()) {
            throw new IllegalArgumentException(member + " must be a JavaScript function");
        }
        return value;
    }

    static boolean optionalBoolean(Value definition, String member, boolean defaultValue) {
        if (!definition.hasMember(member)) {
            return defaultValue;
        }
        Value value = definition.getMember(member);
        if (!value.isBoolean()) {
            throw new IllegalArgumentException(member + " must be a boolean");
        }
        return value.asBoolean();
    }

    static void requireArgumentCount(Value[] arguments, int count, String methodName) {
        if (arguments.length != count) {
            throw new IllegalArgumentException(methodName + " requires exactly " + count + " argument(s)");
        }
    }
}

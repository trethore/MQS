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

package net.me.scripting.typings;

import java.util.Set;

final class TypingsDeclarationUtils {
    private static final Set<String> TS_RESERVED_KEYWORDS = Set.of(
            "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do",
            "else", "enum", "export", "extends", "false", "finally", "for", "function", "if", "import",
            "in", "instanceof", "new", "null", "return", "super", "switch", "this", "throw", "true",
            "try", "typeof", "var", "void", "while", "with", "as", "implements", "interface", "let",
            "package", "private", "protected", "public", "static", "yield", "any", "boolean", "constructor",
            "declare", "get", "module", "require", "number", "set", "string", "symbol", "type", "from",
            "of", "readonly", "keyof", "unique", "unknown", "never", "asserts", "infer", "is", "object"
    );

    private TypingsDeclarationUtils() {
    }

    public static void appendCoreDeclarations(StringBuilder builder) {
        builder.append("interface JavaClass<T = JavaInstance> {\n");
        builder.append(TypingsFormat.INDENT).append(TypingsFormat.READONLY).append("_class: unknown;\n");
        builder.append(TypingsFormat.INDENT).append("[member: string]: any;\n");
        builder.append(TypingsFormat.BLOCK_END);

        builder.append("interface JavaInstance {\n");
        builder.append(TypingsFormat.INDENT).append(TypingsFormat.READONLY).append("_self: unknown;\n");
        builder.append(TypingsFormat.INDENT).append("_instanceof(target: JavaClass<any>): boolean;\n");
        builder.append(TypingsFormat.INDENT).append("equals(other: unknown): boolean;\n");
        builder.append(TypingsFormat.INDENT).append("[member: string]: any;\n");
        builder.append(TypingsFormat.BLOCK_END);
    }

    public static boolean isAccessibleIdentifier(String value) {
        return TypingsNamingUtils.isValidIdentifier(value) && !TS_RESERVED_KEYWORDS.contains(value);
    }
}

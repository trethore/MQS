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

import net.me.scripting.typings.schema.*;

import java.util.List;
import java.util.Set;

/**
 * Responsible for generating TypeScript declaration content for the My QOL Scripts API based on the provided schema.
 * <p>
 * This class uses the {@link MqsApiSchema} to retrieve the necessary information about the API's objects, properties,
 * and methods, and generates corresponding TypeScript declarations. The generated content is appended to a provided
 * StringBuilder, which can then be written to a .d.ts file.
 */
final class MqsApiDtsEmitter {
    private static final String INDENT = TypingsFormat.INDENT;
    private static final String READONLY = TypingsFormat.READONLY;
    private static final String BLOCK_END = TypingsFormat.BLOCK_END;
    private static final Set<String> TS_RESERVED_KEYWORDS = Set.of(
            "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do",
            "else", "enum", "export", "extends", "false", "finally", "for", "function", "if", "import",
            "in", "instanceof", "new", "null", "return", "super", "switch", "this", "throw", "true",
            "try", "typeof", "var", "void", "while", "with", "as", "implements", "interface", "let",
            "package", "private", "protected", "public", "static", "yield", "any", "boolean", "constructor",
            "declare", "get", "module", "require", "number", "set", "string", "symbol", "type", "from",
            "of", "readonly", "keyof", "unique", "unknown", "never", "asserts", "infer", "is", "object"
    );

    public void append(StringBuilder builder) {
        appendCoreDeclarations(builder);
        appendTypeAliases(builder);
        appendSchemaObjects(builder);
    }
    /**
     * Appends core TypeScript declarations and utility functions for the My QOL Scripts API.
     */
    private void appendCoreDeclarations(StringBuilder builder) {
        builder.append("type JavaClass<T = JavaInstance> = {\n");
        builder.append(INDENT).append("new (...args: any[]): T;\n");
        builder.append(INDENT).append(READONLY).append("_class: unknown;\n");
        builder.append(INDENT).append("[member: string]: any;\n");
        builder.append("};\n\n");

        builder.append("interface JavaInstance {\n");
        builder.append(INDENT).append(READONLY).append("_self: unknown;\n");
        builder.append(INDENT).append("_instanceof(target: JavaClass<any>): boolean;\n");
        builder.append(INDENT).append("equals(other: unknown): boolean;\n");
        builder.append(INDENT).append("[member: string]: any;\n");
        builder.append(BLOCK_END);

        builder.append("type MQSDisposer = () => void;\n");
        builder.append("type MQSAnyFunction = (...args: any[]) => unknown;\n\n");

        builder.append("interface MQSExtendMappedConfig {\n");
        builder.append(INDENT).append("extends: unknown;\n");
        builder.append(INDENT).append("implements?: unknown | unknown[];\n");
        builder.append(BLOCK_END);

        builder.append("declare function importClass<K extends keyof MQSClassRegistry>(name: K): MQSClassRegistry[K];\n");
        builder.append("declare function importClass(name: string): JavaClass<any>;\n");
        builder.append("declare function wrap<T = JavaInstance>(instance: unknown): T;\n");
        builder.append("declare function extendMapped(config: MQSExtendMappedConfig, implementation: Record<string, unknown>): JavaClass<any>;\n");
        builder.append("declare function exportModule(...modules: (JavaClass<any> | JavaClass<any>[])[]): void;\n");
        builder.append("declare function isInstanceOf(instance: unknown, clazz: JavaClass<any>): boolean;\n");
        builder.append("declare function print(...args: unknown[]): void;\n");
        builder.append("declare function println(...args: unknown[]): void;\n");
        builder.append("declare const MQS: MQSApi;\n\n");
    }

    /**
     * Appends type aliases for common types used in the My QOL Scripts API.
     */
    private void appendTypeAliases(StringBuilder builder) {
        builder.append("type MQSEventPhase = \"PRE\" | \"POST\";\n");
        builder.append("type MQSEventCallback<T = any> = (event: T) => unknown;\n");
        builder.append("type MQSEventOptionsLike = MQSEventSubscriptionOptions | MQSEventOptionsBuilder | MQSEventPhase | string;\n");
        builder.append("type MQSCommandSuggestions = string | string[] | Iterable<unknown> | Promise<string | string[] | Iterable<unknown> | null> | null;\n");
        builder.append("type MQSCommandSuggestionProvider = ((context: MQSCommandContext) => MQSCommandSuggestions) | MQSCommandSuggestions;\n");
        builder.append("type MQSHookMode = \"BEFORE\" | \"AFTER\" | \"INSTEAD\" | \"before\" | \"after\" | \"instead\";\n");
        builder.append("type MQSHookHandler = (...args: any[]) => unknown;\n\n");
    }

    private void appendSchemaObjects(StringBuilder builder) {
        for (TsObject object : MqsApiSchema.objects()) {
            appendObject(builder, object);
        }
    }

    private void appendObject(StringBuilder builder, TsObject object) {
        builder.append("interface ").append(object.name()).append(" {\n");
        for (TsMember member : object.members()) {
            switch (member) {
                case TsProperty property -> appendProperty(builder, property);
                case TsMethod method -> appendMethod(builder, method);
            }
        }
        builder.append(BLOCK_END);
    }

    private void appendProperty(StringBuilder builder, TsProperty property) {
        String memberName = renderMemberName(property.name());
        builder.append(INDENT);
        if (property.readOnly()) {
            builder.append(READONLY);
        }
        builder.append(memberName);
        if (property.optional() && !isIndexSignature(memberName)) {
            builder.append("?");
        }
        builder.append(": ").append(property.type()).append(";\n");
    }

    private void appendMethod(StringBuilder builder, TsMethod method) {
        for (TsFunction overload : method.overloads()) {
            builder.append(INDENT)
                    .append(renderMemberName(method.name()))
                    .append("(")
                    .append(renderParams(overload.params()))
                    .append("): ")
                    .append(overload.returnType())
                    .append(";\n");
        }
    }

    private String renderParams(List<TsParam> params) {
        if (params.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < params.size(); index++) {
            TsParam parameter = params.get(index);
            if (index > 0) {
                builder.append(", ");
            }
            if (parameter.rest()) {
                builder.append("...");
            }
            builder.append(parameter.name());
            if (parameter.optional() && !parameter.rest()) {
                builder.append("?");
            }
            builder.append(": ").append(parameter.type());
        }
        return builder.toString();
    }

    private String renderMemberName(String memberName) {
        if (isIndexSignature(memberName)) {
            return memberName;
        }
        if (TypingsNamingUtils.isValidIdentifier(memberName) && !TS_RESERVED_KEYWORDS.contains(memberName)) {
            return memberName;
        }
        return "'" + TypingsNamingUtils.escapeSingleQuotedString(memberName) + "'";
    }

    private boolean isIndexSignature(String memberName) {
        return memberName.startsWith("[") && memberName.endsWith("]");
    }
}

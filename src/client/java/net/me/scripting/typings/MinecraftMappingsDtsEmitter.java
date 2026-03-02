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

import net.me.scripting.WrapperConstants;

import java.util.*;

final class MinecraftMappingsDtsEmitter {
    private static final String INDENT = TypingsFormat.INDENT;
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

    public void append(
            StringBuilder builder,
            Set<String> classNames,
            Map<String, Map<String, List<String>>> methodMap,
            Map<String, Map<String, String>> fieldMap
    ) {
        NamespaceNode rootNode = buildNamespaceTree(classNames);
        appendNamespaceDeclarations(builder, rootNode, "", 0, methodMap, fieldMap);
        appendClassRegistry(builder, classNames);
    }

    private NamespaceNode buildNamespaceTree(Set<String> classNames) {
        NamespaceNode root = new NamespaceNode();

        for (String className : classNames) {
            String[] parts = className.split("\\.");
            if (parts.length < 2) {
                continue;
            }

            NamespaceNode current = root;
            for (int index = 0; index < parts.length - 1; index++) {
                current = current.children.computeIfAbsent(parts[index], ignored -> new NamespaceNode());
            }
            current.classes.add(parts[parts.length - 1]);
        }

        return root;
    }

    private void appendNamespaceDeclarations(
            StringBuilder builder,
            NamespaceNode current,
            String parentPackage,
            int depth,
            Map<String, Map<String, List<String>>> methodMap,
            Map<String, Map<String, String>> fieldMap
    ) {
        for (Map.Entry<String, NamespaceNode> childEntry : current.children.entrySet()) {
            String namespaceName = childEntry.getKey();
            NamespaceNode namespaceNode = childEntry.getValue();
            String packageName = parentPackage.isEmpty() ? namespaceName : parentPackage + "." + namespaceName;

            appendIndent(builder, depth);
            builder.append("declare namespace ").append(namespaceName).append(" {\n");

            appendClassDeclarations(builder, packageName, namespaceNode.classes, depth + 1, methodMap, fieldMap);
            appendNamespaceDeclarations(builder, namespaceNode, packageName, depth + 1, methodMap, fieldMap);

            appendIndent(builder, depth);
            builder.append(BLOCK_END);
        }
    }

    private void appendClassDeclarations(
            StringBuilder builder,
            String packageName,
            Set<String> classes,
            int depth,
            Map<String, Map<String, List<String>>> methodMap,
            Map<String, Map<String, String>> fieldMap
    ) {
        for (String className : classes) {
            String fullClassName = packageName + "." + className;
            String instanceTypeName = className + "$Instance";

            appendIndent(builder, depth);
            builder.append("const ").append(className).append(": JavaClass<").append(instanceTypeName).append(">;\n");

            appendIndent(builder, depth);
            builder.append("interface ").append(instanceTypeName).append(" extends JavaInstance {\n");

            Map<String, List<String>> classMethods = methodMap.getOrDefault(fullClassName, Collections.emptyMap());
            Map<String, String> classFields = fieldMap.getOrDefault(fullClassName, Collections.emptyMap());
            appendMappedMembers(builder, classMethods, classFields, depth + 1);

            appendIndent(builder, depth);
            builder.append(BLOCK_END);
        }
    }

    private void appendMappedMembers(
            StringBuilder builder,
            Map<String, List<String>> classMethods,
            Map<String, String> classFields,
            int depth
    ) {
        Set<String> methodNames = new TreeSet<>();
        for (String methodName : classMethods.keySet()) {
            if (isSupportedMemberName(methodName)) {
                methodNames.add(methodName);
            }
        }

        Set<String> fieldNames = new TreeSet<>();
        for (String fieldName : classFields.keySet()) {
            if (isSupportedMemberName(fieldName)) {
                fieldNames.add(fieldName);
            }
        }

        for (String methodName : methodNames) {
            appendIndent(builder, depth);
            builder.append(renderMemberName(methodName)).append("(...args: any[]): any;\n");
        }

        for (String fieldName : fieldNames) {
            if (!methodNames.contains(fieldName)) {
                appendIndent(builder, depth);
                builder.append(renderMemberName(fieldName)).append(": any;\n");
            }

            String explicitFieldName = fieldName + WrapperConstants.FIELD_SUFFIX;
            if (!methodNames.contains(explicitFieldName)) {
                appendIndent(builder, depth);
                builder.append(renderMemberName(explicitFieldName)).append(": any;\n");
            }
        }
    }

    private void appendClassRegistry(StringBuilder builder, Set<String> classNames) {
        builder.append("interface MQSClassRegistry {\n");

        for (String className : new TreeSet<>(classNames)) {
            builder.append(INDENT)
                    .append("\"")
                    .append(className)
                    .append("\": typeof ")
                    .append(buildTypeAccess(className))
                    .append(";\n");
        }

        builder.append("}\n");
    }

    private String buildTypeAccess(String className) {
        String[] parts = className.split("\\.");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "globalThis";
        }

        StringBuilder builder = new StringBuilder(resolveRootAccess(parts[0]));
        for (int index = 1; index < parts.length; index++) {
            builder.append(resolveChildAccess(parts[index]));
        }

        return builder.toString();
    }

    private String resolveRootAccess(String part) {
        if (isAccessibleIdentifier(part)) {
            return part;
        }
        return "globalThis[" + TypingsNamingUtils.renderDoubleQuotedLiteral(part) + "]";
    }

    private String resolveChildAccess(String part) {
        if (isAccessibleIdentifier(part)) {
            return "." + part;
        }
        return "[" + TypingsNamingUtils.renderDoubleQuotedLiteral(part) + "]";
    }

    private boolean isAccessibleIdentifier(String value) {
        return TypingsNamingUtils.isValidIdentifier(value) && !TS_RESERVED_KEYWORDS.contains(value);
    }

    private String renderMemberName(String memberName) {
        if (TypingsNamingUtils.isValidIdentifier(memberName) && !TS_RESERVED_KEYWORDS.contains(memberName)) {
            return memberName;
        }
        return "'" + TypingsNamingUtils.escapeSingleQuotedString(memberName) + "'";
    }

    private boolean isSupportedMemberName(String memberName) {
        if (memberName == null || memberName.isBlank()) {
            return false;
        }
        return !memberName.startsWith("<") && !memberName.endsWith(">");
    }

    private void appendIndent(StringBuilder builder, int depth) {
        if (depth > 0) {
            builder.append(INDENT.repeat(depth));
        }
    }

    private static final class NamespaceNode {
        private final Map<String, NamespaceNode> children = new TreeMap<>();
        private final Set<String> classes = new TreeSet<>();
    }
}

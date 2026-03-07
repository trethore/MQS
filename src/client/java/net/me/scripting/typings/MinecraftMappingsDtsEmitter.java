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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
    private Map<String, String> currentClassMap = Collections.emptyMap();
    private Map<String, String> reflectedNamedClassesCache = Collections.emptyMap();

    public void append(
            StringBuilder builder,
            Map<String, String> classMap,
            Map<String, Map<String, List<String>>> methodMap,
            Map<String, Map<String, String>> fieldMap
    ) {
        this.currentClassMap = classMap;
        this.reflectedNamedClassesCache = new HashMap<>();
        appendCoreDeclarations(builder);
        Map<String, ReflectedClassMembers> reflectedMembers = buildReflectedMembers(classMap, methodMap, fieldMap);
        Set<String> classNames = classMap.keySet();
        NamespaceNode rootNode = buildNamespaceTree(classNames);
        appendNamespaceDeclarations(builder, rootNode, "", 0, methodMap, fieldMap, reflectedMembers);
        appendClassRegistry(builder, classNames);
    }

    private void appendCoreDeclarations(StringBuilder builder) {
        builder.append("interface JavaClass<T = JavaInstance> {\n");
        builder.append(INDENT).append("new (...args: any[]): T;\n");
        builder.append(INDENT).append("readonly _class: unknown;\n");
        builder.append(INDENT).append("[member: string]: any;\n");
        builder.append(BLOCK_END);

        builder.append("interface JavaInstance {\n");
        builder.append(INDENT).append("readonly _self: unknown;\n");
        builder.append(INDENT).append("_instanceof(target: JavaClass<any>): boolean;\n");
        builder.append(INDENT).append("equals(other: unknown): boolean;\n");
        builder.append(INDENT).append("[member: string]: any;\n");
        builder.append(BLOCK_END);
    }

    private NamespaceNode buildNamespaceTree(Set<String> classNames) {
        NamespaceNode root = new NamespaceNode();

        for (String className : classNames) {
            String[] parts = className.split("\\.");
            if (parts.length < 2 || !hasDeclarableNamespace(parts)) {
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
            Map<String, Map<String, String>> fieldMap,
            Map<String, ReflectedClassMembers> reflectedMembers
    ) {
        for (Map.Entry<String, NamespaceNode> childEntry : current.children.entrySet()) {
            String namespaceName = childEntry.getKey();
            NamespaceNode namespaceNode = childEntry.getValue();
            String packageName = parentPackage.isEmpty() ? namespaceName : parentPackage + "." + namespaceName;

            appendIndent(builder, depth);
            if (depth == 0) {
                builder.append("declare namespace ");
            } else {
                builder.append("namespace ");
            }
            builder.append(namespaceName).append(" {\n");

            appendClassDeclarations(builder, packageName, namespaceNode.classes, depth + 1, methodMap, fieldMap, reflectedMembers);
            appendNamespaceDeclarations(builder, namespaceNode, packageName, depth + 1, methodMap, fieldMap, reflectedMembers);

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
            Map<String, Map<String, String>> fieldMap,
            Map<String, ReflectedClassMembers> reflectedMembers
    ) {
        for (String className : classes) {
            if (!isAccessibleIdentifier(className)) {
                continue;
            }

            String fullClassName = packageName + "." + className;
            String instanceTypeName = className + "$Instance";
            String staticTypeName = className + "$Static";
            ReflectedClassMembers reflectionData = reflectedMembers.getOrDefault(fullClassName, ReflectedClassMembers.empty());
            Set<String> staticMethodNames = reflectionData.available() ? reflectionData.staticMethodNames() : Collections.emptySet();
            Set<String> staticFieldNames = reflectionData.available() ? reflectionData.staticFieldNames() : Collections.emptySet();
            Set<String> instanceMethodNames = reflectionData.available() ? reflectionData.instanceMethodNames() : null;
            Set<String> instanceFieldNames = reflectionData.available() ? reflectionData.instanceFieldNames() : null;

            appendIndent(builder, depth);
            builder.append("interface ").append(staticTypeName).append(" extends JavaClass<").append(instanceTypeName).append("> {\n");

            Map<String, List<String>> classMethods = methodMap.getOrDefault(fullClassName, Collections.emptyMap());
            Map<String, String> classFields = fieldMap.getOrDefault(fullClassName, Collections.emptyMap());
            appendMappedMembers(builder, classMethods, classFields, staticMethodNames, staticFieldNames, reflectionData.staticMethodTypes(), reflectionData.staticFieldTypes(), depth + 1);

            appendIndent(builder, depth);
            builder.append(BLOCK_END);

            appendIndent(builder, depth);
            builder.append("const ").append(className).append(": ").append(staticTypeName).append(";\n");

            appendIndent(builder, depth);
            builder.append("interface ").append(instanceTypeName).append(" extends JavaInstance {\n");

            appendMappedMembers(builder, classMethods, classFields, instanceMethodNames, instanceFieldNames, reflectionData.instanceMethodTypes(), reflectionData.instanceFieldTypes(), depth + 1);

            appendIndent(builder, depth);
            builder.append(BLOCK_END);
        }
    }

    private void appendMappedMembers(
            StringBuilder builder,
            Map<String, List<String>> classMethods,
            Map<String, String> classFields,
            Set<String> allowedMethodNames,
            Set<String> allowedFieldNames,
            Map<String, String> methodTypes,
            Map<String, String> fieldTypes,
            int depth
    ) {
        Set<String> methodNames = new TreeSet<>();
        for (String methodName : classMethods.keySet()) {
            if (isSupportedMemberName(methodName) && shouldIncludeMember(methodName, allowedMethodNames)) {
                methodNames.add(methodName);
            }
        }

        Set<String> fieldNames = new TreeSet<>();
        for (String fieldName : classFields.keySet()) {
            if (isSupportedMemberName(fieldName) && shouldIncludeMember(fieldName, allowedFieldNames)) {
                fieldNames.add(fieldName);
            }
        }

        for (String methodName : methodNames) {
            appendIndent(builder, depth);
            builder.append(renderMemberName(methodName))
                    .append("(...args: any[]): ")
                    .append(methodTypes.getOrDefault(methodName, "any"))
                    .append(";\n");
        }

        for (String fieldName : fieldNames) {
            if (!methodNames.contains(fieldName)) {
                appendIndent(builder, depth);
                builder.append(renderMemberName(fieldName))
                        .append(": ")
                        .append(fieldTypes.getOrDefault(fieldName, "any"))
                        .append(";\n");
            }

            String explicitFieldName = fieldName + WrapperConstants.FIELD_SUFFIX;
            if (!methodNames.contains(explicitFieldName)) {
                appendIndent(builder, depth);
                builder.append(renderMemberName(explicitFieldName))
                        .append(": ")
                        .append(fieldTypes.getOrDefault(fieldName, "any"))
                        .append(";\n");
            }
        }
    }

    private boolean shouldIncludeMember(String memberName, Set<String> allowedMemberNames) {
        return allowedMemberNames == null || allowedMemberNames.contains(memberName);
    }

    private Map<String, ReflectedClassMembers> buildReflectedMembers(
            Map<String, String> classMap,
            Map<String, Map<String, List<String>>> methodMap,
            Map<String, Map<String, String>> fieldMap
    ) {
        Map<String, ReflectedClassMembers> reflectedMembers = new HashMap<>();
        ClassLoader classLoader = MinecraftMappingsDtsEmitter.class.getClassLoader();

        for (Map.Entry<String, String> classEntry : classMap.entrySet()) {
            String namedClassName = classEntry.getKey();
            String runtimeClassName = classEntry.getValue();
            reflectedMembers.put(
                    namedClassName,
                    reflectMembers(runtimeClassName, methodMap.get(namedClassName), fieldMap.get(namedClassName), classLoader)
            );
        }

        return reflectedMembers;
    }

    private ReflectedClassMembers reflectMembers(
            String runtimeClassName,
            Map<String, List<String>> classMethods,
            Map<String, String> classFields,
            ClassLoader classLoader
    ) {
        if (runtimeClassName == null || runtimeClassName.isBlank()) {
            return ReflectedClassMembers.empty();
        }

        try {
            Class<?> runtimeClass = Class.forName(runtimeClassName, false, classLoader);
            return new ReflectedClassMembers(
                    true,
                    collectMethodNames(runtimeClass, classMethods, true),
                    collectMethodNames(runtimeClass, classMethods, false),
                    collectFieldNames(runtimeClass, classFields, true),
                    collectFieldNames(runtimeClass, classFields, false),
                    collectMethodTypes(runtimeClass, classMethods, true),
                    collectMethodTypes(runtimeClass, classMethods, false),
                    collectFieldTypes(runtimeClass, classFields, true),
                    collectFieldTypes(runtimeClass, classFields, false)
            );
        } catch (LinkageError | ClassNotFoundException ignored) {
            return ReflectedClassMembers.empty();
        }
    }

    private Set<String> collectMethodNames(Class<?> runtimeClass, Map<String, List<String>> classMethods, boolean staticMembers) {
        if (classMethods == null || classMethods.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> runtimeMethodNames = new HashSet<>();
        for (Method method : runtimeClass.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) == staticMembers) {
                runtimeMethodNames.add(method.getName());
            }
        }

        Set<String> matchingNames = new HashSet<>();
        for (Map.Entry<String, List<String>> methodEntry : classMethods.entrySet()) {
            for (String runtimeName : methodEntry.getValue()) {
                if (runtimeMethodNames.contains(runtimeName)) {
                    matchingNames.add(methodEntry.getKey());
                    break;
                }
            }
        }
        return matchingNames;
    }

    private Set<String> collectFieldNames(Class<?> runtimeClass, Map<String, String> classFields, boolean staticMembers) {
        if (classFields == null || classFields.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> runtimeFieldNames = new HashSet<>();
        for (Field field : runtimeClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) == staticMembers) {
                runtimeFieldNames.add(field.getName());
            }
        }

        Set<String> matchingNames = new HashSet<>();
        for (Map.Entry<String, String> fieldEntry : classFields.entrySet()) {
            if (runtimeFieldNames.contains(fieldEntry.getValue())) {
                matchingNames.add(fieldEntry.getKey());
            }
        }
        return matchingNames;
    }

    private Map<String, String> collectMethodTypes(
            Class<?> runtimeClass,
            Map<String, List<String>> classMethods,
            boolean staticMembers
    ) {
        if (classMethods == null || classMethods.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Set<String>> runtimeMethodTypes = new HashMap<>();
        for (Method method : runtimeClass.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) == staticMembers) {
                runtimeMethodTypes.computeIfAbsent(method.getName(), ignored -> new TreeSet<>())
                        .add(renderJavaType(method.getReturnType()));
            }
        }

        Map<String, String> resolvedTypes = new HashMap<>();
        for (Map.Entry<String, List<String>> methodEntry : classMethods.entrySet()) {
            Set<String> matchingTypes = new TreeSet<>();
            for (String runtimeName : methodEntry.getValue()) {
                matchingTypes.addAll(runtimeMethodTypes.getOrDefault(runtimeName, Set.of()));
            }
            if (!matchingTypes.isEmpty()) {
                resolvedTypes.put(methodEntry.getKey(), String.join(" | ", matchingTypes));
            }
        }
        return resolvedTypes;
    }

    private Map<String, String> collectFieldTypes(
            Class<?> runtimeClass,
            Map<String, String> classFields,
            boolean staticMembers
    ) {
        if (classFields == null || classFields.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> runtimeFieldTypes = new HashMap<>();
        for (Field field : runtimeClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) == staticMembers) {
                runtimeFieldTypes.put(field.getName(), renderJavaType(field.getType()));
            }
        }

        Map<String, String> resolvedTypes = new HashMap<>();
        for (Map.Entry<String, String> fieldEntry : classFields.entrySet()) {
            String type = runtimeFieldTypes.get(fieldEntry.getValue());
            if (type != null) {
                resolvedTypes.put(fieldEntry.getKey(), type);
            }
        }
        return resolvedTypes;
    }

    private String renderJavaType(Class<?> type) {
        if (type == null) {
            return "any";
        }
        if (type == Void.TYPE) {
            return "void";
        }
        if (type == Boolean.TYPE || type == Boolean.class) {
            return "boolean";
        }
        if (type == Character.TYPE || type == Character.class || type == String.class || CharSequence.class.isAssignableFrom(type)) {
            return "string";
        }
        if (type.isPrimitive() || Number.class.isAssignableFrom(type)) {
            return "number";
        }
        if (type.isArray()) {
            return "Array<" + renderJavaType(type.getComponentType()) + ">";
        }
        if (type == Object.class) {
            return "any";
        }
        if (type == Class.class) {
            return "JavaClass<any>";
        }
        if (Enum.class.isAssignableFrom(type)) {
            return "string | number";
        }

        String namedClassName = resolveNamedClassName(type);
        if (namedClassName != null && hasDeclarableTypeAccess(namedClassName)) {
            return buildInstanceTypeAccess(namedClassName);
        }
        return "JavaInstance | any";
    }

    private String resolveNamedClassName(Class<?> type) {
        String normalizedName = normalizeClassName(type);
        if (normalizedName == null) {
            return null;
        }
        return reflectedNamedClassesCache.computeIfAbsent(normalizedName, this::reverseLookupNamedClass);
    }

    private String reverseLookupNamedClass(String runtimeClassName) {
        for (Map.Entry<String, String> classEntry : currentClassMap.entrySet()) {
            if (runtimeClassName.equals(classEntry.getValue())) {
                return classEntry.getKey();
            }
        }
        return null;
    }

    private String normalizeClassName(Class<?> type) {
        if (type.isArray()) {
            return null;
        }
        return type.getName();
    }

    private void appendClassRegistry(StringBuilder builder, Set<String> classNames) {
        builder.append("interface MQSClassRegistry {\n");

        for (String className : new TreeSet<>(classNames)) {
            boolean hasTypedAccess = hasDeclarableTypeAccess(className);
            builder.append(INDENT)
                    .append("\"")
                    .append(className)
                    .append("\": ")
                    .append(hasTypedAccess ? "typeof " + buildTypeAccess(className) : "JavaClass<any>")
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

    private String buildInstanceTypeAccess(String className) {
        String[] parts = className.split("\\.");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "JavaInstance | any";
        }

        StringBuilder builder = new StringBuilder(resolveRootAccess(parts[0]));
        for (int index = 1; index < parts.length - 1; index++) {
            builder.append(resolveChildAccess(parts[index]));
        }
        builder.append(resolveChildAccess(parts[parts.length - 1] + "$Instance"));
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

    private boolean hasDeclarableNamespace(String[] parts) {
        for (int index = 0; index < parts.length - 1; index++) {
            if (!isAccessibleIdentifier(parts[index])) {
                return false;
            }
        }
        return true;
    }

    private boolean hasDeclarableTypeAccess(String className) {
        String[] parts = className.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        for (String part : parts) {
            if (!isAccessibleIdentifier(part)) {
                return false;
            }
        }
        return true;
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

    private record ReflectedClassMembers(
            boolean available,
            Set<String> staticMethodNames,
            Set<String> instanceMethodNames,
            Set<String> staticFieldNames,
            Set<String> instanceFieldNames,
            Map<String, String> staticMethodTypes,
            Map<String, String> instanceMethodTypes,
            Map<String, String> staticFieldTypes,
            Map<String, String> instanceFieldTypes
    ) {
        private ReflectedClassMembers {
            staticMethodNames = Set.copyOf(staticMethodNames);
            instanceMethodNames = Set.copyOf(instanceMethodNames);
            staticFieldNames = Set.copyOf(staticFieldNames);
            instanceFieldNames = Set.copyOf(instanceFieldNames);
            staticMethodTypes = Map.copyOf(staticMethodTypes);
            instanceMethodTypes = Map.copyOf(instanceMethodTypes);
            staticFieldTypes = Map.copyOf(staticFieldTypes);
            instanceFieldTypes = Map.copyOf(instanceFieldTypes);
        }

        private static ReflectedClassMembers empty() {
            return new ReflectedClassMembers(false, Set.of(), Set.of(), Set.of(), Set.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
    }
}

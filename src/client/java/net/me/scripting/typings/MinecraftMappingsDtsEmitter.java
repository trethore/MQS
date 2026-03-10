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
import net.me.scripting.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;

final class MinecraftMappingsDtsEmitter {
    private static final String INDENT = TypingsFormat.INDENT;
    private static final String BLOCK_END = TypingsFormat.BLOCK_END;
    private static final String JAVA_CLASS = "JavaClass";
    private static final String JAVA_INSTANCE = "JavaInstance";
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
        Map<String, ReflectedClassMembers> reflectedMembers = buildReflectedMembers(classMap, methodMap, fieldMap);
        Set<String> classNames = classMap.keySet();
        NamespaceNode rootNode = buildNamespaceTree(classNames);
        appendNamespaceDeclarations(builder, rootNode, "", 0, methodMap, fieldMap, reflectedMembers);
        appendClassRegistry(builder, classNames);
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
            if (isAccessibleIdentifier(className)) {
                appendClassDeclaration(builder, packageName, className, depth, methodMap, fieldMap, reflectedMembers);
            }
        }
    }

    private void appendClassDeclaration(
            StringBuilder builder,
            String packageName,
            String className,
            int depth,
            Map<String, Map<String, List<String>>> methodMap,
            Map<String, Map<String, String>> fieldMap,
            Map<String, ReflectedClassMembers> reflectedMembers
    ) {
        String fullClassName = packageName + "." + className;
        String instanceTypeName = className + "$Instance";
        String staticTypeName = className + "$Static";
        ReflectedClassMembers reflectionData = reflectedMembers.getOrDefault(fullClassName, ReflectedClassMembers.empty());
        Map<String, List<String>> classMethods = methodMap.getOrDefault(fullClassName, Collections.emptyMap());
        Map<String, String> classFields = fieldMap.getOrDefault(fullClassName, Collections.emptyMap());

        MemberDeclarationContext staticContext = createMemberDeclarationContext(classMethods, classFields, reflectionData.staticMembers());
        MemberDeclarationContext instanceContext = createMemberDeclarationContext(classMethods, classFields, reflectionData.instanceMembers());

        appendStaticClassDeclaration(builder, staticTypeName, instanceTypeName, className, reflectionData.constructorSignatures(), staticContext, depth);
        appendInstanceClassDeclaration(builder, instanceTypeName, instanceContext, depth);
    }

    private void appendStaticClassDeclaration(
            StringBuilder builder,
            String staticTypeName,
            String instanceTypeName,
            String className,
            List<String> constructorSignatures,
            MemberDeclarationContext memberContext,
            int depth
    ) {
        appendIndent(builder, depth);
        builder.append("interface ").append(staticTypeName).append(" extends " + JAVA_CLASS + "<").append(instanceTypeName).append("> {\n");
        appendConstructSignatures(builder, constructorSignatures, instanceTypeName, depth + 1);
        appendMembers(builder, memberContext, depth + 1);
        appendIndent(builder, depth);
        builder.append(BLOCK_END);

        appendIndent(builder, depth);
        builder.append("const ").append(className).append(": ").append(staticTypeName).append(";\n");
    }

    private void appendInstanceClassDeclaration(
            StringBuilder builder,
            String instanceTypeName,
            MemberDeclarationContext memberContext,
            int depth
    ) {
        appendIndent(builder, depth);
        builder.append("interface ").append(instanceTypeName).append(" extends " + JAVA_INSTANCE + " {\n");
        appendMembers(builder, memberContext, depth + 1);
        appendIndent(builder, depth);
        builder.append(BLOCK_END);
    }

    private MemberDeclarationContext createMemberDeclarationContext(
            Map<String, List<String>> classMethods,
            Map<String, String> classFields,
            ReflectedMemberData reflectedMemberData
    ) {
        Set<String> methodNames = collectVisibleMemberNames(
                classMethods.keySet(),
                reflectedMemberData.mappedMethodNames(),
                reflectedMemberData.directMethodNames()
        );
        Set<String> fieldNames = collectVisibleMemberNames(
                classFields.keySet(),
                reflectedMemberData.mappedFieldNames(),
                reflectedMemberData.directFieldNames()
        );
        return new MemberDeclarationContext(
                methodNames,
                fieldNames,
                mergeTypes(reflectedMemberData.mappedMethodTypes(), reflectedMemberData.directMethodTypes()),
                mergeTypes(reflectedMemberData.mappedFieldTypes(), reflectedMemberData.directFieldTypes())
        );
    }

    private void appendMembers(StringBuilder builder, MemberDeclarationContext memberContext, int depth) {
        appendMethodDeclarations(builder, memberContext.methodNames(), memberContext.methodTypes(), depth);
        appendFieldDeclarations(builder, memberContext.fieldNames(), memberContext.methodNames(), memberContext.fieldTypes(), depth);
    }

    private Set<String> collectVisibleMemberNames(
            Collection<String> mappedMemberNames,
            Set<String> allowedMappedNames,
            Set<String> directMemberNames
    ) {
        Set<String> visibleMemberNames = new TreeSet<>();
        addMappedMemberNames(visibleMemberNames, mappedMemberNames, allowedMappedNames);
        addDirectMemberNames(visibleMemberNames, directMemberNames);
        return visibleMemberNames;
    }

    private void addMappedMemberNames(Set<String> target, Collection<String> mappedMemberNames, Set<String> allowedMappedNames) {
        for (String memberName : mappedMemberNames) {
            if (isSupportedMemberName(memberName) && shouldIncludeMember(memberName, allowedMappedNames)) {
                target.add(memberName);
            }
        }
    }

    private void addDirectMemberNames(Set<String> target, Collection<String> directMemberNames) {
        for (String memberName : directMemberNames) {
            if (isSupportedMemberName(memberName)) {
                target.add(memberName);
            }
        }
    }

    private void appendMethodDeclarations(StringBuilder builder, Set<String> methodNames, Map<String, String> methodTypes, int depth) {
        for (String methodName : methodNames) {
            appendIndent(builder, depth);
            builder.append(renderMemberName(methodName))
                    .append("(...args: any[]): ")
                    .append(methodTypes.getOrDefault(methodName, "any"))
                    .append(";\n");
        }
    }

    private void appendFieldDeclarations(
            StringBuilder builder,
            Set<String> fieldNames,
            Set<String> methodNames,
            Map<String, String> fieldTypes,
            int depth
    ) {
        for (String fieldName : fieldNames) {
            appendFieldDeclaration(builder, fieldName, fieldName, methodNames, fieldTypes, depth);
            String explicitFieldName = fieldName + WrapperConstants.FIELD_SUFFIX;
            appendFieldDeclaration(builder, explicitFieldName, fieldName, methodNames, fieldTypes, depth);
        }
    }

    private void appendFieldDeclaration(
            StringBuilder builder,
            String renderedFieldName,
            String typeKey,
            Set<String> methodNames,
            Map<String, String> fieldTypes,
            int depth
    ) {
        if (methodNames.contains(renderedFieldName)) {
            return;
        }

        appendIndent(builder, depth);
        builder.append(renderMemberName(renderedFieldName))
                .append(": ")
                .append(fieldTypes.getOrDefault(typeKey, "any"))
                .append(";\n");
    }

    private void appendConstructSignatures(StringBuilder builder, List<String> constructorSignatures, String instanceTypeName, int depth) {
        if (constructorSignatures.isEmpty()) {
            appendIndent(builder, depth);
            builder.append("new (...args: any[]): ")
                    .append(instanceTypeName)
                    .append(";\n");
            return;
        }

        for (String constructorSignature : constructorSignatures) {
            appendIndent(builder, depth);
            builder.append("new (")
                    .append(constructorSignature)
                    .append("): ")
                    .append(instanceTypeName)
                    .append(";\n");
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
            ReflectedMemberData staticMembers = collectReflectedMemberData(runtimeClass, classMethods, classFields, true);
            ReflectedMemberData instanceMembers = collectReflectedMemberData(runtimeClass, classMethods, classFields, false);
            return new ReflectedClassMembers(
                    collectConstructorSignatures(runtimeClass),
                    staticMembers,
                    instanceMembers
            );
        } catch (LinkageError | ClassNotFoundException ignored) {
            return ReflectedClassMembers.empty();
        }
    }

    private ReflectedMemberData collectReflectedMemberData(
            Class<?> runtimeClass,
            Map<String, List<String>> classMethods,
            Map<String, String> classFields,
            boolean staticMembers
    ) {
        MemberNameAndTypeData mappedMethods = collectMappedMethodData(runtimeClass, classMethods, staticMembers);
        MemberNameAndTypeData mappedFields = collectMappedFieldData(runtimeClass, classFields, staticMembers);
        MemberNameAndTypeData directMethods = collectDirectMethodData(runtimeClass, staticMembers);
        MemberNameAndTypeData directFields = staticMembers
                ? MemberNameAndTypeData.empty()
                : collectDirectInstanceFieldData(runtimeClass);
        return new ReflectedMemberData(
                mappedMethods.names(),
                mappedFields.names(),
                directMethods.names(),
                directFields.names(),
                mappedMethods.types(),
                mappedFields.types(),
                directMethods.types(),
                directFields.types()
        );
    }

    private List<String> collectConstructorSignatures(Class<?> runtimeClass) {
        List<String> constructorSignatures = new ArrayList<>();
        for (java.lang.reflect.Constructor<?> constructor : runtimeClass.getConstructors()) {
            constructorSignatures.add(renderExecutableParameters(constructor.getParameterTypes(), constructor.getParameters()));
        }
        constructorSignatures.sort(Comparator.comparingInt(this::countRenderedParameters).thenComparing(String::compareTo));
        return List.copyOf(constructorSignatures);
    }

    private MemberNameAndTypeData collectMappedMethodData(Class<?> runtimeClass, Map<String, List<String>> classMethods, boolean staticMembers) {
        if (classMethods == null || classMethods.isEmpty()) {
            return MemberNameAndTypeData.empty();
        }

        Set<String> matchingNames = new TreeSet<>();
        Map<String, String> resolvedTypes = new TreeMap<>();
        for (Map.Entry<String, List<String>> methodEntry : classMethods.entrySet()) {
            List<Method> matchingMethods = ReflectionUtils.findMethods(runtimeClass, methodEntry.getValue(), staticMembers);
            if (!matchingMethods.isEmpty()) {
                matchingNames.add(methodEntry.getKey());
                resolvedTypes.put(methodEntry.getKey(), renderMethodTypes(matchingMethods));
            }
        }
        return new MemberNameAndTypeData(matchingNames, resolvedTypes);
    }

    private MemberNameAndTypeData collectMappedFieldData(Class<?> runtimeClass, Map<String, String> classFields, boolean staticMembers) {
        if (classFields == null || classFields.isEmpty()) {
            return MemberNameAndTypeData.empty();
        }

        Set<String> matchingNames = new TreeSet<>();
        Map<String, String> resolvedTypes = new TreeMap<>();
        for (Map.Entry<String, String> fieldEntry : classFields.entrySet()) {
            Field field = findField(runtimeClass, fieldEntry.getValue());
            if (field != null && Modifier.isStatic(field.getModifiers()) == staticMembers) {
                matchingNames.add(fieldEntry.getKey());
                resolvedTypes.put(fieldEntry.getKey(), renderJavaType(field.getType()));
            }
        }
        return new MemberNameAndTypeData(matchingNames, resolvedTypes);
    }

    private MemberNameAndTypeData collectDirectMethodData(Class<?> runtimeClass, boolean staticMembers) {
        Set<String> methodNames = new TreeSet<>();
        Map<String, Set<String>> methodTypes = new TreeMap<>();
        for (Method method : runtimeClass.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) == staticMembers) {
                methodNames.add(method.getName());
                methodTypes.computeIfAbsent(method.getName(), ignored -> new TreeSet<>())
                        .add(renderJavaType(method.getReturnType()));
            }
        }
        return new MemberNameAndTypeData(methodNames, flattenCollectedTypes(methodTypes));
    }

    private MemberNameAndTypeData collectDirectInstanceFieldData(Class<?> runtimeClass) {
        Set<String> fieldNames = new TreeSet<>();
        Map<String, String> fieldTypes = new TreeMap<>();
        for (Field field : runtimeClass.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                fieldNames.add(field.getName());
                fieldTypes.put(field.getName(), renderJavaType(field.getType()));
            }
        }
        return new MemberNameAndTypeData(fieldNames, fieldTypes);
    }

    private String renderExecutableParameters(Class<?>[] parameterTypes, Parameter[] parameters) {
        if (parameterTypes.length == 0) {
            return "";
        }

        List<String> renderedParameters = new ArrayList<>(parameterTypes.length);
        for (int index = 0; index < parameterTypes.length; index++) {
            renderedParameters.add(renderParameter(index, parameterTypes[index], parameters));
        }
        return String.join(", ", renderedParameters);
    }

    private String renderParameter(int index, Class<?> parameterType, Parameter[] parameters) {
        String parameterName = "arg" + index;
        if (parameters != null && index < parameters.length) {
            String reflectedName = parameters[index].getName();
            if (TypingsDeclarationUtils.isAccessibleIdentifier(reflectedName)) {
                parameterName = reflectedName;
            }
        }
        return parameterName + ": " + renderJavaType(parameterType);
    }

    private int countRenderedParameters(String signature) {
        if (signature.isEmpty()) {
            return 0;
        }
        return signature.split(", ").length;
    }

    private String renderMethodTypes(List<Method> methods) {
        Set<String> matchingTypes = new TreeSet<>();
        for (Method method : methods) {
            matchingTypes.add(renderJavaType(method.getReturnType()));
        }
        return String.join(" | ", matchingTypes);
    }

    private Field findField(Class<?> runtimeClass, String runtimeFieldName) {
        try {
            return ReflectionUtils.findField(runtimeClass, runtimeFieldName);
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private Map<String, String> mergeTypes(Map<String, String> primaryTypes, Map<String, String> secondaryTypes) {
        if (primaryTypes.isEmpty()) {
            return secondaryTypes;
        }
        if (secondaryTypes.isEmpty()) {
            return primaryTypes;
        }

        Map<String, Set<String>> mergedTypes = new TreeMap<>();
        addTypes(mergedTypes, primaryTypes);
        addTypes(mergedTypes, secondaryTypes);
        return flattenCollectedTypes(mergedTypes);
    }

    private void addTypes(Map<String, Set<String>> collectedTypes, Map<String, String> typesToAdd) {
        for (Map.Entry<String, String> entry : typesToAdd.entrySet()) {
            Set<String> typeSet = collectedTypes.computeIfAbsent(entry.getKey(), ignored -> new TreeSet<>());
            String[] splitTypes = entry.getValue().split(" \\| ");
            Collections.addAll(typeSet, splitTypes);
        }
    }

    private Map<String, String> flattenCollectedTypes(Map<String, Set<String>> collectedTypes) {
        Map<String, String> flattenedTypes = new TreeMap<>();
        for (Map.Entry<String, Set<String>> entry : collectedTypes.entrySet()) {
            flattenedTypes.put(entry.getKey(), String.join(" | ", entry.getValue()));
        }
        return flattenedTypes;
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
            return JAVA_CLASS + "<any>";
        }
        if (Enum.class.isAssignableFrom(type)) {
            return "string | number";
        }

        String namedClassName = resolveNamedClassName(type);
        if (namedClassName != null && hasDeclarableTypeAccess(namedClassName)) {
            return buildInstanceTypeAccess(namedClassName);
        }
        return JAVA_INSTANCE + " | any";
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
                    .append(hasTypedAccess ? "typeof " + buildTypeAccess(className) : JAVA_CLASS + "<any>")
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
            return JAVA_INSTANCE + " | any";
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
        return TypingsDeclarationUtils.isAccessibleIdentifier(value);
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
        if (TypingsDeclarationUtils.isAccessibleIdentifier(memberName)) {
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

    private record MemberDeclarationContext(
            Set<String> methodNames,
            Set<String> fieldNames,
            Map<String, String> methodTypes,
            Map<String, String> fieldTypes
    ) {
    }

    private record MemberNameAndTypeData(Set<String> names, Map<String, String> types) {
        private MemberNameAndTypeData {
            names = Set.copyOf(names);
            types = Map.copyOf(types);
        }

        private static MemberNameAndTypeData empty() {
            return new MemberNameAndTypeData(Set.of(), Map.of());
        }
    }

    private record ReflectedMemberData(
            Set<String> mappedMethodNames,
            Set<String> mappedFieldNames,
            Set<String> directMethodNames,
            Set<String> directFieldNames,
            Map<String, String> mappedMethodTypes,
            Map<String, String> mappedFieldTypes,
            Map<String, String> directMethodTypes,
            Map<String, String> directFieldTypes
    ) {
        private ReflectedMemberData {
            mappedMethodNames = Set.copyOf(mappedMethodNames);
            mappedFieldNames = Set.copyOf(mappedFieldNames);
            directMethodNames = Set.copyOf(directMethodNames);
            directFieldNames = Set.copyOf(directFieldNames);
            mappedMethodTypes = Map.copyOf(mappedMethodTypes);
            mappedFieldTypes = Map.copyOf(mappedFieldTypes);
            directMethodTypes = Map.copyOf(directMethodTypes);
            directFieldTypes = Map.copyOf(directFieldTypes);
        }

        private static ReflectedMemberData empty() {
            return new ReflectedMemberData(Set.of(), Set.of(), Set.of(), Set.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
    }

    private record ReflectedClassMembers(
            List<String> constructorSignatures,
            ReflectedMemberData staticMembers,
            ReflectedMemberData instanceMembers
    ) {
        private ReflectedClassMembers {
            constructorSignatures = List.copyOf(constructorSignatures);
            staticMembers = staticMembers == null ? ReflectedMemberData.empty() : staticMembers;
            instanceMembers = instanceMembers == null ? ReflectedMemberData.empty() : instanceMembers;
        }

        private static ReflectedClassMembers empty() {
            return new ReflectedClassMembers(List.of(), ReflectedMemberData.empty(), ReflectedMemberData.empty());
        }
    }
}

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
package io.github.trethore.myqolpackages.internal.runtime.graal.interop.generation;

import io.github.trethore.myqolpackages.internal.runtime.graal.interop.JavaInteropAccess;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GeneratedTypeValidator {
    private static final String SYNTHETIC_PREFIX = "$mqp$";

    private final ConstructorImplementationValidator constructorValidator = new ConstructorImplementationValidator();
    private final JavaInteropAccess interop;

    GeneratedTypeValidator(JavaInteropAccess interop) {
        this.interop = interop;
    }

    GeneratedTypeDefinition validate(GeneratedTypeDefinition definition) {
        validateHierarchy(definition);
        validateFields(definition);
        validateConstructors(definition);
        List<GeneratedTypeDefinition.MethodDefinition> methods = validateMethods(definition);
        return new GeneratedTypeDefinition(
                definition.kind(),
                definition.packageId(),
                definition.binaryName(),
                definition.superclass(),
                definition.interfaces(),
                definition.fields(),
                definition.constructors(),
                methods,
                definition.isFinal(),
                definition.isAbstract());
    }

    private void validateHierarchy(GeneratedTypeDefinition definition) {
        Set<Class<?>> directInterfaces = new HashSet<>();
        for (Class<?> interfaceType : definition.interfaces()) {
            if (!interfaceType.isInterface()) {
                throw new IllegalArgumentException(interfaceType.getTypeName() + " is not an interface");
            }
            if (!directInterfaces.add(interfaceType)) {
                throw new IllegalArgumentException("Duplicate interface: " + interfaceType.getTypeName());
            }
            requireAccessible(interfaceType, definition.binaryName());
        }
        if (definition.kind() == GeneratedTypeDefinition.Kind.INTERFACE) {
            return;
        }
        Class<?> superclass = definition.superclass();
        if (superclass.isPrimitive() || superclass.isArray() || superclass.isInterface()) {
            throw new IllegalArgumentException("Invalid superclass: " + superclass.getTypeName());
        }
        if (Modifier.isFinal(superclass.getModifiers())) {
            throw new IllegalArgumentException("Cannot extend final class " + superclass.getTypeName());
        }
        requireAccessible(superclass, definition.binaryName());
        if (definition.isFinal() && definition.isAbstract()) {
            throw new IllegalArgumentException("Generated class cannot be both final and abstract");
        }
    }

    private void validateFields(GeneratedTypeDefinition definition) {
        Set<String> names = new HashSet<>();
        for (GeneratedTypeDefinition.FieldDefinition field : definition.fields()) {
            validateMemberName(field.name(), "field");
            if (!names.add(field.name())) {
                throw new IllegalArgumentException("Duplicate field: " + field.name());
            }
            requireAccessible(field.type(), definition.binaryName());
        }
    }

    private void validateConstructors(GeneratedTypeDefinition definition) {
        if (definition.kind() == GeneratedTypeDefinition.Kind.INTERFACE) {
            if (!definition.constructors().isEmpty()) {
                throw new IllegalArgumentException("Interfaces cannot define constructors");
            }
            return;
        }
        List<Constructor<?>> superConstructors = Arrays.stream(
                        definition.superclass().getDeclaredConstructors())
                .filter(constructor -> isConstructorAccessible(constructor, definition.binaryName()))
                .toList();
        if (definition.constructors().isEmpty()) {
            boolean noArgumentConstructor =
                    superConstructors.stream().anyMatch(constructor -> constructor.getParameterCount() == 0);
            if (!noArgumentConstructor) {
                throw new IllegalArgumentException("Superclass "
                        + definition.superclass().getTypeName()
                        + " has no accessible no-argument constructor");
            }
            return;
        }
        if (superConstructors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Superclass " + definition.superclass().getTypeName() + " has no accessible constructor");
        }
        Set<List<Class<?>>> descriptors = new HashSet<>();
        for (GeneratedTypeDefinition.ConstructorDefinition constructor : definition.constructors()) {
            if (!descriptors.add(constructor.argumentTypes())) {
                throw new IllegalArgumentException("Duplicate constructor descriptor: " + constructor.argumentTypes());
            }
            constructor.argumentTypes().forEach(type -> requireAccessible(type, definition.binaryName()));
            constructorValidator.validate(constructor.implementation());
        }
    }

    private List<GeneratedTypeDefinition.MethodDefinition> validateMethods(GeneratedTypeDefinition definition) {
        Set<MethodKey> descriptors = new HashSet<>();
        List<GeneratedTypeDefinition.MethodDefinition> normalized = new ArrayList<>();
        for (GeneratedTypeDefinition.MethodDefinition method : definition.methods()) {
            validateMemberName(method.exposedName(), "method");
            MethodKey descriptor = new MethodKey(method.exposedName(), method.argumentTypes());
            if (!descriptors.add(descriptor)) {
                throw new IllegalArgumentException("Duplicate method descriptor: " + method.exposedName());
            }
            requireAccessible(method.returnType(), definition.binaryName());
            method.argumentTypes().forEach(type -> requireAccessible(type, definition.binaryName()));
            normalized.add(
                    definition.kind() == GeneratedTypeDefinition.Kind.CLASS
                            ? validateClassMethod(definition, method)
                            : validateInterfaceMethod(definition, method));
        }
        if (definition.kind() == GeneratedTypeDefinition.Kind.CLASS
                && !definition.isAbstract()
                && normalized.stream().anyMatch(GeneratedTypeDefinition.MethodDefinition::isAbstract)) {
            throw new IllegalArgumentException("A class containing an abstract method must be abstract");
        }
        validateInheritedInterfaceReturns(definition, normalized);
        if (definition.kind() == GeneratedTypeDefinition.Kind.CLASS && !definition.isAbstract()) {
            validateConcreteClass(definition, normalized);
        }
        return List.copyOf(normalized);
    }

    private GeneratedTypeDefinition.MethodDefinition validateClassMethod(
            GeneratedTypeDefinition definition, GeneratedTypeDefinition.MethodDefinition method) {
        validateClassMethodDeclaration(method);
        List<Method> inheritedInstance =
                findInheritedMethods(definition, method.exposedName(), method.argumentTypes(), false);
        List<Method> inheritedStatic =
                findInheritedMethods(definition, method.exposedName(), method.argumentTypes(), true);
        validateClassMethodInheritanceKinds(method, inheritedInstance, inheritedStatic);
        List<Method> inherited = method.isStatic() ? inheritedStatic : inheritedInstance;
        if (method.override() && inherited.isEmpty()) {
            throw new IllegalArgumentException("No inherited method to override: " + method.exposedName());
        }
        if (inherited.isEmpty()) {
            return method;
        }
        Method inheritedMethod = inherited.getFirst();
        validateOverrideCompatibility(method, inheritedMethod);
        return copyWithRuntimeName(method, inheritedMethod.getName());
    }

    private static void validateClassMethodDeclaration(GeneratedTypeDefinition.MethodDefinition method) {
        if (method.isAbstract() && isInvalidAbstractClassMethod(method)) {
            throw new IllegalArgumentException("Invalid abstract method: " + method.exposedName());
        }
        if (!method.isAbstract() && method.implementation() == null) {
            throw new IllegalArgumentException("Missing implementation for method " + method.exposedName());
        }
    }

    private static boolean isInvalidAbstractClassMethod(GeneratedTypeDefinition.MethodDefinition method) {
        return method.isStatic()
                || method.isFinal()
                || method.visibility() == JavaVisibility.PRIVATE
                || method.implementation() != null;
    }

    private static void validateClassMethodInheritanceKinds(
            GeneratedTypeDefinition.MethodDefinition method,
            List<Method> inheritedInstance,
            List<Method> inheritedStatic) {
        if (method.isStatic() && !inheritedInstance.isEmpty()) {
            throw new IllegalArgumentException(
                    "Static method conflicts with inherited instance method: " + method.exposedName());
        }
        if (!method.isStatic() && !inheritedStatic.isEmpty()) {
            throw new IllegalArgumentException(
                    "Instance method conflicts with inherited static method: " + method.exposedName());
        }
        if (method.isStatic() && method.override()) {
            throw new IllegalArgumentException("Static methods cannot be inherited overrides: " + method.exposedName());
        }
    }

    private static void validateOverrideCompatibility(
            GeneratedTypeDefinition.MethodDefinition method, Method inheritedMethod) {
        int modifiers = inheritedMethod.getModifiers();
        if (Modifier.isFinal(modifiers)) {
            throw new IllegalArgumentException("Cannot override final method " + inheritedMethod.toGenericString());
        }
        if (Modifier.isStatic(modifiers) != method.isStatic()) {
            throw new IllegalArgumentException(
                    "Static and instance methods cannot override each other: " + method.exposedName());
        }
        if (!inheritedMethod.getReturnType().isAssignableFrom(method.returnType())) {
            throw new IllegalArgumentException("Incompatible return type for override " + method.exposedName());
        }
        if (visibilityRank(method.visibility()) < visibilityRank(modifiers)) {
            throw new IllegalArgumentException("Override reduces visibility: " + method.exposedName());
        }
    }

    private GeneratedTypeDefinition.MethodDefinition validateInterfaceMethod(
            GeneratedTypeDefinition definition, GeneratedTypeDefinition.MethodDefinition method) {
        validateInterfaceMethodDeclaration(method);
        validateInterfaceMethodOverrideFlags(method);
        validateInterfaceObjectMethod(method);
        List<Method> inherited = findInheritedMethods(definition, method.exposedName(), method.argumentTypes(), false);
        if (method.override() && inherited.isEmpty()) {
            throw new IllegalArgumentException("No inherited interface method to override: " + method.exposedName());
        }
        String runtimeName = inherited.isEmpty()
                ? method.runtimeName()
                : inherited.getFirst().getName();
        return copyWithRuntimeName(method, runtimeName);
    }

    private static void validateInterfaceMethodDeclaration(GeneratedTypeDefinition.MethodDefinition method) {
        if (method.visibility() != JavaVisibility.PUBLIC && method.visibility() != JavaVisibility.PRIVATE) {
            throw new IllegalArgumentException("Interface methods must be public or private");
        }
        if (method.isFinal()) {
            throw new IllegalArgumentException("Interface methods cannot be final");
        }
        if (method.isAbstract() && isInvalidAbstractInterfaceMethod(method)) {
            throw new IllegalArgumentException("Invalid abstract interface method: " + method.exposedName());
        }
        if (!method.isAbstract() && method.implementation() == null) {
            throw new IllegalArgumentException(
                    "Concrete interface method requires an implementation: " + method.exposedName());
        }
    }

    private static boolean isInvalidAbstractInterfaceMethod(GeneratedTypeDefinition.MethodDefinition method) {
        return method.visibility() != JavaVisibility.PUBLIC || method.isStatic() || method.implementation() != null;
    }

    private static void validateInterfaceMethodOverrideFlags(GeneratedTypeDefinition.MethodDefinition method) {
        if (method.visibility() == JavaVisibility.PRIVATE && method.override()) {
            throw new IllegalArgumentException("Private interface methods cannot override inherited methods");
        }
        if (method.isStatic() && method.override()) {
            throw new IllegalArgumentException("Static interface methods cannot override inherited methods");
        }
    }

    private static void validateInterfaceObjectMethod(GeneratedTypeDefinition.MethodDefinition method) {
        if (isPublicDefaultMethod(method) && isPublicObjectMethod(method.exposedName(), method.argumentTypes())) {
            throw new IllegalArgumentException(
                    "Interface cannot define a default implementation for public Object method "
                            + method.exposedName());
        }
    }

    private static boolean isPublicDefaultMethod(GeneratedTypeDefinition.MethodDefinition method) {
        return !method.isAbstract() && !method.isStatic() && method.visibility() == JavaVisibility.PUBLIC;
    }

    private List<Method> findInheritedMethods(
            GeneratedTypeDefinition definition,
            String exposedName,
            List<Class<?>> argumentTypes,
            boolean staticMethods) {
        List<Method> methods = new ArrayList<>();
        if (definition.kind() == GeneratedTypeDefinition.Kind.CLASS) {
            methods.addAll(
                    staticMethods
                            ? interop.getStaticMethods(definition.superclass(), exposedName)
                            : interop.getInstanceMethods(definition.superclass(), exposedName));
        }
        if (!staticMethods) {
            for (Class<?> interfaceType : definition.interfaces()) {
                methods.addAll(interop.getInstanceMethods(interfaceType, exposedName));
            }
        }
        Class<?>[] expectedTypes = argumentTypes.toArray(Class<?>[]::new);
        return methods.stream()
                .filter(method -> !Modifier.isPrivate(method.getModifiers()))
                .filter(method -> Modifier.isStatic(method.getModifiers()) == staticMethods)
                .filter(method -> Arrays.equals(method.getParameterTypes(), expectedTypes))
                .toList();
    }

    private void validateInheritedInterfaceReturns(
            GeneratedTypeDefinition definition, List<GeneratedTypeDefinition.MethodDefinition> declaredMethods) {
        Map<RuntimeMethodKey, List<Method>> inherited = collectInterfaceMethods(definition.interfaces());
        for (Map.Entry<RuntimeMethodKey, List<Method>> entry : inherited.entrySet()) {
            List<Class<?>> returnTypes = distinctReturnTypes(entry.getValue());
            boolean compatible = returnTypes.stream()
                    .anyMatch(candidate -> returnTypes.stream().allMatch(type -> type.isAssignableFrom(candidate)));
            if (compatible) {
                continue;
            }
            GeneratedTypeDefinition.MethodDefinition declared = declaredMethods.stream()
                    .filter(method -> !method.isStatic()
                            && method.runtimeName().equals(entry.getKey().name())
                            && method.argumentTypes().equals(entry.getKey().argumentTypes()))
                    .findFirst()
                    .orElse(null);
            if (declared == null
                    || returnTypes.stream().anyMatch(type -> !type.isAssignableFrom(declared.returnType()))) {
                throw new IllegalArgumentException("Inherited interface methods have incompatible return types: "
                        + entry.getKey().name());
            }
        }
    }

    private static List<Class<?>> distinctReturnTypes(List<Method> methods) {
        List<Class<?>> returnTypes = new ArrayList<>();
        for (Method method : methods) {
            Class<?> returnType = method.getReturnType();
            if (!returnTypes.contains(returnType)) {
                returnTypes.add(returnType);
            }
        }
        return List.copyOf(returnTypes);
    }

    private void validateConcreteClass(
            GeneratedTypeDefinition definition, List<GeneratedTypeDefinition.MethodDefinition> declaredMethods) {
        Map<RuntimeMethodKey, GeneratedTypeDefinition.MethodDefinition> declared =
                collectConcreteDeclaredMethods(declaredMethods);
        validateAbstractSuperclassMethods(definition, declared);
        validateInheritedInterfaceMethods(definition, declared);
    }

    private static Map<RuntimeMethodKey, GeneratedTypeDefinition.MethodDefinition> collectConcreteDeclaredMethods(
            List<GeneratedTypeDefinition.MethodDefinition> declaredMethods) {
        Map<RuntimeMethodKey, GeneratedTypeDefinition.MethodDefinition> declared = new LinkedHashMap<>();
        for (GeneratedTypeDefinition.MethodDefinition method : declaredMethods) {
            if (!method.isStatic() && !method.isAbstract()) {
                declared.put(new RuntimeMethodKey(method.runtimeName(), method.argumentTypes()), method);
            }
        }
        return declared;
    }

    private static void validateAbstractSuperclassMethods(
            GeneratedTypeDefinition definition,
            Map<RuntimeMethodKey, GeneratedTypeDefinition.MethodDefinition> declared) {
        Class<?> currentClass = definition.superclass();
        while (currentClass != null) {
            for (Method method : currentClass.getDeclaredMethods()) {
                if (requiresImplementation(method, definition.binaryName())) {
                    RuntimeMethodKey key = new RuntimeMethodKey(method.getName(), List.of(method.getParameterTypes()));
                    if (!hasConcreteImplementation(definition, declared, key)) {
                        throw new IllegalArgumentException(
                                "Concrete class does not implement abstract method " + method.toGenericString());
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    private static boolean requiresImplementation(Method method, String generatedBinaryName) {
        int modifiers = method.getModifiers();
        return Modifier.isAbstract(modifiers)
                && !Modifier.isPrivate(modifiers)
                && !Modifier.isStatic(modifiers)
                && isMemberAccessible(method.getDeclaringClass(), modifiers, generatedBinaryName);
    }

    private static void validateInheritedInterfaceMethods(
            GeneratedTypeDefinition definition,
            Map<RuntimeMethodKey, GeneratedTypeDefinition.MethodDefinition> declared) {
        Map<RuntimeMethodKey, List<Method>> interfaceMethods = collectInterfaceMethods(definition.interfaces());
        for (Map.Entry<RuntimeMethodKey, List<Method>> entry : interfaceMethods.entrySet()) {
            validateInheritedInterfaceMethod(definition, declared, entry.getKey(), entry.getValue());
        }
    }

    private static void validateInheritedInterfaceMethod(
            GeneratedTypeDefinition definition,
            Map<RuntimeMethodKey, GeneratedTypeDefinition.MethodDefinition> declared,
            RuntimeMethodKey key,
            List<Method> methods) {
        boolean concreteImplementation = hasConcreteImplementation(definition, declared, key);
        boolean hasDefault = methods.stream().anyMatch(method -> !Modifier.isAbstract(method.getModifiers()));
        boolean hasAbstract = methods.stream().anyMatch(method -> Modifier.isAbstract(method.getModifiers()));
        if (hasAbstract && !hasDefault && !concreteImplementation) {
            throw new IllegalArgumentException("Concrete class does not implement interface method " + key.name());
        }
        if (countMostSpecificDefaults(methods) > 1 && !concreteImplementation) {
            throw new IllegalArgumentException("Unresolved inherited interface default-method conflict: " + key.name());
        }
    }

    private static long countMostSpecificDefaults(List<Method> methods) {
        List<Method> defaults = methods.stream()
                .filter(method -> !Modifier.isAbstract(method.getModifiers()))
                .toList();
        return defaults.stream()
                .filter(candidate -> defaults.stream()
                        .noneMatch(other -> candidate != other
                                && candidate.getDeclaringClass().isAssignableFrom(other.getDeclaringClass())))
                .count();
    }

    private static boolean hasConcreteImplementation(
            GeneratedTypeDefinition definition,
            Map<RuntimeMethodKey, GeneratedTypeDefinition.MethodDefinition> declared,
            RuntimeMethodKey key) {
        if (declared.containsKey(key)) {
            return true;
        }
        Class<?> currentClass = definition.superclass();
        while (currentClass != null) {
            try {
                Method method = currentClass.getDeclaredMethod(
                        key.name(), key.argumentTypes().toArray(Class<?>[]::new));
                int modifiers = method.getModifiers();
                return !Modifier.isAbstract(modifiers)
                        && !Modifier.isPrivate(modifiers)
                        && !Modifier.isStatic(modifiers)
                        && isMemberAccessible(method.getDeclaringClass(), modifiers, definition.binaryName());
            } catch (NoSuchMethodException ignored) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return false;
    }

    private static Map<RuntimeMethodKey, List<Method>> collectInterfaceMethods(List<Class<?>> interfaces) {
        Map<RuntimeMethodKey, List<Method>> methods = new LinkedHashMap<>();
        for (Class<?> interfaceType : interfaces) {
            for (Method method : interfaceType.getMethods()) {
                int modifiers = method.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers)) {
                    continue;
                }
                RuntimeMethodKey key = new RuntimeMethodKey(method.getName(), List.of(method.getParameterTypes()));
                methods.computeIfAbsent(key, ignored -> new ArrayList<>()).add(method);
            }
        }
        return methods;
    }

    private static boolean isMemberAccessible(Class<?> declaringClass, int modifiers, String generatedBinaryName) {
        if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
            return true;
        }
        return !Modifier.isPrivate(modifiers)
                && packageName(declaringClass.getName()).equals(packageName(generatedBinaryName));
    }

    private static GeneratedTypeDefinition.MethodDefinition copyWithRuntimeName(
            GeneratedTypeDefinition.MethodDefinition method, String runtimeName) {
        return new GeneratedTypeDefinition.MethodDefinition(
                method.exposedName(),
                runtimeName,
                method.returnType(),
                method.argumentTypes(),
                method.implementation(),
                method.visibility(),
                method.isStatic(),
                method.isFinal(),
                method.isAbstract(),
                method.override());
    }

    private static boolean isPublicObjectMethod(String name, List<Class<?>> argumentTypes) {
        return Arrays.stream(Object.class.getMethods())
                .anyMatch(method -> method.getName().equals(name)
                        && Arrays.equals(method.getParameterTypes(), argumentTypes.toArray(Class<?>[]::new)));
    }

    private static void validateMemberName(String name, String kind) {
        if (name.startsWith(SYNTHETIC_PREFIX)
                || name.isEmpty()
                || !Character.isJavaIdentifierStart(name.codePointAt(0))
                || name.codePoints().skip(1).anyMatch(codePoint -> !Character.isJavaIdentifierPart(codePoint))) {
            throw new IllegalArgumentException("Invalid " + kind + " name: " + name);
        }
    }

    private static void requireAccessible(Class<?> type, String generatedBinaryName) {
        Class<?> componentType = type;
        while (componentType.isArray()) {
            componentType = componentType.getComponentType();
        }
        if (componentType.isPrimitive() || Modifier.isPublic(componentType.getModifiers())) {
            return;
        }
        if (!packageName(componentType.getName()).equals(packageName(generatedBinaryName))) {
            throw new IllegalArgumentException(
                    "Type is not accessible to generated type: " + componentType.getTypeName());
        }
    }

    private static boolean isConstructorAccessible(Constructor<?> constructor, String generatedBinaryName) {
        int modifiers = constructor.getModifiers();
        if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
            return true;
        }
        return !Modifier.isPrivate(modifiers)
                && packageName(constructor.getDeclaringClass().getName()).equals(packageName(generatedBinaryName));
    }

    private static String packageName(String binaryName) {
        int separator = binaryName.lastIndexOf('.');
        return separator < 0 ? "" : binaryName.substring(0, separator);
    }

    private static int visibilityRank(JavaVisibility visibility) {
        return switch (visibility) {
            case PRIVATE -> 0;
            case PACKAGE -> 1;
            case PROTECTED -> 2;
            case PUBLIC -> 3;
        };
    }

    private static int visibilityRank(int modifiers) {
        if (Modifier.isPublic(modifiers)) {
            return 3;
        }
        if (Modifier.isProtected(modifiers)) {
            return 2;
        }
        if (Modifier.isPrivate(modifiers)) {
            return 0;
        }
        return 1;
    }

    private record MethodKey(String name, List<Class<?>> argumentTypes) {}

    private record RuntimeMethodKey(String name, List<Class<?>> argumentTypes) {}
}

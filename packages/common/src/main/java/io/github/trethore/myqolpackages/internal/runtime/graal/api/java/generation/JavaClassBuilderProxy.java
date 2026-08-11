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

import static io.github.trethore.myqolpackages.internal.runtime.graal.api.java.generation.BuilderValueReader.requireArgumentCount;

import java.util.ArrayList;
import java.util.List;
import org.graalvm.polyglot.Value;

final class JavaClassBuilderProxy extends AbstractTypeBuilderProxy {
    private static final String ABSTRACT_METHOD = "abstract";
    private static final String ARGUMENT_TYPES_MEMBER = "argTypes";
    private static final String BUILD_METHOD = "build";
    private static final String CONSTRUCTOR_METHOD = "constructor";
    private static final String EXTENDS_METHOD = "extends";
    private static final String FIELD_METHOD = "field";
    private static final String FINAL_METHOD = "final";
    private static final String FINAL_MEMBER = "isFinal";
    private static final String IMPLEMENTATION_MEMBER = "implementation";
    private static final String IMPLEMENTS_METHOD = "implements";
    private static final String METHOD_METHOD = "method";
    private static final String NAME_MEMBER = "name";
    private static final String STATIC_MEMBER = "isStatic";
    private static final String TYPE_MEMBER = "type";
    private static final String VALUE_MEMBER = "value";
    private static final String VISIBILITY_MEMBER = "visibility";

    private final List<GeneratedTypeDefinition.ConstructorDefinition> constructors = new ArrayList<>();
    private final List<GeneratedTypeDefinition.FieldDefinition> fields = new ArrayList<>();
    private final List<Class<?>> interfaces = new ArrayList<>();
    private final List<GeneratedTypeDefinition.MethodDefinition> methods = new ArrayList<>();
    private final String binaryName;
    private final JavaTypeGenerationService service;
    private final GeneratedJavaTypeResolver typeResolver;

    private boolean abstractType;
    private boolean finalType;
    private Class<?> superclass = Object.class;
    private boolean superclassSet;

    JavaClassBuilderProxy(JavaTypeGenerationService service, String binaryName) {
        this.service = service;
        this.binaryName = binaryName;
        this.typeResolver = service.typeResolver();
        defineMember(EXTENDS_METHOD, this::extend);
        defineMember(IMPLEMENTS_METHOD, this::implement);
        defineMember(FINAL_METHOD, this::markFinal);
        defineMember(ABSTRACT_METHOD, this::markAbstract);
        defineMember(FIELD_METHOD, this::addField);
        defineMember(CONSTRUCTOR_METHOD, this::addConstructor);
        defineMember(METHOD_METHOD, this::addMethod);
        defineMember(BUILD_METHOD, this::build);
    }

    private Object extend(Value... arguments) {
        requireMutable();
        requireArgumentCount(arguments, 1, EXTENDS_METHOD);
        if (superclassSet) {
            throw new IllegalStateException("Superclass has already been defined");
        }
        superclass = typeResolver.resolve(arguments[0], "superclass", false);
        superclassSet = true;
        return this;
    }

    private Object implement(Value... arguments) {
        requireMutable();
        requireArgumentCount(arguments, 1, IMPLEMENTS_METHOD);
        interfaces.addAll(typeResolver.resolveSingleOrArray(arguments[0], "interfaces", false));
        return this;
    }

    private Object markFinal(Value... arguments) {
        requireMutable();
        requireArgumentCount(arguments, 0, FINAL_METHOD);
        finalType = true;
        return this;
    }

    private Object markAbstract(Value... arguments) {
        requireMutable();
        requireArgumentCount(arguments, 0, ABSTRACT_METHOD);
        abstractType = true;
        return this;
    }

    private Object addField(Value... arguments) {
        requireMutable();
        Value definition = BuilderValueReader.requireDefinition(arguments, FIELD_METHOD);
        String name = BuilderValueReader.requireString(definition, NAME_MEMBER);
        Class<?> type =
                typeResolver.resolve(BuilderValueReader.requireMember(definition, TYPE_MEMBER), "field type", false);
        boolean hasValue = definition.hasMember(VALUE_MEMBER);
        Value value = hasValue ? definition.getMember(VALUE_MEMBER) : null;
        JavaVisibility visibility = JavaVisibility.read(definition, VISIBILITY_MEMBER, JavaVisibility.PACKAGE);
        boolean isStatic = BuilderValueReader.optionalBoolean(definition, STATIC_MEMBER, false);
        boolean isFinal = BuilderValueReader.optionalBoolean(definition, FINAL_MEMBER, false);
        fields.add(new GeneratedTypeDefinition.FieldDefinition(
                name, type, value, hasValue, visibility, isStatic, isFinal));
        return this;
    }

    private Object addConstructor(Value... arguments) {
        requireMutable();
        Value definition = BuilderValueReader.requireDefinition(arguments, CONSTRUCTOR_METHOD);
        List<Class<?>> argumentTypes = definition.hasMember(ARGUMENT_TYPES_MEMBER)
                ? typeResolver.resolveSingleOrArray(
                        definition.getMember(ARGUMENT_TYPES_MEMBER), "constructor argument types", false)
                : List.of();
        Value implementation = BuilderValueReader.requireFunction(definition, IMPLEMENTATION_MEMBER);
        JavaVisibility visibility = JavaVisibility.read(definition, VISIBILITY_MEMBER, JavaVisibility.PACKAGE);
        constructors.add(new GeneratedTypeDefinition.ConstructorDefinition(argumentTypes, implementation, visibility));
        return this;
    }

    private Object addMethod(Value... arguments) {
        requireMutable();
        Value definition = BuilderValueReader.requireDefinition(arguments, METHOD_METHOD);
        String name = BuilderValueReader.requireString(definition, NAME_MEMBER);
        Class<?> returnType =
                typeResolver.resolve(BuilderValueReader.requireMember(definition, "returnType"), "return type", true);
        List<Class<?>> argumentTypes = definition.hasMember(ARGUMENT_TYPES_MEMBER)
                ? typeResolver.resolveSingleOrArray(
                        definition.getMember(ARGUMENT_TYPES_MEMBER), "method argument types", false)
                : List.of();
        boolean isAbstract = BuilderValueReader.optionalBoolean(definition, "isAbstract", false);
        Value implementation = null;
        if (definition.hasMember(IMPLEMENTATION_MEMBER)) {
            implementation = definition.getMember(IMPLEMENTATION_MEMBER);
            if (!implementation.canExecute()) {
                throw new IllegalArgumentException("implementation must be a JavaScript function");
            }
        }
        JavaVisibility visibility = JavaVisibility.read(definition, VISIBILITY_MEMBER, JavaVisibility.PACKAGE);
        boolean isStatic = BuilderValueReader.optionalBoolean(definition, STATIC_MEMBER, false);
        boolean isFinal = BuilderValueReader.optionalBoolean(definition, FINAL_MEMBER, false);
        boolean override = BuilderValueReader.optionalBoolean(definition, "override", false);
        methods.add(new GeneratedTypeDefinition.MethodDefinition(
                name,
                name,
                returnType,
                argumentTypes,
                implementation,
                visibility,
                isStatic,
                isFinal,
                isAbstract,
                override));
        return this;
    }

    private Object build(Value... arguments) {
        requireArgumentCount(arguments, 0, BUILD_METHOD);
        return completeBuild(() -> service.build(new GeneratedTypeDefinition(
                GeneratedTypeDefinition.Kind.CLASS,
                service.packageId(),
                binaryName,
                superclass,
                interfaces,
                fields,
                constructors,
                methods,
                finalType,
                abstractType)));
    }
}

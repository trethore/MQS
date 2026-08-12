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

import static io.github.trethore.myqolpackages.internal.runtime.graal.interop.generation.BuilderValueReader.requireArgumentCount;

import java.util.ArrayList;
import java.util.List;
import org.graalvm.polyglot.Value;

final class JavaInterfaceBuilderProxy extends AbstractTypeBuilderProxy {
    private final List<GeneratedTypeDefinition.FieldDefinition> fields = new ArrayList<>();
    private final List<GeneratedTypeDefinition.MethodDefinition> methods = new ArrayList<>();
    private final List<Class<?>> parentInterfaces = new ArrayList<>();
    private final String binaryName;
    private final JavaTypeGenerationService service;
    private final GeneratedJavaTypeResolver typeResolver;

    JavaInterfaceBuilderProxy(JavaTypeGenerationService service, String binaryName) {
        this.service = service;
        this.binaryName = binaryName;
        this.typeResolver = service.typeResolver();
        defineMember("extends", this::extend);
        defineMember("field", this::addField);
        defineMember("method", this::addMethod);
        defineMember("build", this::build);
    }

    private Object extend(Value... arguments) {
        requireMutable();
        requireArgumentCount(arguments, 1, "extends");
        parentInterfaces.addAll(typeResolver.resolveSingleOrArray(arguments[0], "parent interfaces"));
        return this;
    }

    private Object addField(Value... arguments) {
        requireMutable();
        Value definition = BuilderValueReader.requireDefinition(arguments, "field");
        String name = BuilderValueReader.requireString(definition, "name");
        Class<?> type = typeResolver.resolve(BuilderValueReader.requireMember(definition, "type"), "field type", false);
        Value value = BuilderValueReader.requireMember(definition, "value");
        fields.add(new GeneratedTypeDefinition.FieldDefinition(
                name, type, value, true, JavaVisibility.PUBLIC, true, true));
        return this;
    }

    private Object addMethod(Value... arguments) {
        requireMutable();
        Value definition = BuilderValueReader.requireDefinition(arguments, "method");
        String name = BuilderValueReader.requireString(definition, "name");
        Class<?> returnType =
                typeResolver.resolve(BuilderValueReader.requireMember(definition, "returnType"), "return type", true);
        List<Class<?>> argumentTypes = definition.hasMember("argTypes")
                ? typeResolver.resolveSingleOrArray(definition.getMember("argTypes"), "method argument types")
                : List.of();
        boolean isAbstract = BuilderValueReader.optionalBoolean(definition, "isAbstract");
        Value implementation = null;
        if (definition.hasMember("implementation")) {
            implementation = definition.getMember("implementation");
            if (!implementation.canExecute()) {
                throw new IllegalArgumentException("implementation must be a JavaScript function");
            }
        }
        JavaVisibility visibility = JavaVisibility.read(definition, "visibility", JavaVisibility.PUBLIC);
        boolean isStatic = BuilderValueReader.optionalBoolean(definition, "isStatic");
        boolean override = BuilderValueReader.optionalBoolean(definition, "override");
        methods.add(new GeneratedTypeDefinition.MethodDefinition(
                name,
                name,
                returnType,
                argumentTypes,
                implementation,
                visibility,
                isStatic,
                false,
                isAbstract,
                override));
        return this;
    }

    private Object build(Value... arguments) {
        requireArgumentCount(arguments, 0, "build");
        return completeBuild(() -> service.build(new GeneratedTypeDefinition(
                GeneratedTypeDefinition.Kind.INTERFACE,
                service.packageId(),
                binaryName,
                Object.class,
                parentInterfaces,
                fields,
                List.of(),
                methods,
                false,
                true)));
    }
}

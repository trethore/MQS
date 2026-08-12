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

import java.util.List;
import org.graalvm.polyglot.Value;

final class GeneratedMethodDefinitionReader {
    private static final String ABSTRACT_MEMBER = "isAbstract";
    private static final String ARGUMENT_TYPES_MEMBER = "argTypes";
    private static final String FINAL_MEMBER = "isFinal";
    private static final String IMPLEMENTATION_MEMBER = "implementation";
    private static final String NAME_MEMBER = "name";
    private static final String OVERRIDE_MEMBER = "override";
    private static final String RETURN_TYPE_MEMBER = "returnType";
    private static final String STATIC_MEMBER = "isStatic";
    private static final String VISIBILITY_MEMBER = "visibility";

    private GeneratedMethodDefinitionReader() {}

    static GeneratedTypeDefinition.MethodDefinition read(
            GeneratedJavaTypeResolver typeResolver,
            Value[] arguments,
            JavaVisibility defaultVisibility,
            boolean supportsFinal) {
        Value definition = BuilderValueReader.requireDefinition(arguments, "method");
        String name = BuilderValueReader.requireString(definition, NAME_MEMBER);
        Class<?> returnType = typeResolver.resolve(
                BuilderValueReader.requireMember(definition, RETURN_TYPE_MEMBER), "return type", true);
        List<Class<?>> argumentTypes = definition.hasMember(ARGUMENT_TYPES_MEMBER)
                ? typeResolver.resolveSingleOrArray(
                        definition.getMember(ARGUMENT_TYPES_MEMBER), "method argument types")
                : List.of();
        boolean isAbstract = BuilderValueReader.optionalBoolean(definition, ABSTRACT_MEMBER);
        Value implementation = readImplementation(definition);
        JavaVisibility visibility = JavaVisibility.read(definition, VISIBILITY_MEMBER, defaultVisibility);
        boolean isStatic = BuilderValueReader.optionalBoolean(definition, STATIC_MEMBER);
        boolean isFinal = supportsFinal && BuilderValueReader.optionalBoolean(definition, FINAL_MEMBER);
        boolean override = BuilderValueReader.optionalBoolean(definition, OVERRIDE_MEMBER);
        return new GeneratedTypeDefinition.MethodDefinition(
                name,
                name,
                returnType,
                argumentTypes,
                implementation,
                visibility,
                isStatic,
                isFinal,
                isAbstract,
                override);
    }

    private static Value readImplementation(Value definition) {
        if (!definition.hasMember(IMPLEMENTATION_MEMBER)) {
            return null;
        }
        Value implementation = definition.getMember(IMPLEMENTATION_MEMBER);
        if (!implementation.canExecute()) {
            throw new IllegalArgumentException("implementation must be a JavaScript function");
        }
        return implementation;
    }
}

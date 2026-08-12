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

record GeneratedTypeDefinition(
        Kind kind,
        String packageId,
        String binaryName,
        Class<?> superclass,
        List<Class<?>> interfaces,
        List<FieldDefinition> fields,
        List<ConstructorDefinition> constructors,
        List<MethodDefinition> methods,
        boolean isFinal,
        boolean isAbstract) {
    GeneratedTypeDefinition {
        interfaces = List.copyOf(interfaces);
        fields = List.copyOf(fields);
        constructors = List.copyOf(constructors);
        methods = List.copyOf(methods);
    }

    StructuralSignature structuralSignature() {
        return new StructuralSignature(
                kind,
                binaryName,
                superclass,
                interfaces,
                fields.stream().map(FieldDefinition::structural).toList(),
                constructors.stream().map(ConstructorDefinition::structural).toList(),
                methods.stream().map(MethodDefinition::structural).toList(),
                isFinal,
                isAbstract);
    }

    enum Kind {
        CLASS,
        INTERFACE
    }

    record FieldDefinition(
            String name,
            Class<?> type,
            Value value,
            boolean hasValue,
            JavaVisibility visibility,
            boolean isStatic,
            boolean isFinal) {
        FieldStructural structural() {
            return new FieldStructural(name, type, visibility, isStatic, isFinal);
        }
    }

    record ConstructorDefinition(List<Class<?>> argumentTypes, Value implementation, JavaVisibility visibility) {
        ConstructorDefinition {
            argumentTypes = List.copyOf(argumentTypes);
        }

        ConstructorStructural structural() {
            return new ConstructorStructural(argumentTypes, visibility);
        }
    }

    record MethodDefinition(
            String exposedName,
            String runtimeName,
            Class<?> returnType,
            List<Class<?>> argumentTypes,
            Value implementation,
            JavaVisibility visibility,
            boolean isStatic,
            boolean isFinal,
            boolean isAbstract,
            boolean override) {
        MethodDefinition {
            argumentTypes = List.copyOf(argumentTypes);
        }

        MethodStructural structural() {
            return new MethodStructural(
                    runtimeName, returnType, argumentTypes, visibility, isStatic, isFinal, isAbstract);
        }
    }

    record StructuralSignature(
            Kind kind,
            String binaryName,
            Class<?> superclass,
            List<Class<?>> interfaces,
            List<FieldStructural> fields,
            List<ConstructorStructural> constructors,
            List<MethodStructural> methods,
            boolean isFinal,
            boolean isAbstract) {}

    record FieldStructural(String name, Class<?> type, JavaVisibility visibility, boolean isStatic, boolean isFinal) {}

    record ConstructorStructural(List<Class<?>> argumentTypes, JavaVisibility visibility) {}

    record MethodStructural(
            String runtimeName,
            Class<?> returnType,
            List<Class<?>> argumentTypes,
            JavaVisibility visibility,
            boolean isStatic,
            boolean isFinal,
            boolean isAbstract) {}
}

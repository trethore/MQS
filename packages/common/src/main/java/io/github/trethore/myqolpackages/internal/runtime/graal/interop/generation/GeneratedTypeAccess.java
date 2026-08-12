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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

final class GeneratedTypeAccess {
    private GeneratedTypeAccess() {}

    static boolean isConstructorAccessible(Constructor<?> constructor, String generatedBinaryName) {
        return isMemberAccessible(constructor.getDeclaringClass(), constructor.getModifiers(), generatedBinaryName);
    }

    static boolean isMemberAccessible(Class<?> declaringClass, int modifiers, String generatedBinaryName) {
        if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
            return true;
        }
        return !Modifier.isPrivate(modifiers)
                && packageName(declaringClass.getName()).equals(packageName(generatedBinaryName));
    }

    static void requireTypeAccessible(Class<?> type, String generatedBinaryName) {
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

    private static String packageName(String binaryName) {
        int separator = binaryName.lastIndexOf('.');
        return separator < 0 ? "" : binaryName.substring(0, separator);
    }
}

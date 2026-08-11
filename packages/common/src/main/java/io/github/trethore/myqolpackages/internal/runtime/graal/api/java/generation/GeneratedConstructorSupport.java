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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

final class GeneratedConstructorSupport {
    private GeneratedConstructorSupport() {}

    static List<Constructor<?>> accessibleConstructors(Class<?> superclass, String generatedBinaryName) {
        return Arrays.stream(superclass.getDeclaredConstructors())
                .filter(constructor -> isAccessible(constructor, generatedBinaryName))
                .sorted(Comparator.comparing(Constructor::toGenericString))
                .toList();
    }

    private static boolean isAccessible(Constructor<?> constructor, String generatedBinaryName) {
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
}

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

import io.github.trethore.myqolpackages.internal.runtime.graal.interop.JavaInteropAccess;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.polyglot.Value;

final class GeneratedJavaTypeResolver {
    private final JavaInteropAccess interop;

    GeneratedJavaTypeResolver(JavaInteropAccess interop) {
        this.interop = interop;
    }

    Class<?> resolve(Value value, String description, boolean allowVoid) {
        Class<?> type;
        try {
            type = interop.resolveClass(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(description + " must be a Java type", exception);
        }
        if (!allowVoid && type == Void.TYPE) {
            throw new IllegalArgumentException(description + " cannot be void");
        }
        return type;
    }

    List<Class<?>> resolveSingleOrArray(Value value, String description, boolean allowVoid) {
        if (value == null || value.isNull()) {
            return List.of();
        }
        if (!value.hasArrayElements()) {
            return List.of(resolve(value, description, allowVoid));
        }
        long size = value.getArraySize();
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(description + " contains too many types");
        }
        List<Class<?>> types = new ArrayList<>((int) size);
        for (long index = 0; index < size; index++) {
            types.add(resolve(value.getArrayElement(index), description + "[" + index + "]", allowVoid));
        }
        return List.copyOf(types);
    }
}

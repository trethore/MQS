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

import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

public record GeneratedConstructorSelection(int constructorIndex, Object[] arguments) {
    @Override
    public boolean equals(Object object) {
        return object instanceof GeneratedConstructorSelection(int otherIndex, Object[] otherArguments)
                && constructorIndex == otherIndex
                && Arrays.deepEquals(arguments, otherArguments);
    }

    @Override
    public int hashCode() {
        return 31 * Integer.hashCode(constructorIndex) + Arrays.deepHashCode(arguments);
    }

    @Override
    public @NotNull String toString() {
        return "GeneratedConstructorSelection[constructorIndex="
                + constructorIndex
                + ", arguments="
                + Arrays.deepToString(arguments)
                + "]";
    }
}

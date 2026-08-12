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

import net.bytebuddy.description.modifier.Visibility;
import org.graalvm.polyglot.Value;

public enum JavaVisibility {
    PRIVATE(Visibility.PRIVATE),
    PACKAGE(Visibility.PACKAGE_PRIVATE),
    PROTECTED(Visibility.PROTECTED),
    PUBLIC(Visibility.PUBLIC);

    private final Visibility byteBuddyVisibility;

    JavaVisibility(Visibility byteBuddyVisibility) {
        this.byteBuddyVisibility = byteBuddyVisibility;
    }

    public Visibility byteBuddyVisibility() {
        return byteBuddyVisibility;
    }

    static JavaVisibility read(Value definition, String member, JavaVisibility defaultValue) {
        if (!definition.hasMember(member)) {
            return defaultValue;
        }
        Value value = definition.getMember(member);
        if (value.isHostObject() && value.asHostObject() instanceof JavaVisibility visibility) {
            return visibility;
        }
        throw new IllegalArgumentException(member + " must be an mqp.java.visibility value");
    }
}

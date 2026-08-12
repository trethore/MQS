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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GeneratedTypeNameResolverTest {
    private final GeneratedTypeNameResolver resolver = new GeneratedTypeNameResolver();

    @Test
    void hashesSimpleClassAndInterfaceNames() {
        String hash = "a8db796cd28c7f1366515107a32045d8a574df980112f6a89b9f080298b82763";

        assertEquals(
                "mqp.generated.clazz.p" + hash + ".Example",
                resolver.resolve("Example", "example-package", GeneratedTypeDefinition.Kind.CLASS));
        assertEquals(
                "mqp.generated.iface.p" + hash + ".Example",
                resolver.resolve("Example", "example-package", GeneratedTypeDefinition.Kind.INTERFACE));
    }

    @Test
    void preservesValidFullyQualifiedNames() {
        assertEquals(
                "example.generated.Type",
                resolver.resolve("example.generated.Type", "example-package", GeneratedTypeDefinition.Kind.CLASS));
    }

    @Test
    void rejectsInvalidAndReservedNames() {
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve("bad-name", "example-package", GeneratedTypeDefinition.Kind.CLASS));
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve("java.lang.Bad", "example-package", GeneratedTypeDefinition.Kind.CLASS));
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(" example.Type", "example-package", GeneratedTypeDefinition.Kind.CLASS));
    }
}

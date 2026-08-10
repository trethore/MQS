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
package io.github.trethore.myqolpackages.internal.trust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SemanticVersionTest {
    @Test
    void parsesAndFormatsCompleteVersions() {
        SemanticVersion version = SemanticVersion.parse("1.2.3-alpha.1+build.5");

        assertEquals(1, version.major());
        assertEquals(2, version.minor());
        assertEquals(3, version.patch());
        assertEquals("1.2.3-alpha.1+build.5", version.toString());
    }

    @Test
    void comparesPrereleasesAccordingToSemver() {
        assertTrue(SemanticVersion.parse("1.0.0-alpha").compareTo(SemanticVersion.parse("1.0.0-alpha.1")) < 0);
        assertTrue(SemanticVersion.parse("1.0.0-beta.11").compareTo(SemanticVersion.parse("1.0.0-rc.1")) < 0);
        assertTrue(SemanticVersion.parse("1.0.0-rc.1").compareTo(SemanticVersion.parse("1.0.0")) < 0);
    }

    @Test
    void rejectsInvalidVersions() {
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.parse("1.2"));
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.parse("01.2.3"));
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.parse("1.2.3-01"));
    }
}

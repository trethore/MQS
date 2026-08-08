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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrustedVersionRangeTest {
  @Test
  void matchesExactVersionsIncludingBuildMetadata() {
    TrustedVersionRange range = TrustedVersionRange.parse("=1.2.3+one");

    assertTrue(range.matches(SemanticVersion.parse("1.2.3+one")));
    assertFalse(range.matches(SemanticVersion.parse("1.2.3+two")));
  }

  @Test
  void matchesPatchUpdates() {
    TrustedVersionRange range = TrustedVersionRange.parse("~1.2.3");

    assertTrue(range.matches(SemanticVersion.parse("1.2.9")));
    assertFalse(range.matches(SemanticVersion.parse("1.3.0")));
  }

  @Test
  void matchesCompatibleUpdates() {
    assertTrue(TrustedVersionRange.parse("^1.2.3").matches(SemanticVersion.parse("1.9.0")));
    assertFalse(TrustedVersionRange.parse("^1.2.3").matches(SemanticVersion.parse("2.0.0")));
    assertTrue(TrustedVersionRange.parse("^0.2.3").matches(SemanticVersion.parse("0.2.9")));
    assertFalse(TrustedVersionRange.parse("^0.2.3").matches(SemanticVersion.parse("0.3.0")));
    assertFalse(TrustedVersionRange.parse("^0.0.3").matches(SemanticVersion.parse("0.0.4")));
  }

  @Test
  void wildcardMatchesPrereleases() {
    assertTrue(TrustedVersionRange.parse("*").matches(SemanticVersion.parse("2.0.0-beta.1")));
  }
}

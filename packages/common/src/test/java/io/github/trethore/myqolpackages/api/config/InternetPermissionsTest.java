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
package io.github.trethore.myqolpackages.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class InternetPermissionsTest {
  @Test
  void normalizesAndDeduplicatesDomains() {
    InternetPermissions permissions =
        InternetPermissions.domains(List.of("API.Example.com.", "api.example.com"));

    assertEquals(List.of("api.example.com"), permissions.domains());
  }

  @Test
  void wildcardMatchesSubdomainsButNotApex() {
    InternetPermissions permissions = InternetPermissions.domains(List.of("*.example.com"));

    assertTrue(permissions.allowsHost("api.example.com"));
    assertTrue(permissions.allowsHost("nested.api.example.com"));
    assertFalse(permissions.allowsHost("example.com"));
    assertFalse(permissions.allowsHost("notexample.com"));
  }

  @Test
  void broaderWildcardCoversNarrowerRequests() {
    InternetPermissions granted = InternetPermissions.domains(List.of("*.example.com"));

    assertTrue(granted.allows(InternetPermissions.domains(List.of("api.example.com"))));
    assertTrue(granted.allows(InternetPermissions.domains(List.of("*.api.example.com"))));
    assertFalse(granted.allows(InternetPermissions.domains(List.of("example.com"))));
  }

  @Test
  void rejectsInvalidWildcardLocationsAndUrls() {
    List<String> embeddedWildcard = List.of("api.*.example.com");
    List<String> url = List.of("https://example.com");
    List<String> bareWildcard = List.of("*");

    assertThrows(
        IllegalArgumentException.class, () -> InternetPermissions.domains(embeddedWildcard));
    assertThrows(IllegalArgumentException.class, () -> InternetPermissions.domains(url));
    assertThrows(IllegalArgumentException.class, () -> InternetPermissions.domains(bareWildcard));
  }
}

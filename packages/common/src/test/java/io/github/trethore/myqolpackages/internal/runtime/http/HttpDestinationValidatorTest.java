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
package io.github.trethore.myqolpackages.internal.runtime.http;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.trethore.myqolpackages.api.config.InternetPermissions;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class HttpDestinationValidatorTest {
  @Test
  void rejectsUnsupportedSchemesAndCredentials() {
    URI unsupportedScheme = URI.create("ftp://example.com/file");
    URI credentials = URI.create("https://user:secret@example.com/");
    InternetPermissions permissions = InternetPermissions.full();

    assertThrows(
        IllegalArgumentException.class,
        () -> HttpDestinationValidator.validate(unsupportedScheme, permissions));
    assertThrows(
        IllegalArgumentException.class,
        () -> HttpDestinationValidator.validate(credentials, permissions));
  }

  @Test
  void rejectsHostsOutsideDomainPermission() {
    InternetPermissions permissions = InternetPermissions.domains(List.of("api.example.com"));
    URI uri = URI.create("https://example.org/");

    assertThrows(
        SecurityException.class, () -> HttpDestinationValidator.validate(uri, permissions));
  }

  @Test
  void rejectsLocalAndPrivateDestinations() {
    URI localhost = URI.create("http://localhost/");
    URI loopback = URI.create("http://127.0.0.1/");
    URI privateAddress = URI.create("http://192.168.1.1/");
    InternetPermissions permissions = InternetPermissions.full();

    assertThrows(
        SecurityException.class, () -> HttpDestinationValidator.validate(localhost, permissions));
    assertThrows(
        SecurityException.class, () -> HttpDestinationValidator.validate(loopback, permissions));
    assertThrows(
        SecurityException.class,
        () -> HttpDestinationValidator.validate(privateAddress, permissions));
  }
}

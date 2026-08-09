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
package io.github.trethore.myqolpackages.internal.runtime.api.fetch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.myqolpackages.internal.runtime.http.PackageHttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.junit.jupiter.api.Test;

class FetchResponseTest {
  @Test
  void consumesTextOnlyOnce() {
    FetchResponse response = createResponse("text".getBytes(StandardCharsets.UTF_8));

    assertFalse(response.isBodyUsed());
    assertEquals("text", response.consumeText());
    assertTrue(response.isBodyUsed());
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, response::consumeBytes);
    assertEquals("Response body has already been used", exception.getMessage());
  }

  @Test
  void exposesUnsignedResponseBytes() {
    FetchResponse response = createResponse(new byte[] {0, 1, 127, (byte) 128, (byte) 255});

    ProxyArray bytes = response.consumeBytes();
    assertEquals(5, bytes.getSize());
    assertEquals(0, bytes.get(0));
    assertEquals(1, bytes.get(1));
    assertEquals(127, bytes.get(2));
    assertEquals(128, bytes.get(3));
    assertEquals(255, bytes.get(4));
  }

  @Test
  void mapsResponseStatusProperties() {
    FetchResponse response =
        new FetchResponse(
            new PackageHttpResponse(201, "https://example.com", true, Map.of(), new byte[0]));

    assertEquals(201, response.getMember("status"));
    assertEquals("Created", response.getMember("statusText"));
    assertEquals(Boolean.TRUE, response.getMember("ok"));
    assertEquals(Boolean.TRUE, response.getMember("redirected"));
  }

  private static FetchResponse createResponse(byte[] body) {
    return new FetchResponse(
        new PackageHttpResponse(200, "https://example.com", false, Map.of(), body));
  }
}

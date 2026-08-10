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
package io.github.trethore.myqolpackages.internal.runtime.graal.api.fetch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FetchHeadersTest {
    @Test
    void readsNormalizedHeaders() {
        Map<String, List<String>> values = new LinkedHashMap<>();
        values.put("X-First", List.of("one", "two"));
        values.put("X-Second", List.of("three"));
        values.put("X-Empty", List.of(""));
        FetchHeaders headers = new FetchHeaders(values);

        assertEquals("one, two", headers.get("x-FIRST"));
        assertTrue(headers.has("X-Second"));
        assertFalse(headers.has("missing"));
        assertNull(headers.get("missing"));
        assertEquals("", headers.get("X-Empty"));
    }
}

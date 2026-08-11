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
package io.github.trethore.myqolpackages.internal.runtime.graal.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MqpApiBuilder {
    private static final Set<String> RESERVED_MEMBERS = Set.of("version", "dataDirectory", "package");

    private final Map<String, Object> members = new LinkedHashMap<>();

    public void define(String name, Object value) {
        requireName(name);
        Objects.requireNonNull(value, "value");
        if (RESERVED_MEMBERS.contains(name)) {
            throw new IllegalArgumentException("MQP API member name is reserved: " + name);
        }
        if (members.putIfAbsent(name, value) != null) {
            throw new IllegalArgumentException("Duplicate MQP API member: " + name);
        }
    }

    Map<String, Object> members() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(members));
    }

    private static void requireName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("MQP API member name must not be blank");
        }
    }
}

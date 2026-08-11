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

public final class ApiObjectBuilder {
    private final PackageApiBuilder.ObjectNode node;

    ApiObjectBuilder(PackageApiBuilder.ObjectNode node) {
        this.node = node;
    }

    public void define(String name, Object value) {
        node.define(name, value);
    }

    public ApiObjectBuilder defineObject(String name) {
        return new ApiObjectBuilder(node.defineObject(name));
    }

    public ApiObjectBuilder object(String name) {
        return new ApiObjectBuilder(node.object(name));
    }
}

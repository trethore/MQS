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

import io.github.trethore.myqolpackages.internal.runtime.graal.api.js.JavaScriptGlobalSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class GlobalApiRegistry {
    private static final String MQP_GLOBAL = "mqp";

    private final Map<String, Object> values = new LinkedHashMap<>();
    private final JavaScriptGlobalSupport javaScriptGlobals;

    GlobalApiRegistry(JavaScriptGlobalSupport javaScriptGlobals) {
        this.javaScriptGlobals = Objects.requireNonNull(javaScriptGlobals, "javaScriptGlobals");
    }

    public void define(String name, Object value) {
        requireName(name);
        Objects.requireNonNull(value, "value");
        if (MQP_GLOBAL.equals(name)) {
            throw new IllegalArgumentException("Global name is reserved: " + name);
        }
        if (values.putIfAbsent(name, value) != null) {
            throw new IllegalArgumentException("Duplicate package API global: " + name);
        }
    }

    void install(Object mqp) {
        Objects.requireNonNull(mqp, "mqp");
        requireAvailable(MQP_GLOBAL);
        for (String name : values.keySet()) {
            requireAvailable(name);
        }
        javaScriptGlobals.defineGlobal(MQP_GLOBAL, mqp);
        values.forEach(javaScriptGlobals::defineGlobal);
    }

    private void requireAvailable(String name) {
        if (javaScriptGlobals.hasOwnGlobal(name)) {
            throw new IllegalStateException(
                    "MQP cannot install global because globalThis already contains it: " + name);
        }
    }

    private static void requireName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Global name must not be blank");
        }
    }
}

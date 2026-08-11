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
package io.github.trethore.myqolpackages.internal.runtime.graal.api.js;

import org.graalvm.polyglot.Value;

public final class JavaScriptGlobalSupport {
    private final Value defineGlobal;
    private final Value hasOwnGlobal;

    JavaScriptGlobalSupport(Value support) {
        this.defineGlobal = JavaScriptModuleLoader.requireFunction(support, "defineGlobal");
        this.hasOwnGlobal = JavaScriptModuleLoader.requireFunction(support, "hasOwnGlobal");
    }

    public void defineGlobal(String name, Object value) {
        defineGlobal.execute(name, value);
    }

    public boolean hasOwnGlobal(String name) {
        return hasOwnGlobal.execute(name).asBoolean();
    }
}

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
package io.github.trethore.myqolpackages.internal.runtime.graal.js;

import org.graalvm.polyglot.Value;

public final class JavaScriptValueSupport {
    private final Value isArray;
    private final Value isObject;
    private final Value isUndefined;
    private final Value ownKeys;
    private final Value stringify;

    JavaScriptValueSupport(Value support) {
        this.isArray = JavaScriptModuleLoader.requireFunction(support, "isArray");
        this.isObject = JavaScriptModuleLoader.requireFunction(support, "isObject");
        this.isUndefined = JavaScriptModuleLoader.requireFunction(support, "isUndefined");
        this.ownKeys = JavaScriptModuleLoader.requireFunction(support, "ownKeys");
        this.stringify = JavaScriptModuleLoader.requireFunction(support, "stringify");
    }

    public boolean isArray(Value value) {
        return isArray.execute(value).asBoolean();
    }

    public boolean isObject(Value value) {
        return isObject.execute(value).asBoolean();
    }

    public boolean isUndefined(Value value) {
        return isUndefined.execute(value).asBoolean();
    }

    public Value ownKeys(Value value) {
        return ownKeys.execute(value);
    }

    public String stringify(Value value) {
        return stringify.execute(value).asString();
    }
}

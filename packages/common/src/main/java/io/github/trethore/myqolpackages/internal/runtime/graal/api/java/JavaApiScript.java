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
package io.github.trethore.myqolpackages.internal.runtime.graal.api.java;

import io.github.trethore.myqolpackages.internal.runtime.graal.interop.generation.JavaVisibility;
import io.github.trethore.myqolpackages.internal.runtime.graal.js.JavaScriptModuleLoader;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

final class JavaApiScript {
    private JavaApiScript() {}

    static Value create(
            JavaScriptModuleLoader moduleLoader, ProxyExecutable defineClass, ProxyExecutable defineInterface) {
        Value createJavaApi = moduleLoader.loadFunction(JavaApiScript.class, "java.js");
        return createJavaApi.execute(
                defineClass,
                defineInterface,
                Void.TYPE,
                Boolean.TYPE,
                Byte.TYPE,
                Short.TYPE,
                Integer.TYPE,
                Long.TYPE,
                Float.TYPE,
                Double.TYPE,
                Character.TYPE,
                JavaVisibility.PRIVATE,
                JavaVisibility.PACKAGE,
                JavaVisibility.PROTECTED,
                JavaVisibility.PUBLIC);
    }
}

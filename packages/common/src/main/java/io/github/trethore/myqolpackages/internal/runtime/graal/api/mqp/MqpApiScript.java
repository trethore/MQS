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
package io.github.trethore.myqolpackages.internal.runtime.graal.api.mqp;

import io.github.trethore.myqolpackages.internal.runtime.PackageContextSpec;
import io.github.trethore.myqolpackages.internal.runtime.graal.js.JavaScriptModuleLoader;
import java.util.Map;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public final class MqpApiScript {
    private MqpApiScript() {}

    public static Value create(
            JavaScriptModuleLoader moduleLoader,
            String mqpVersion,
            PackageContextSpec spec,
            Map<String, Object> members) {
        ProxyObject memberObject = ProxyObject.fromMap(members);
        Value createMqp = moduleLoader.loadFunction(MqpApiScript.class, "mqp.js");
        return createMqp.execute(mqpVersion, spec.dataDirectory().toString(), spec.packageId(), memberObject);
    }
}

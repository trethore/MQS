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

import io.github.trethore.myqolpackages.api.MqpRuntimeEnvironment;
import io.github.trethore.myqolpackages.internal.runtime.graal.api.PackageApiInstallContext;
import io.github.trethore.myqolpackages.internal.runtime.graal.api.PackageApiModule;
import io.github.trethore.myqolpackages.internal.runtime.graal.api.PackageApiSession;
import io.github.trethore.myqolpackages.internal.runtime.graal.api.java.interop.ClassInteropBridge;
import io.github.trethore.myqolpackages.internal.runtime.graal.api.java.interop.ClassInteropBridgeFactory;

public final class JavaApiModule implements PackageApiModule {
    private final ClassInteropBridgeFactory bridgeFactory;

    public JavaApiModule(MqpRuntimeEnvironment environment) {
        bridgeFactory = new ClassInteropBridgeFactory(environment);
    }

    @Override
    public PackageApiSession install(PackageApiInstallContext context) {
        ClassInteropBridge interopBridge = bridgeFactory.create();
        context.mqp().define("java", JavaApiScript.create(context.javaScriptModules()));
        context.globals().define("importClass", interopBridge.importClass());
        context.globals().define("wrap", interopBridge.wrap());
        context.globals().define("packages", interopBridge.packages());
        context.globals().define("net", interopBridge.net());
        return PackageApiSession.empty();
    }
}

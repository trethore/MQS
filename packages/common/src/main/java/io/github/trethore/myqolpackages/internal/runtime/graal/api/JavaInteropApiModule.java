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

import io.github.trethore.myqolpackages.api.MqpRuntimeEnvironment;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextSpec;
import io.github.trethore.myqolpackages.internal.runtime.graal.interop.ClassInteropBridge;
import io.github.trethore.myqolpackages.internal.runtime.graal.interop.ClassInteropBridgeFactory;

final class JavaInteropApiModule implements PackageApiModule {
  private final ClassInteropBridgeFactory bridgeFactory;

  JavaInteropApiModule(MqpRuntimeEnvironment environment) {
    bridgeFactory = new ClassInteropBridgeFactory(environment);
  }

  @Override
  public PackageApiSession install(JavaScriptApiBridge bridge, PackageContextSpec spec) {
    ClassInteropBridge interopBridge = bridgeFactory.create();
    bridge.defineGlobal("importClass", interopBridge.importClass());
    bridge.defineGlobal("wrap", interopBridge.wrap());
    bridge.defineGlobal("packages", interopBridge.packages());
    bridge.defineGlobal("net", interopBridge.net());
    return PackageApiSession.empty();
  }
}

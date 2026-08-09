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
package io.github.trethore.myqolpackages.internal.runtime.api;

import io.github.trethore.myqolpackages.api.MqpRuntimeEnvironment;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextSpec;
import io.github.trethore.myqolpackages.internal.runtime.api.fetch.FetchApi;
import io.github.trethore.myqolpackages.internal.runtime.http.PackageHttpClient;
import io.github.trethore.myqolpackages.internal.runtime.interop.ClassInteropBridge;
import io.github.trethore.myqolpackages.internal.runtime.interop.ClassInteropBridgeFactory;
import java.util.Objects;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public final class MqpApiInstaller {
  private final ClassInteropBridgeFactory classInteropBridgeFactory;
  private final String mqpVersion;

  public MqpApiInstaller(String mqpVersion, MqpRuntimeEnvironment environment) {
    this.mqpVersion = Objects.requireNonNull(mqpVersion, "mqpVersion");
    this.classInteropBridgeFactory =
        new ClassInteropBridgeFactory(Objects.requireNonNull(environment, "environment"));
  }

  public MqpApiContext install(
      Context context, PackageContextSpec spec, PackageHttpClient packageHttpClient) {
    JsRuntimeAdapter adapter = new JsRuntimeAdapter(context);
    FetchApi fetchApi = new FetchApi(adapter, packageHttpClient);
    installMetadata(adapter, spec);
    installJavaInterop(adapter);
    installFetch(adapter, fetchApi);
    return new MqpApiContext(fetchApi, packageHttpClient);
  }

  private void installMetadata(JsRuntimeAdapter adapter, PackageContextSpec spec) {
    Value metadata =
        adapter.createMetadata(mqpVersion, spec.dataDirectory().toString(), spec.packageId());
    adapter.defineGlobal("mqp", metadata);
  }

  private void installJavaInterop(JsRuntimeAdapter adapter) {
    ClassInteropBridge bridge = classInteropBridgeFactory.create();
    adapter.defineGlobal("importClass", bridge.importClass());
    adapter.defineGlobal("wrap", bridge.wrap());
    adapter.defineGlobal("packages", bridge.packages());
    adapter.defineGlobal("net", bridge.net());
  }

  private static void installFetch(JsRuntimeAdapter adapter, FetchApi fetchApi) {
    adapter.defineGlobal("fetch", adapter.createFetch(fetchApi));
  }
}

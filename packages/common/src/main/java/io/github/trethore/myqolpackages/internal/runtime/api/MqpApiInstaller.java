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
import io.github.trethore.myqolpackages.internal.runtime.api.MqpApiSourceLoader.MqpApiSources;
import io.github.trethore.myqolpackages.internal.runtime.http.PackageHttpClient;
import io.github.trethore.myqolpackages.internal.runtime.interop.ClassInteropBridgeFactory;
import java.util.Map;
import java.util.Objects;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public final class MqpApiInstaller {
  private static final MqpApiSources SOURCES = MqpApiSourceLoader.load();

  private final ClassInteropBridgeFactory classInteropBridgeFactory;
  private final String mqpVersion;

  public MqpApiInstaller(String mqpVersion, MqpRuntimeEnvironment environment) {
    this.mqpVersion = Objects.requireNonNull(mqpVersion, "mqpVersion");
    this.classInteropBridgeFactory =
        new ClassInteropBridgeFactory(Objects.requireNonNull(environment, "environment"));
  }

  public void install(
      Context context, PackageContextSpec spec, PackageHttpClient packageHttpClient) {
    ProxyObject hostBridge = createHostBridge(spec, packageHttpClient);
    Value bootstrap = getInstaller(context, SOURCES.bootstrap()).execute();
    getInstaller(context, SOURCES.mqp()).execute(hostBridge, bootstrap);
    getInstaller(context, SOURCES.javaInterop()).execute(hostBridge, bootstrap);
    getInstaller(context, SOURCES.fetch()).execute(hostBridge, bootstrap);
  }

  private ProxyObject createHostBridge(
      PackageContextSpec spec, PackageHttpClient packageHttpClient) {
    ProxyObject metadata =
        ProxyObject.fromMap(
            Map.of(
                "version", mqpVersion,
                "packageId", spec.packageId(),
                "dataDirectory", spec.dataDirectory().toString()));
    ProxyObject interop = classInteropBridgeFactory.create();
    return ProxyObject.fromMap(
        Map.of(
            "metadata", metadata,
            "interop", interop,
            "fetch", packageHttpClient));
  }

  private static Value getInstaller(Context context, Source source) {
    Value installer = context.eval(source).getMember("default");
    if (installer == null || !installer.canExecute()) {
      throw new IllegalStateException(
          "JavaScript API resource must have a default function export: " + source.getName());
    }
    return installer;
  }
}

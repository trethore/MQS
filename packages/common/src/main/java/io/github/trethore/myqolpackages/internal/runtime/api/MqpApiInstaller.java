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
import io.github.trethore.myqolpackages.api.config.FileSystemPermissions;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextSpec;
import io.github.trethore.myqolpackages.internal.runtime.api.MqpApiSourceLoader.MqpApiSources;
import io.github.trethore.myqolpackages.internal.runtime.http.PackageHttpClient;
import io.github.trethore.myqolpackages.internal.runtime.interop.ClassInteropBridgeFactory;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
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
    Value bootstrap = context.eval(SOURCES.bootstrap()).execute();
    Value permissions = context.eval(SOURCES.permissions()).execute(hostBridge);
    context.eval(SOURCES.mqp()).execute(hostBridge, bootstrap, permissions);
    context.eval(SOURCES.javaInterop()).execute(hostBridge, bootstrap);
    context.eval(SOURCES.fetch()).execute(hostBridge, bootstrap);
  }

  private ProxyObject createHostBridge(
      PackageContextSpec spec, PackageHttpClient packageHttpClient) {
    FileSystemPermissions filesystemPermissions = spec.permissions().filesystem();
    ProxyObject metadata =
        ProxyObject.fromMap(Map.of("version", mqpVersion, "packageId", spec.packageId()));
    ProxyObject permissions =
        ProxyObject.fromMap(
            Map.of(
                "hostAccess", permissionName(spec.permissions().hostAccess()),
                "hostClassLookup", permissionName(spec.permissions().hostClassLookup()),
                "filesystemRead", permissionName(filesystemPermissions.read()),
                "filesystemWrite", permissionName(filesystemPermissions.write()),
                "internetAccess", permissionName(spec.permissions().internet().access()),
                "internetDomains",
                    ProxyArray.fromArray(spec.permissions().internet().domains().toArray())));
    ProxyObject interop =
        classInteropBridgeFactory.create(
            spec.permissions().hostAccess(), spec.permissions().hostClassLookup());
    return ProxyObject.fromMap(
        Map.of(
            "metadata", metadata,
            "permissions", permissions,
            "interop", interop,
            "fetch", packageHttpClient));
  }

  private static String permissionName(Enum<?> permission) {
    return permission.name().toLowerCase(Locale.ROOT);
  }
}

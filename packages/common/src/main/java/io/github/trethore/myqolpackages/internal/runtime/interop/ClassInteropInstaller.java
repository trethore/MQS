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
package io.github.trethore.myqolpackages.internal.runtime.interop;

import io.github.trethore.myqolpackages.api.MqpRuntimeEnvironment;
import io.github.trethore.myqolpackages.api.config.HostAccessPermission;
import io.github.trethore.myqolpackages.api.config.HostClassLookupPermission;
import io.github.trethore.myqolpackages.internal.mappings.ClassInteropMetadata;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public final class ClassInteropInstaller {
  private final MqpRuntimeEnvironment environment;
  private final ClassInteropMetadata metadata;

  public ClassInteropInstaller(MqpRuntimeEnvironment environment) {
    this.environment = environment;
    this.metadata = ClassInteropMetadata.load(environment);
  }

  public void install(
      Value bindings,
      HostAccessPermission hostAccessPermission,
      HostClassLookupPermission classLookupPermission) {
    HostClassResolver resolver =
        new HostClassResolver(metadata, environment, hostAccessPermission, classLookupPermission);
    JavaPackageProxy packages = new JavaPackageProxy("", resolver);
    ProxyExecutable importClass =
        arguments -> {
          if (arguments.length != 1 || !arguments[0].isString()) {
            throw new IllegalArgumentException(
                "importClass requires exactly one class name string");
          }
          return resolver.resolveImport(arguments[0].asString());
        };
    bindings.putMember("__mqpImportClass", importClass);
    bindings.putMember("__mqpPackages", packages);
    bindings.putMember("__mqpNet", packages.getNetPackage());
  }
}

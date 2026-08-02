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
package io.github.trethore.myqolpackages.api;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MqpRuntimeEnvironment(
    ClassLoader classLoader,
    List<String> minecraftNamespaces,
    Optional<String> classCatalogResource,
    Optional<String> mappingsResource) {
  public MqpRuntimeEnvironment {
    Objects.requireNonNull(classLoader, "classLoader");
    minecraftNamespaces =
        Objects.requireNonNull(minecraftNamespaces, "minecraftNamespaces").stream()
            .map(MqpRuntimeEnvironment::normalizeNamespace)
            .distinct()
            .toList();
    classCatalogResource =
        Objects.requireNonNull(classCatalogResource, "classCatalogResource")
            .map(MqpRuntimeEnvironment::normalizeResourceName);
    mappingsResource =
        Objects.requireNonNull(mappingsResource, "mappingsResource")
            .map(MqpRuntimeEnvironment::normalizeResourceName);
  }

  public static MqpRuntimeEnvironment identity(ClassLoader classLoader) {
    return new MqpRuntimeEnvironment(classLoader, List.of(), Optional.empty(), Optional.empty());
  }

  public boolean isMinecraftClass(String className) {
    for (String namespace : minecraftNamespaces) {
      if (className.startsWith(namespace)) {
        return true;
      }
    }
    return false;
  }

  public boolean isMinecraftNamespacePath(String path) {
    String packagePrefix = path.endsWith(".") ? path : path + ".";
    for (String namespace : minecraftNamespaces) {
      if (namespace.startsWith(packagePrefix) || path.startsWith(namespace)) {
        return true;
      }
    }
    return false;
  }

  private static String normalizeResourceName(String resourceName) {
    String normalizedName = resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;
    if (normalizedName.isBlank()) {
      throw new IllegalArgumentException("Resource name must not be blank");
    }
    return normalizedName;
  }

  private static String normalizeNamespace(String namespace) {
    String normalizedNamespace = Objects.requireNonNull(namespace, "namespace").trim();
    if (normalizedNamespace.isEmpty()) {
      throw new IllegalArgumentException("Minecraft namespace must not be blank");
    }
    return normalizedNamespace.endsWith(".") ? normalizedNamespace : normalizedNamespace + ".";
  }
}

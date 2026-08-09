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
package io.github.trethore.myqolpackages.internal.runtime.graal.interop;

import io.github.trethore.myqolpackages.api.MqpRuntimeEnvironment;
import io.github.trethore.myqolpackages.internal.runtime.graal.interop.mapping.ClassInteropMetadata;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.graalvm.polyglot.Value;

final class HostClassResolver {
  private final Map<ClassProxyKey, JavaClassProxy> classProxies = new HashMap<>();
  private final MqpRuntimeEnvironment environment;
  private final JavaInteropService interopService;
  private final ClassInteropMetadata metadata;

  HostClassResolver(ClassInteropMetadata metadata, MqpRuntimeEnvironment environment) {
    this.metadata = metadata;
    this.environment = environment;
    this.interopService = new JavaInteropService(metadata.mappings());
  }

  JavaClassProxy resolveImport(String requestedName) {
    String normalizedName = normalizeRequestedName(requestedName);
    Class<?> exactClass = resolveExactClass(normalizedName);
    if (exactClass != null) {
      return getOrCreateProxy(normalizedName, exactClass);
    }

    List<String> catalogMatches = metadata.catalog().findBySuffix(normalizedName);
    if (!catalogMatches.isEmpty()) {
      if (catalogMatches.size() > 1) {
        throw new IllegalArgumentException(
            "Ambiguous class name '"
                + normalizedName
                + "'. Possible matches: "
                + String.join(", ", catalogMatches));
      }
      String matchedClassName = catalogMatches.getFirst();
      Class<?> resolvedClass = resolveExactClass(matchedClassName);
      if (resolvedClass != null) {
        return getOrCreateProxy(matchedClassName, resolvedClass);
      }
    }

    throw new IllegalArgumentException("Unknown class: " + normalizedName);
  }

  JavaClassProxy resolvePackageMember(String className) {
    if (metadata.catalog().containsPackage(className)) {
      return null;
    }
    Class<?> resolvedClass = resolveExactClass(className);
    return resolvedClass == null ? null : getOrCreateProxy(className, resolvedClass);
  }

  Object wrap(Value value) {
    return interopService.wrap(value);
  }

  private Class<?> resolveExactClass(String namedClassName) {
    Class<?> identityClass = loadClass(namedClassName);
    if (identityClass != null) {
      return identityClass;
    }
    String runtimeClassName = metadata.mappings().getRuntimeClassName(namedClassName);
    if (runtimeClassName == null || runtimeClassName.equals(namedClassName)) {
      return null;
    }
    return loadClass(runtimeClassName);
  }

  private Class<?> loadClass(String className) {
    try {
      return Class.forName(className, false, environment.classLoader());
    } catch (ClassNotFoundException exception) {
      return null;
    }
  }

  private JavaClassProxy getOrCreateProxy(String namedClassName, Class<?> targetClass) {
    ClassProxyKey key = new ClassProxyKey(namedClassName, targetClass);
    return classProxies.computeIfAbsent(
        key, ignored -> new JavaClassProxy(namedClassName, targetClass, interopService));
  }

  private static String normalizeRequestedName(String requestedName) {
    if (requestedName == null) {
      throw new IllegalArgumentException("Class name must not be null");
    }
    String normalizedName = requestedName.trim().replace('/', '.');
    if (normalizedName.isEmpty()) {
      throw new IllegalArgumentException("Class name must not be empty");
    }
    return normalizedName;
  }

  private record ClassProxyKey(String namedClassName, Class<?> targetClass) {}
}

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

import java.util.HashMap;
import java.util.Map;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

final class JavaPackageProxy implements ProxyObject {
  private final Map<String, Object> members = new HashMap<>();
  private final String packageName;
  private final HostClassResolver resolver;

  JavaPackageProxy(String packageName, HostClassResolver resolver) {
    this.packageName = packageName;
    this.resolver = resolver;
  }

  @Override
  public Object getMember(String key) {
    validateMemberName(key);
    Object cachedMember = members.get(key);
    if (cachedMember != null) {
      return cachedMember;
    }
    String qualifiedName = packageName.isEmpty() ? key : packageName + "." + key;
    JavaClassProxy resolvedClass = resolver.resolvePackageMember(qualifiedName);
    if (resolvedClass != null) {
      members.put(key, resolvedClass);
      return resolvedClass;
    }
    return getOrCreatePackage(key, qualifiedName);
  }

  @Override
  public Object getMemberKeys() {
    return null;
  }

  @Override
  public boolean hasMember(String key) {
    return key != null && !key.isBlank();
  }

  @Override
  public void putMember(String key, Value value) {
    throw new UnsupportedOperationException("Java package proxies are read-only");
  }

  JavaPackageProxy getNetPackage() {
    return getOrCreatePackage("net", "net");
  }

  private JavaPackageProxy getOrCreatePackage(String key, String qualifiedName) {
    Object cachedMember = members.get(key);
    if (cachedMember instanceof JavaPackageProxy packageProxy) {
      return packageProxy;
    }
    JavaPackageProxy packageProxy = new JavaPackageProxy(qualifiedName, resolver);
    members.put(key, packageProxy);
    return packageProxy;
  }

  private static void validateMemberName(String key) {
    if (key == null || key.isBlank() || key.indexOf('.') >= 0) {
      throw new IllegalArgumentException("Invalid package member: " + key);
    }
  }
}

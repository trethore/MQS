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

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

class JavaClassProxy implements ProxyObject {
  private final String namedClassName;
  private final Class<?> targetClass;

  JavaClassProxy(String namedClassName, Class<?> targetClass) {
    this.namedClassName = namedClassName;
    this.targetClass = targetClass;
  }

  @Override
  public Object getMember(String key) {
    return null;
  }

  @Override
  public Object getMemberKeys() {
    return null;
  }

  @Override
  public boolean hasMember(String key) {
    return false;
  }

  @Override
  public void putMember(String key, Value value) {
    throw new UnsupportedOperationException("Java class proxy is opaque without full host access");
  }

  final String getNamedClassName() {
    return namedClassName;
  }

  final Class<?> getTargetClass() {
    return targetClass;
  }

  @Override
  public final String toString() {
    return namedClassName.equals(targetClass.getName())
        ? namedClassName
        : namedClassName + " -> " + targetClass.getName();
  }
}

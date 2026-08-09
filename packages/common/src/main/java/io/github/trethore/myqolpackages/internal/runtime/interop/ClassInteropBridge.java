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

import java.util.Objects;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

public record ClassInteropBridge(
    ProxyExecutable importClass, ProxyExecutable wrap, ProxyObject packages, ProxyObject net) {
  public ClassInteropBridge {
    Objects.requireNonNull(importClass, "importClass");
    Objects.requireNonNull(wrap, "wrap");
    Objects.requireNonNull(packages, "packages");
    Objects.requireNonNull(net, "net");
  }
}

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
package io.github.trethore.myqolpackages.internal.runtime.graal.interop.generation;

import java.util.LinkedHashMap;
import java.util.Map;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

abstract class AbstractTypeBuilderProxy implements ProxyObject {
    private final Map<String, ProxyExecutable> members = new LinkedHashMap<>();
    private boolean built;

    AbstractTypeBuilderProxy() {}

    @Override
    public final Object getMember(String key) {
        return members.get(key);
    }

    @Override
    public final Object getMemberKeys() {
        return members.keySet().toArray(String[]::new);
    }

    @Override
    public final boolean hasMember(String key) {
        return members.containsKey(key);
    }

    @Override
    public final void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Generated type builders cannot be modified directly");
    }

    final void requireMutable() {
        if (built) {
            throw new IllegalStateException("Generated type builder has already been built");
        }
    }

    final Object completeBuild(BuildOperation operation) {
        requireMutable();
        Object result = operation.build();
        built = true;
        return result;
    }

    final void defineMember(String name, ProxyExecutable executable) {
        members.put(name, executable);
    }

    @FunctionalInterface
    interface BuildOperation {
        Object build();
    }
}

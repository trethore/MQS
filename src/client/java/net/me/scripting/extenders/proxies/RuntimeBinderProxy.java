/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
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

package net.me.scripting.extenders.proxies;

import lombok.Setter;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Map;

public class RuntimeBinderProxy implements ProxyObject {
    private final Map<String, Object> originalOverrides;
    @Setter
    private ExtendedInstanceProxy bindingTarget;

    public RuntimeBinderProxy(Map<String, Object> originalOverrides) {
        this.originalOverrides = originalOverrides;
    }

    @Override
    public Object getMember(String key) {
        Object member = originalOverrides.get(key);
        if (member instanceof Value func && func.canExecute()) {
            return (ProxyExecutable) proxyArgs -> {
                if (bindingTarget == null) {
                    throw new IllegalStateException("Binding target not set on RuntimeBinderProxy before method invocation.");
                }
                return func.invokeMember("apply", bindingTarget, proxyArgs);
            };
        }
        return member;
    }

    @Override
    public Object getMemberKeys() {
        return originalOverrides.keySet().toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        return originalOverrides.containsKey(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot set members on a RuntimeBinderProxy.");
    }
}
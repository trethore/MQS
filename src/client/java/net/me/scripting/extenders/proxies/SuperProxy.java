/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
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

package net.me.scripting.extenders.proxies;

import net.me.scripting.WrapperConstants;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.*;

public class SuperProxy implements ProxyObject {
    private final Value parentOverrides;
    private final Value grandParentSuper;
    private final Value childInstance;
    private final Map<String, List<String>> methodMappings;

    public SuperProxy(Value parentOverrides, Value grandParentSuper, Value childInstance, Map<String, List<String>> methodMappings) {
        this.parentOverrides = parentOverrides;
        this.grandParentSuper = grandParentSuper != null ? grandParentSuper : Value.asValue(Collections.emptyMap());
        this.childInstance = childInstance;
        this.methodMappings = methodMappings != null ? methodMappings : Collections.emptyMap();
    }

    @Override
    public Object getMember(String key) {
        Value override = getParentOverride(key);
        if (override != null) {
            return wrapParentOverride(override);
        }

        Value mappedMember = resolveMappedMember(key);
        if (mappedMember != null) {
            return mappedMember;
        }

        return grandParentSuper.getMember(key);
    }

    private Value getParentOverride(String key) {
        if (parentOverrides == null || !parentOverrides.hasMember(key)) {
            return null;
        }
        return parentOverrides.getMember(key);
    }

    private Object wrapParentOverride(Value parentFunction) {
        if (!parentFunction.canExecute()) {
            return parentFunction;
        }
        return (ProxyExecutable) args -> parentFunction.invokeMember("apply", createTemporaryThis(), args);
    }

    private Value resolveMappedMember(String key) {
        List<String> runtimeNames = this.methodMappings.get(key);
        if (runtimeNames == null || runtimeNames.isEmpty()) {
            return null;
        }
        for (String runtimeName : runtimeNames) {
            if (grandParentSuper.hasMember(runtimeName)) {
                return grandParentSuper.getMember(runtimeName);
            }
        }
        return null;
    }

    private ProxyObject createTemporaryThis() {
        return new ProxyObject() {
            @Override
            public Object getMember(String memberKey) {
                return WrapperConstants.SUPER.equals(memberKey) ? grandParentSuper : childInstance.getMember(memberKey);
            }

            @Override
            public boolean hasMember(String memberKey) {
                return WrapperConstants.SUPER.equals(memberKey) || childInstance.hasMember(memberKey);
            }

            @Override
            public void putMember(String memberKey, Value value) {
                childInstance.putMember(memberKey, value);
            }

            @Override
            public Object getMemberKeys() {
                return childInstance.getMemberKeys();
            }
        };
    }

    @Override
    public Object getMemberKeys() {
        Set<String> combinedKeys = new HashSet<>();
        if (this.parentOverrides != null) {
            combinedKeys.addAll(this.parentOverrides.getMemberKeys());
        }
        if (this.grandParentSuper != null) {
            combinedKeys.addAll(this.grandParentSuper.getMemberKeys());
        }
        return combinedKeys.toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        if (parentOverrides != null && parentOverrides.hasMember(key)) {
            return true;
        }
        List<String> runtimeNames = this.methodMappings.get(key);
        if (runtimeNames != null && !runtimeNames.isEmpty()) {
            for (String runtimeName : runtimeNames) {
                if (grandParentSuper.hasMember(runtimeName)) {
                    return true;
                }
            }
        }
        return grandParentSuper.hasMember(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify a _super object.");
    }
}

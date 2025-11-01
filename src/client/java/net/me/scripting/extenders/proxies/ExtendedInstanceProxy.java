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

import lombok.Getter;
import lombok.Setter;
import net.me.scripting.WrapperConstants;
import net.me.scripting.config.ExtensionConfig;
import net.me.scripting.utils.ScriptUtils;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ExtendedInstanceProxy implements ProxyObject {
    private final Map<String, Object> properties;
    @Getter
    private final Object baseInstance;
    @Getter
    private final Value originalOverrides;
    @Getter
    private final Value originalAddons;
    @Getter
    private final ExtensionConfig originalConfig;

    @Setter
    private MappedInstanceProxy javaInstanceProxy;

    public ExtendedInstanceProxy(Map<String, Object> properties, Object baseInstance, ExtensionConfig originalConfig, Value originalOverrides, Value originalAddons) {
        this.properties = properties;
        this.baseInstance = baseInstance;
        this.originalConfig = originalConfig;
        this.originalOverrides = originalOverrides;
        this.originalAddons = originalAddons;
    }

    @Override
    public Object getMember(String key) {
        if (WrapperConstants.INSTANCE_OF.equals(key)) {
            return (ProxyExecutable) (Value... args) -> {
                if (args.length != 1) {
                    throw new IllegalArgumentException("_instanceof(class) requires exactly one argument.");
                }

                Value classValue = args[0];
                Class<?> rawClass;

                if (classValue == null || classValue.isNull()) {
                    throw new IllegalArgumentException("The argument to _instanceof cannot be null.");
                }
                Object proxy = classValue.isProxyObject() ? classValue.asProxyObject() : null;
                if (proxy instanceof net.me.scripting.wrappers.LazyJsClassHolder holder) {
                    rawClass = holder.getWrapper().getTargetClass();
                } else if (proxy instanceof net.me.scripting.wrappers.JsClassWrapper wrapper) {
                    rawClass = wrapper.getTargetClass();
                } else {
                    Object unwrapped = ScriptUtils.unwrapReceiver(classValue);
                    if (unwrapped instanceof Class) {
                        rawClass = (Class<?>) unwrapped;
                    } else {
                        throw new IllegalArgumentException("The argument to _instanceof must be a class.");
                    }
                }

                return rawClass.isInstance(this.baseInstance);
            };
        }
        if (WrapperConstants.EQUALS.equals(key)) {
            return (ProxyExecutable) (Value... args) -> {
                if (args.length != 1) return false;
                Object otherRaw = ScriptUtils.unwrapReceiver(args[0]);
                return this.baseInstance.equals(otherRaw);
            };
        }

        if (properties.containsKey(key)) {
            if (WrapperConstants.SELF.equals(key)) {
                return baseInstance;
            }
            return properties.get(key);
        }

        if (javaInstanceProxy != null && javaInstanceProxy.hasMember(key)) {
            return javaInstanceProxy.getMember(key);
        }

        return null;
    }

    @Override
    public Object getMemberKeys() {
        Set<String> keys = new HashSet<>(properties.keySet());
        if (javaInstanceProxy != null) {
            String[] javaKeys = (String[]) javaInstanceProxy.getMemberKeys();
            keys.addAll(Set.of(javaKeys));
        }
        keys.add(WrapperConstants.EQUALS);
        keys.add(WrapperConstants.INSTANCE_OF);
        return keys.toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        return properties.containsKey(key)
                || (javaInstanceProxy != null && javaInstanceProxy.hasMember(key))
                || WrapperConstants.EQUALS.equals(key)
                || WrapperConstants.INSTANCE_OF.equals(key);
    }

    @Override
    public void putMember(String key, Value value) {
        if (WrapperConstants.SELF.equals(key) || WrapperConstants.SUPER.equals(key) || WrapperConstants.EQUALS.equals(key) || WrapperConstants.INSTANCE_OF.equals(key)) {
            throw new UnsupportedOperationException("Cannot modify the " + key + " reference.");
        }

        if (javaInstanceProxy != null && javaInstanceProxy.hasMember(key)) {
            javaInstanceProxy.putMember(key, value);
        } else {
            properties.put(key, value);
        }
    }

    public Map<String, Object> getPropertiesForModification() {
        return this.properties;
    }

    @Override
    public String toString() {
        return String.format("[MQS Extended Instance: %s (extends %s)]",
                this.baseInstance.getClass().getName(),
                this.getOriginalConfig().extendsClass().yarnName());
    }
}
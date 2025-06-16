package net.me.scripting.extenders.proxies;

import net.me.scripting.config.ExtensionConfig;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Map;

public class ExtendedInstanceProxy implements ProxyObject {
    private final Map<String, Object> properties;
    private final Object baseInstance;
    private final Value originalOverrides;
    private final Value originalAddons;
    private final ExtensionConfig originalConfig;

    public ExtendedInstanceProxy(Map<String, Object> properties, Object baseInstance,ExtensionConfig originalConfig, Value originalOverrides, Value originalAddons) {
        this.properties = properties;
        this.baseInstance = baseInstance;
        this.originalConfig = originalConfig;
        this.originalOverrides = originalOverrides;
        this.originalAddons = originalAddons;
    }


    public Object getBaseInstance() {
        return baseInstance;
    }

    public Value getOriginalOverrides() {
        return originalOverrides;
    }

    public Value getOriginalAddons() {
        return originalAddons;
    }

    @Override
    public Object getMember(String key) {
        if (properties.containsKey(key)) {
            if ("_self".equals(key)) {
                return baseInstance;
            }
            return properties.get(key);
        }
        return null;
    }

    @Override
    public Object getMemberKeys() {
        return properties.keySet().toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        return properties.containsKey(key);
    }

    @Override
    public void putMember(String key, Value value) {
        if ("_self".equals(key)) {
            throw new UnsupportedOperationException("Cannot modify the _self reference.");
        }
        properties.put(key, value);
    }
    public ExtensionConfig getOriginalConfig() {
        return originalConfig;
    }

    public Map<String, Object> getPropertiesForModification() {
        return this.properties;
    }
}
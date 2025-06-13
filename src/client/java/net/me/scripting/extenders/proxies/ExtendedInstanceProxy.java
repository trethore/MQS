package net.me.scripting.extenders.proxies;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Map;

public class ExtendedInstanceProxy implements ProxyObject {
    private final Map<String, Object> properties;
    private final Object baseInstance;

    public ExtendedInstanceProxy(Map<String, Object> properties, Object baseInstance) {
        this.properties = properties;
        this.baseInstance = baseInstance;
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
        if (properties.containsKey(key)) {
            properties.put(key, value);
        } else {
            throw new UnsupportedOperationException("Cannot add new properties to this extended instance proxy.");
        }
    }
}
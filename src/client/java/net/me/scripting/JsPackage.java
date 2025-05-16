package net.me.scripting;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JsPackage implements ProxyObject {
    private final Map<String, Object> children = new HashMap<>();

    public void put(String name, Object obj) {
        if (!(obj instanceof JsPackage || obj instanceof LazyJsClassHolder || obj instanceof JsClassWrapper)) {
            throw new IllegalArgumentException("JsPackage can only hold JsPackage, LazyJsClassHolder, or JsClassWrapper instances.");
        }
        children.put(name, obj);
    }

    @Override
    public Object getMember(String key) {
        return children.get(key);
    }

    @Override
    public Object getMemberKeys() {
        Set<String> keys = children.keySet();
        return keys.toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        return children.containsKey(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot set members on a JsPackage.");
    }
}
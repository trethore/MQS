package net.me.scripting;

import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Map;

public class JsPackage implements ProxyObject {
    private final Map<String,ProxyObject> children = new HashMap<>();

    public void put(String name, ProxyObject obj) {
        children.put(name, obj);
    }

    @Override public Object getMember(String key) {
        return children.get(key);
    }
    @Override public Object getMemberKeys() {
        return children.keySet().toArray(new String[0]);
    }
    @Override public boolean hasMember(String key) {
        return children.containsKey(key);
    }
    @Override public void putMember(String key, org.graalvm.polyglot.Value value) { }
}
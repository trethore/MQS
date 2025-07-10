package net.me.scripting.extenders.proxies;

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
    private final Object baseInstance;
    private final Value originalOverrides;
    private final Value originalAddons;
    private final ExtensionConfig originalConfig;

    private MappedInstanceProxy javaInstanceProxy;

    public ExtendedInstanceProxy(Map<String, Object> properties, Object baseInstance, ExtensionConfig originalConfig, Value originalOverrides, Value originalAddons) {
        this.properties = properties;
        this.baseInstance = baseInstance;
        this.originalConfig = originalConfig;
        this.originalOverrides = originalOverrides;
        this.originalAddons = originalAddons;
    }

    public void setJavaInstanceProxy(MappedInstanceProxy javaInstanceProxy) {
        this.javaInstanceProxy = javaInstanceProxy;
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
        if ("equals".equals(key)) {
            return (ProxyExecutable) (Value... args) -> {
                if (args.length != 1) return false;
                Object otherRaw = ScriptUtils.unwrapReceiver(args[0]);
                return this.baseInstance.equals(otherRaw);
            };
        }

        if (properties.containsKey(key)) {
            if ("_self".equals(key)) {
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
        keys.add("equals");
        return keys.toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        return properties.containsKey(key)
                || (javaInstanceProxy != null && javaInstanceProxy.hasMember(key))
                || "equals".equals(key);
    }

    @Override
    public void putMember(String key, Value value) {
        if ("_self".equals(key) || "_super".equals(key) || "equals".equals(key)) {
            throw new UnsupportedOperationException("Cannot modify the " + key + " reference.");
        }

        if (javaInstanceProxy != null && javaInstanceProxy.hasMember(key)) {
            javaInstanceProxy.putMember(key, value);
        } else {
            properties.put(key, value);
        }
    }

    public ExtensionConfig getOriginalConfig() {
        return originalConfig;
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
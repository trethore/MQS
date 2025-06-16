package net.me.scripting.extenders.proxies;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Map;

public class RuntimeBinderProxy implements ProxyObject {
    private final Map<String, Object> originalOverrides;
    private ExtendedInstanceProxy bindingTarget;

    public RuntimeBinderProxy(Map<String, Object> originalOverrides) {
        this.originalOverrides = originalOverrides;
    }

    public void setBindingTarget(ExtendedInstanceProxy bindingTarget) {
        this.bindingTarget = bindingTarget;
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
package net.me.scripting.extenders.proxies;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import java.util.Collections;

public class SuperProxy implements ProxyObject {
    private final Value parentOverrides;
    private final Value grandParentSuper;
    private final Value childInstance;

    public SuperProxy(Value parentOverrides, Value grandParentSuper, Value childInstance) {
        this.parentOverrides = parentOverrides;
        this.grandParentSuper = grandParentSuper != null ? grandParentSuper : Value.asValue(Collections.emptyMap());
        this.childInstance = childInstance;
    }

    @Override
    public Object getMember(String key) {
        if (!parentOverrides.hasMember(key)) {
            return grandParentSuper.getMember(key);
        }

        Value parentFunction = parentOverrides.getMember(key);
        if (!parentFunction.canExecute()) {
            return parentFunction;
        }

        return (ProxyExecutable) args -> {
            ProxyObject temporaryThis = new ProxyObject() {
                @Override
                public Object getMember(String memberKey) {
                    if ("_super".equals(memberKey)) {
                        return grandParentSuper;
                    }
                    return childInstance.getMember(memberKey);
                }
                @Override public boolean hasMember(String memberKey) { return "_super".equals(memberKey) || childInstance.hasMember(memberKey); }
                @Override public void putMember(String memberKey, Value value) { childInstance.putMember(memberKey, value); }
                @Override public Object getMemberKeys() { return childInstance.getMemberKeys(); }
            };

            return parentFunction.invokeMember("apply", temporaryThis, args);
        };
    }

    @Override
    public Object getMemberKeys() {
        return parentOverrides.getMemberKeys();
    }

    @Override
    public boolean hasMember(String key) {
        return parentOverrides.hasMember(key) || grandParentSuper.hasMember(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify a _super object.");
    }
}
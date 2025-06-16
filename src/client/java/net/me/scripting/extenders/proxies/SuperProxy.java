package net.me.scripting.extenders.proxies;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
        if (parentOverrides != null && parentOverrides.hasMember(key)) {
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
                    @Override
                    public boolean hasMember(String memberKey) { return "_super".equals(memberKey) || childInstance.hasMember(memberKey); }
                    @Override
                    public void putMember(String memberKey, Value value) { childInstance.putMember(memberKey, value); }
                    @Override
                    public Object getMemberKeys() { return childInstance.getMemberKeys(); }
                };
                return parentFunction.invokeMember("apply", temporaryThis, args);
            };
        }
        if (grandParentSuper != null && grandParentSuper.hasMember(key)) {
            return grandParentSuper.getMember(key);
        }

        return null;
    }

    @Override
    public Object getMemberKeys() {
        Set<String> combinedKeys = new HashSet<>();
        if (this.parentOverrides != null && this.parentOverrides.hasMembers()) {
            combinedKeys.addAll(this.parentOverrides.getMemberKeys());
        }
        if (this.grandParentSuper != null && this.grandParentSuper.hasMembers()) {
            combinedKeys.addAll(this.grandParentSuper.getMemberKeys());
        }
        return combinedKeys.toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        if (parentOverrides != null && parentOverrides.hasMember(key)) {
            return true;
        }
        return grandParentSuper != null && grandParentSuper.hasMember(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify a _super object.");
    }
}
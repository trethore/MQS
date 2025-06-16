package net.me.scripting.extenders.proxies;

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

        List<String> runtimeNames = this.methodMappings.get(key);
        if (runtimeNames != null && !runtimeNames.isEmpty()) {
            String runtimeName = runtimeNames.getFirst();
            if (grandParentSuper.hasMember(runtimeName)) {
                return grandParentSuper.getMember(runtimeName);
            }
        }

        return grandParentSuper.getMember(key);
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
        if (parentOverrides.hasMember(key)) {
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
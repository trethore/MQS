package net.me.scripting.extenders.proxies;

import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.utils.MappingUtils;
import net.me.scripting.wrappers.JsObjectWrapper;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MappedInstanceProxy implements ProxyObject {
    private final Object extendedInstance;
    private final JsObjectWrapper originalWrapper;
    private final Value asValue;

    public MappedInstanceProxy(Object extendedInstance) {
        this.extendedInstance = extendedInstance;
        this.asValue = Value.asValue(extendedInstance);

        var cm = MappingUtils.combineMappings(
                extendedInstance.getClass(),
                MappingsManager.getInstance().getRuntimeToYarnClassMap(),
                MappingsManager.getInstance().getMethodMap(),
                MappingsManager.getInstance().getFieldMap()
        );
        this.originalWrapper = new JsObjectWrapper(extendedInstance, extendedInstance.getClass(), cm.methods(), cm.fields());
    }

    @Override
    public Object getMember(String key) {
        if ("_self".equals(key)) {
            return extendedInstance;
        }

        if (asValue.hasMember(key)) {
            return asValue.getMember(key);
        }

        return originalWrapper.getMember(key);
    }

    @Override
    public Object getMemberKeys() {
        Set<String> keys = new HashSet<>(asValue.getMemberKeys());
        Object originalKeysObj = originalWrapper.getMemberKeys();
        if (originalKeysObj instanceof String[] originalKeys) {
            keys.addAll(Arrays.asList(originalKeys));
        }
        keys.add("_self");
        return keys.toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        return "_self".equals(key) || asValue.hasMember(key) || originalWrapper.hasMember(key);
    }

    @Override
    public void putMember(String key, Value value) {
        if ("_self".equals(key)) {
            throw new UnsupportedOperationException("Cannot modify the _self reference.");
        }

        if (asValue.hasMember(key)) {
            asValue.putMember(key, value);
            return;
        }

        if (originalWrapper.hasMember(key)) {
            originalWrapper.putMember(key, value);
            return;
        }

        throw new UnsupportedOperationException("Cannot add or modify member: " + key);
    }

    public Object getInstance() {
        return extendedInstance;
    }
}
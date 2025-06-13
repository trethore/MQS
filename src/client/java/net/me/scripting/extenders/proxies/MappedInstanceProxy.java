package net.me.scripting.extenders.proxies;

import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.utils.MappingUtils;
import net.me.scripting.wrappers.JsObjectWrapper;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MappedInstanceProxy implements ProxyObject {
    private final Object extendedInstance;
    private final JsObjectWrapper originalWrapper;

    public MappedInstanceProxy(Object extendedInstance) {
        this.extendedInstance = extendedInstance;

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
        return originalWrapper.getMember(key);
    }

    @Override
    public Object getMemberKeys() {
        Object originalKeysObj = originalWrapper.getMemberKeys();
        if (originalKeysObj instanceof String[] originalKeys) {
            List<String> keys = new ArrayList<>(Arrays.asList(originalKeys));
            if (!keys.contains("_self")) {
                keys.add("_self");
            }
            return keys.toArray(new String[0]);
        }
        return new String[]{"_self"};
    }

    @Override
    public boolean hasMember(String key) {
        return "_self".equals(key) || originalWrapper.hasMember(key);
    }

    @Override
    public void putMember(String key, Value value) {
        if ("_self".equals(key)) {
            throw new UnsupportedOperationException("Cannot modify the _self reference.");
        }
        if (originalWrapper.hasMember(key)) {
            originalWrapper.putMember(key, value);
        } else {
            throw new UnsupportedOperationException("Cannot add or modify member: " + key);
        }
    }

    public Object getInstance() {
        return extendedInstance;
    }
}
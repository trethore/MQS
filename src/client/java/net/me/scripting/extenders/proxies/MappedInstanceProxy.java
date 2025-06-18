package net.me.scripting.extenders.proxies;

import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.utils.MappingUtils;
import net.me.scripting.wrappers.JsObjectWrapper;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public class MappedInstanceProxy implements ProxyObject {
    private final Object extendedInstance;
    private final JsObjectWrapper methodAndFieldWrapper;

    public MappedInstanceProxy(Object extendedInstance) {
        this.extendedInstance = extendedInstance;

        var cm = MappingUtils.combineMappings(
                extendedInstance.getClass(),
                MappingsManager.getInstance().getRuntimeToYarnClassMap(),
                MappingsManager.getInstance().getMethodMap(),
                MappingsManager.getInstance().getFieldMap()
        );
        this.methodAndFieldWrapper = new JsObjectWrapper(extendedInstance, extendedInstance.getClass(), cm.methods(), cm.fields());
    }

    @Override
    public Object getMember(String key) {
        if ("_self".equals(key)) {
            return extendedInstance;
        }
        return methodAndFieldWrapper.getMember(key);
    }

    @Override
    public Object getMemberKeys() {
        return methodAndFieldWrapper.getMemberKeys();
    }

    @Override
    public boolean hasMember(String key) {
        return methodAndFieldWrapper.hasMember(key);
    }

    @Override
    public void putMember(String key, Value value) {
        methodAndFieldWrapper.putMember(key, value);
    }

    public Object getInstance() {
        return extendedInstance;
    }
}
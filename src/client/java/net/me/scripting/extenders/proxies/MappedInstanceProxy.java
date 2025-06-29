package net.me.scripting.extenders.proxies;

import net.me.scripting.utils.MappingUtils;
import net.me.scripting.wrappers.JsObjectWrapper;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.List;
import java.util.Map;

public class MappedInstanceProxy implements ProxyObject {
    private final Object extendedInstance;
    private final JsObjectWrapper methodAndFieldWrapper;

    public MappedInstanceProxy(Object extendedInstance, Map<String, String> runtimeToYarnClassMap,
                               Map<String, Map<String, List<String>>> methodMap,
                               Map<String, Map<String, String>> fieldMap) {
        this.extendedInstance = extendedInstance;

        var cm = MappingUtils.combineMappings(
                extendedInstance.getClass(),
                runtimeToYarnClassMap,
                methodMap,
                fieldMap
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
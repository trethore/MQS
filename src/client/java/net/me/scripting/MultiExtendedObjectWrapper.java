package net.me.scripting;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.*;

public class MultiExtendedObjectWrapper implements ProxyObject {
    private final List<SuperAccessWrapper> superList;
    private final Value jsOverrides;
    private final Object javaInstance;

    public MultiExtendedObjectWrapper(Object javaInstance,
                                      List<ScriptUtils.ClassMappings> mappingsList,
                                      Value overrides) {
        this.javaInstance = javaInstance;
        this.jsOverrides  = overrides;
        this.superList    = new ArrayList<>();

        Class<?> actualClass = javaInstance.getClass();
        for (var cm : mappingsList) {
            superList.add(new SuperAccessWrapper(
                    javaInstance,
                    actualClass,
                    cm.methods(),
                    cm.fields()
            ));
        }
    }

    @Override
    public Object getMember(String key) {
        if ("_self".equals(key)) {
            return this.javaInstance;
        }
        if ("_super".equals(key)) {
            return superList.toArray(new SuperAccessWrapper[0]);
        }

        if (jsOverrides != null && jsOverrides.hasMember(key)) {
            Value fn = jsOverrides.getMember(key);
            if (fn.canExecute()) {
                return (ProxyExecutable) args -> fn.invokeMember("call", Value.asValue(this), args);
            }
            return ScriptUtils.wrapReturn(fn);
        }

        for (SuperAccessWrapper sup : superList) {
            if (sup.hasMember(key)) {
                return sup.getMember(key);
            }
        }

        return null;
    }

    @Override
    public boolean hasMember(String key) {
        if ("_self".equals(key)) {
            return true;
        }
        if ("_super".equals(key)) return true;
        if (jsOverrides != null && jsOverrides.hasMember(key)) return true;
        return superList.stream().anyMatch(sup -> sup.hasMember(key));
    }

    @Override
    public Object getMemberKeys() {
        Set<String> keys = new LinkedHashSet<>();
        keys.add("_self");
        keys.add("_super");
        if (jsOverrides != null) {
            keys.addAll(jsOverrides.getMemberKeys());
        }
        for (SuperAccessWrapper sup : superList) {
            Collections.addAll(keys, (String[]) sup.getMemberKeys());
        }
        return keys.toArray(new String[0]);
    }

    @Override
    public void putMember(String key, Value value) {
        if ("_super".equals(key)) {
            throw new UnsupportedOperationException("Cannot overwrite _super");
        }
        if (jsOverrides != null && jsOverrides.hasMember(key)) {
            jsOverrides.putMember(key, value);
            return;
        }
        for (SuperAccessWrapper sup : superList) {
            if (sup.hasMember(key)) {
                sup.putMember(key, value);
                return;
            }
        }
        throw new UnsupportedOperationException("No writable member: " + key);
    }
}

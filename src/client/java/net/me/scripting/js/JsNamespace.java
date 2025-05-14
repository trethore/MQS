package net.me.scripting.js;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import net.me.scripting.MappingData;
import net.me.scripting.ScriptManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JsNamespace implements ProxyObject {
    private final String fqdnPrefix; // e.g., "net.minecraft.util.math." or "" for root
    private final Map<String, Object> members = new HashMap<>(); // String -> JsNamespace or JsClassProxy
    private final MappingData mappingData;
    private final ScriptManager scriptManager;

    public JsNamespace(String fqdnPrefix, MappingData mappingData, ScriptManager scriptManager) {
        this.fqdnPrefix = fqdnPrefix;
        this.mappingData = mappingData;
        this.scriptManager = scriptManager;
    }

    @Override
    public Object getMember(String key) {
        if (members.containsKey(key)) {
            return members.get(key);
        }

        String potentialClassYarnName = fqdnPrefix + key;
        if (mappingData.getOfficialClassName(potentialClassYarnName).isPresent()) {
            JsClassProxy classProxy = new JsClassProxy(potentialClassYarnName, mappingData, scriptManager);
            members.put(key, classProxy);
            return classProxy;
        }

        // Assume it's a deeper package
        JsNamespace nextPackage = new JsNamespace(potentialClassYarnName + ".", mappingData, scriptManager);
        members.put(key, nextPackage);
        return nextPackage;
    }

    @Override
    public Object getMemberKeys() {
        // This is tricky - you don't want to list ALL possible sub-packages/classes.
        // Could return currently resolved members or leave empty.
        return members.keySet().toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        // Optimistically say yes, getMember will try to resolve it.
        return true;
    }

    @Override
    public void putMember(String key, Value value) {
        // Namespaces are generally read-only from JS side
        throw new UnsupportedOperationException("Cannot add members to namespace proxy");
    }
}
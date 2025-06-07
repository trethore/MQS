package net.me.scripting;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyInstantiable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappedClassExtender implements ProxyObject, ProxyInstantiable {
    private final Value baseAdapterConstructor; 
    private final JsClassWrapper wrapper;       

    public MappedClassExtender(Value baseAdapterConstructor, JsClassWrapper wrapper) {
        this.baseAdapterConstructor = baseAdapterConstructor;
        this.wrapper = wrapper;
    }

    @Override
    public Object newInstance(Value... args) {
        if (args.length == 0) { 
            throw new RuntimeException("Cannot extend with mapped names without an overrides object. " +
                    "If no constructor arguments, pass at least an empty overrides object {}.");
        }

        Value lastArg = args[args.length - 1];
        if (!lastArg.isProxyObject() && !lastArg.hasMembers()) { 
            
            if (args.length > 0 && !lastArg.hasMembers()) { 
                return baseAdapterConstructor.newInstance(args);
            }
            throw new RuntimeException("The last argument to the adapter constructor must be an overrides object (e.g., {}).");
        }

        Map<String, Object> runtimeOverrides = new HashMap<>();
        
        if (lastArg.hasMembers()) {
            for (String yarnKey : lastArg.getMemberKeys()) {
                Object jsFunc = lastArg.getMember(yarnKey);
                
                List<String> mappedRuntimeNames = wrapper.getMethodMappings().get(yarnKey);

                if (mappedRuntimeNames != null && !mappedRuntimeNames.isEmpty()) {
                    for (String runtimeName : mappedRuntimeNames) {
                        runtimeOverrides.put(runtimeName, jsFunc);
                    }
                } else {
                    
                    runtimeOverrides.put(yarnKey, jsFunc);
                }
            }
        }


        ProxyObject overridesProxy = ProxyObject.fromMap(runtimeOverrides);

        Object[] ctorArgs;
        if (lastArg.hasMembers()) { 
            ctorArgs = new Object[args.length];
            for (int i = 0; i < args.length - 1; i++) {
                ctorArgs[i] = args[i];
            }
            ctorArgs[args.length - 1] = overridesProxy;
        } else {
            ctorArgs = new Object[args.length + 1];
            for (int i = 0; i < args.length; i++) {
                ctorArgs[i] = args[i];
            }
            ctorArgs[args.length] = overridesProxy;
        }


        return baseAdapterConstructor.newInstance(ctorArgs);
    }

    @Override
    public Object getMember(String key) {
        if ("prototype".equals(key)) {
            return baseAdapterConstructor.getMember("prototype");
        }
        return null;
    }

    @Override
    public Object getMemberKeys() {
        return new String[]{"prototype"};
    }

    @Override
    public boolean hasMember(String key) {
        return "prototype".equals(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot set members on MappedClassExtender function object.");
    }
}
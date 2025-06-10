package net.me.scripting;

import org.graalvm.polyglot.Context;
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
        validateArguments(args);

        ArgumentParser parser = parseArguments(args);
        Map<String, Object> runtimeOverrides = buildRuntimeOverrides(parser.overridesValue);

        if (parser.addonsValue == null) {
            return createSimpleInstance(parser.constructorArgs, runtimeOverrides);
        }

        return createWrappedInstance(parser.constructorArgs, runtimeOverrides, parser.addonsValue);
    }

    private void validateArguments(Value[] args) {
        if (args.length == 0) {
            throw new RuntimeException("Cannot extend with mapped names without configuration object. " +
                    "Pass at least { overrides: {} } or { overrides: {}, addons: {} }.");
        }
    }

    private ArgumentParser parseArguments(Value[] args) {
        Value lastArg = args[args.length - 1];

        if (!isObjectLike(lastArg)) {
            throw new RuntimeException(
                    "The last argument must be a configuration object with 'overrides' and optionally 'addons' keys.");
        }

        Object[] constructorArgs = new Object[args.length - 1];
        System.arraycopy(args, 0, constructorArgs, 0, args.length - 1);

        Value overridesValue = lastArg.hasMember("overrides") ? lastArg.getMember("overrides") : null;
        Value addonsValue = lastArg.hasMember("addons") ? lastArg.getMember("addons") : null;

        if (overridesValue == null) {
            throw new RuntimeException(
                    "Configuration object must contain an 'overrides' key, even if empty: { overrides: {} }");
        }

        return new ArgumentParser(overridesValue, addonsValue, constructorArgs);
    }

    private boolean isObjectLike(Value value) {
        return value != null && (value.hasMembers() || value.isProxyObject());
    }

    private Map<String, Object> buildRuntimeOverrides(Value overridesArg) {
        Map<String, Object> runtimeOverrides = new HashMap<>();

        if (overridesArg == null || !overridesArg.hasMembers()) {
            return runtimeOverrides;
        }

        for (String jsMethodName : overridesArg.getMemberKeys()) {
            Object jsFunction = overridesArg.getMember(jsMethodName);
            addMethodOverrides(runtimeOverrides, jsMethodName, jsFunction);
        }

        return runtimeOverrides;
    }

    private void addMethodOverrides(Map<String, Object> overrides, String jsMethodName, Object jsFunction) {
        List<String> mappedNames = wrapper.getMethodMappings().get(jsMethodName);

        if (mappedNames != null && !mappedNames.isEmpty()) {
            for (String runtimeName : mappedNames) {
                overrides.put(runtimeName, jsFunction);
            }
        } else {
            overrides.put(jsMethodName, jsFunction);
        }
    }

    private Object createSimpleInstance(Object[] constructorArgs, Map<String, Object> runtimeOverrides) {
        ProxyObject overridesProxy = ProxyObject.fromMap(runtimeOverrides);
        Object[] ctorArgs = appendToArray(constructorArgs, overridesProxy);
        return baseAdapterConstructor.newInstance(ctorArgs);
    }

    private Object createWrappedInstance(Object[] constructorArgs, Map<String, Object> runtimeOverrides, Value addonsArg) {
        ProxyObject overridesProxy = ProxyObject.fromMap(runtimeOverrides);
        Object[] ctorArgs = appendToArray(constructorArgs, overridesProxy);
        Object baseInstance = baseAdapterConstructor.newInstance(ctorArgs);

        Map<String, Object> wrapperProperties = buildWrapperProperties(addonsArg, baseInstance);
        ProxyObject wrapper = new CustomProxyWrapper(wrapperProperties);

        bindFunctionsToWrapper(wrapperProperties, wrapper);

        return wrapper;
    }

    private Map<String, Object> buildWrapperProperties(Value addonsArg, Object baseInstance) {
        Map<String, Object> wrapperProperties = new HashMap<>();
        wrapperProperties.put("instance", baseInstance);

        if (addonsArg != null && addonsArg.hasMembers()) {
            for (String addonKey : addonsArg.getMemberKeys()) {
                wrapperProperties.put(addonKey, addonsArg.getMember(addonKey));
            }
        }

        return wrapperProperties;
    }

    private void bindFunctionsToWrapper(Map<String, Object> wrapperProperties, ProxyObject wrapper) {
        Context ctx = Context.getCurrent();
        Value wrapperVal = ctx.asValue(wrapper);

        for (Map.Entry<String, Object> entry : wrapperProperties.entrySet()) {
            Object value = entry.getValue();
            if (isExecutableValue(value)) {
                Value bound = ((Value) value).invokeMember("bind", wrapperVal);
                entry.setValue(bound);
            }
        }
    }

    private boolean isExecutableValue(Object value) {
        return value instanceof Value && ((Value) value).canExecute();
    }

    private Object[] appendToArray(Object[] original, Object newElement) {
        Object[] result = new Object[original.length + 1];
        System.arraycopy(original, 0, result, 0, original.length);
        result[original.length] = newElement;
        return result;
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
        throw new UnsupportedOperationException(
                "Cannot set members on MappedClassExtender function object.");
    }

    private record ArgumentParser(Value overridesValue, Value addonsValue, Object[] constructorArgs) {}

    public record CustomProxyWrapper(Map<String, Object> properties) implements ProxyObject {
        @Override
        public Object getMember(String key) {
            return properties.get(key);
        }

        @Override
        public Object getMemberKeys() {
            return properties.keySet().toArray(new String[0]);
        }

        @Override
        public boolean hasMember(String key) {
            return properties.containsKey(key);
        }

        @Override
        public void putMember(String key, Value value) {
            properties.put(key, value);
        }
    }
}
package net.me.scripting;

import net.me.scripting.mappings.MappingsManager;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyInstantiable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlexibleMappedClassExtender implements ProxyObject, ProxyInstantiable {
    private final ScriptManager.ExtensionConfig config;
    private final Context context;
    private final Value baseAdapterConstructor;

    public FlexibleMappedClassExtender(ScriptManager.ExtensionConfig config, Context context) {
        this.config = config;
        this.context = context;
        this.baseAdapterConstructor = createBaseAdapter();
    }

    private Value createBaseAdapter() {
        Value javaObj = context.eval("js", "Java");
        Value extendFn = javaObj.getMember("extend");

        Class<?> extendsClass = config.extendsClass().getTargetClass();
        Value hostClassValue = context.asValue(extendsClass);

        if (!config.implementsClasses().isEmpty()) {
            List<Class<?>> interfaceClasses = new ArrayList<>();
            for (JsClassWrapper wrapper : config.implementsClasses()) {
                interfaceClasses.add(wrapper.getTargetClass());
            }

            Value[] extendArgs = new Value[1 + interfaceClasses.size()];
            extendArgs[0] = hostClassValue;

            for (int i = 0; i < interfaceClasses.size(); i++) {
                extendArgs[i + 1] = context.asValue(interfaceClasses.get(i));
            }

            return extendFn.execute(extendArgs);
        } else {
            return extendFn.execute(hostClassValue);
        }
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
        List<String> mappedNames = config.extendsClass().getMethodMappings().get(jsMethodName);

        if (mappedNames != null && !mappedNames.isEmpty()) {
            for (String runtimeName : mappedNames) {
                overrides.put(runtimeName, jsFunction);
            }
        } else {
            boolean foundInInterface = false;
            for (JsClassWrapper interfaceWrapper : config.implementsClasses()) {
                List<String> interfaceMappings = interfaceWrapper.getMethodMappings().get(jsMethodName);
                if (interfaceMappings != null && !interfaceMappings.isEmpty()) {
                    for (String runtimeName : interfaceMappings) {
                        overrides.put(runtimeName, jsFunction);
                    }
                    foundInInterface = true;
                    break;
                }
            }

            if (!foundInInterface) {
                overrides.put(jsMethodName, jsFunction);
            }
        }
    }

    private Object createSimpleInstance(Object[] constructorArgs, Map<String, Object> runtimeOverrides) {
        ProxyObject overridesProxy = ProxyObject.fromMap(runtimeOverrides);
        Object[] ctorArgs = appendToArray(constructorArgs, overridesProxy);
        return baseAdapterConstructor.newInstance(ctorArgs);
    }

    private Object createWrappedInstance(Object[] constructorArgs,
                                         Map<String, Object> runtimeOverrides,
                                         Value addonsArg) {
        ProxyObject overridesProxy = ProxyObject.fromMap(runtimeOverrides);
        Object[] ctorArgs = appendToArray(constructorArgs, overridesProxy);
        Object baseInstance = baseAdapterConstructor.newInstance(ctorArgs);

        Map<String, Object> wrapperProperties = new HashMap<>();
        wrapperProperties.put("instance", baseInstance);

        if (addonsArg != null && addonsArg.hasMembers()) {
            for (String addonKey : addonsArg.getMemberKeys()) {
                wrapperProperties.put(addonKey, addonsArg.getMember(addonKey));
            }
        }

        ProxyObject wrapper = new CustomProxyWrapper(wrapperProperties, config, context, baseInstance);
        bindFunctionsToWrapper(wrapperProperties, wrapper);
        return wrapper;
    }

    private void bindFunctionsToWrapper(Map<String, Object> wrapperProperties, ProxyObject wrapper) {
        Value wrapperVal = context.asValue(wrapper);

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
                "Cannot set members on FlexibleMappedClassExtender function object.");
    }

    private record ArgumentParser(Value overridesValue, Value addonsValue, Object[] constructorArgs) {}

    public static class CustomProxyWrapper implements ProxyObject {
        private final Map<String, Object> properties;
        private final Context context;
        private final Object baseInstance;

        public CustomProxyWrapper(Map<String, Object> properties, ScriptManager.ExtensionConfig config, Context context, Object baseInstance) {
            this.properties = properties;
            this.context = context;
            this.baseInstance = baseInstance;
        }

        @Override
        public Object getMember(String key) {
            if (properties.containsKey(key)) {
                if ("instance".equals(key)) {
                    return new ExtendedInstanceWrapper(baseInstance, context);
                }
                return properties.get(key);
            }
            return null;
        }

        @Override
        public Object getMemberKeys() {
            List<String> keys = new ArrayList<>(properties.keySet());
            return keys.toArray(new String[0]);
        }

        @Override
        public boolean hasMember(String key) {
            return properties.containsKey(key);
        }

        @Override
        public void putMember(String key, Value value) {
            if (properties.containsKey(key)) {
                properties.put(key, value);
            } else {
                throw new UnsupportedOperationException("Cannot add new properties to wrapper");
            }
        }
    }

    public static class ExtendedInstanceWrapper implements ProxyObject {
        private final Object extendedInstance;
        private final Value extendedInstanceValue;
        private final JsObjectWrapper originalWrapper;

        public ExtendedInstanceWrapper(Object extendedInstance, Context context) {
            this.extendedInstance = extendedInstance;
            this.extendedInstanceValue = context.asValue(extendedInstance);

            var cm = ScriptUtils.combineMappings(
                    extendedInstance.getClass().getSuperclass(),
                    MappingsManager.getInstance().getRuntimeToYarnClassMap(),
                    MappingsManager.getInstance().getMethodMap(),
                    MappingsManager.getInstance().getFieldMap()
            );
            this.originalWrapper = new JsObjectWrapper(extendedInstance, extendedInstance.getClass(), cm.methods(), cm.fields());
        }

        @Override
        public Object getMember(String key) {
            if (extendedInstanceValue.hasMember(key)) {
                Object member = extendedInstanceValue.getMember(key);
                if (member instanceof Value v && v.canExecute()) {
                    return createExecutableProxy(key, member);
                }
                return member;
            }

            if (originalWrapper.hasMember(key)) {
                return originalWrapper.getMember(key);
            }

            return null;
        }

        private ProxyExecutable createExecutableProxy(String methodName, Object member) {
            return (Value... args) -> {
                if (member instanceof Value && ((Value) member).canExecute()) {
                    Object[] javaArgs = ScriptUtils.unwrapArgs(args, null);
                    return ((Value) member).execute(javaArgs);
                }
                throw new RuntimeException("Method " + methodName + " is not executable");
            };
        }

        @Override
        public Object getMemberKeys() {

            return extendedInstanceValue.getMemberKeys().toArray();
        }

        @Override
        public boolean hasMember(String key) {
            return extendedInstanceValue.hasMember(key) || originalWrapper.hasMember(key);
        }

        @Override
        public void putMember(String key, Value value) {
            if (originalWrapper.hasMember(key)) {
                originalWrapper.putMember(key, value);
            } else if (extendedInstanceValue.hasMember(key)) {
                extendedInstanceValue.putMember(key, value);
            } else {
                throw new UnsupportedOperationException("No writable member: " + key);
            }
        }

        public Object getInstance() {
            return extendedInstance;
        }
    }
}
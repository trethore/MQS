package net.me.scripting;

import net.me.scripting.mappings.MappingsManager;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyInstantiable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // Get the main class to extend
        Class<?> extendsClass = config.extendsClass().getTargetClass();
        Value hostClassValue = context.asValue(extendsClass);

        // If there are interfaces to implement, we need to handle them
        if (!config.implementsClasses().isEmpty()) {
            // Convert interface wrappers to actual classes
            List<Class<?>> interfaceClasses = new ArrayList<>();
            for (JsClassWrapper wrapper : config.implementsClasses()) {
                interfaceClasses.add(wrapper.getTargetClass());
            }

            // Create array of classes for Java.extend(Class, Interface1, Interface2, ...)
            Value[] extendArgs = new Value[1 + interfaceClasses.size()];
            extendArgs[0] = hostClassValue;

            for (int i = 0; i < interfaceClasses.size(); i++) {
                extendArgs[i + 1] = context.asValue(interfaceClasses.get(i));
            }

            return extendFn.execute(extendArgs);
        } else {
            // Simple case: just extend the main class
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
        // Check extends class mappings first
        List<String> mappedNames = config.extendsClass().getMethodMappings().get(jsMethodName);

        if (mappedNames != null && !mappedNames.isEmpty()) {
            for (String runtimeName : mappedNames) {
                overrides.put(runtimeName, jsFunction);
            }
        } else {
            // Check interface mappings
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
                // Fallback to original name
                overrides.put(jsMethodName, jsFunction);
            }
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
        wrapperProperties.putAll(runtimeOverrides);
        ProxyObject wrapper = new CustomProxyWrapper(wrapperProperties, config, context);

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

    /**
     * The custom proxy that wraps the extended Java instance and adds JS 'addon' properties.
     */
    public static class CustomProxyWrapper implements ProxyObject {
        private final Map<String, Object> properties;
        private final Value instanceValue;
        private final JsObjectWrapper instanceWrapper;

        public CustomProxyWrapper(Map<String, Object> properties, ScriptManager.ExtensionConfig config, Context context) {
            this.properties = properties;
            Object instance = properties.get("instance");
            this.instanceValue = context.asValue(instance);

            // Create a full JsObjectWrapper for the instance to handle mapped method/field access
            var cm = ScriptUtils.combineMappings(
                    instance.getClass(),
                    MappingsManager.getInstance().getRuntimeToYarnClassMap(),
                    MappingsManager.getInstance().getMethodMap(),
                    MappingsManager.getInstance().getFieldMap()
            );
            this.instanceWrapper = new JsObjectWrapper(instance, instance.getClass(), cm.methods(), cm.fields());
        }

        public Object getInstance() {
            return properties.get("instance");
        }

        @Override
        public Object getMember(String key) {
            System.out.println("getMember: " + key);

            // 1. Check for addon properties first.
            if (properties.containsKey(key)) {
                return properties.get(key);
            }

            // 2. Delegate to the instance wrapper to handle mapped methods/fields.
            if (instanceWrapper.hasMember(key)) {
                Object member = instanceWrapper.getMember(key);
                // If it's a method, wrap it in a ProxyExecutable to make it callable
                if (member instanceof Value && ((Value) member).canExecute()) {
                    return createExecutableProxy(key, member);
                }
                return member;
            }

            // 3. Fallback to direct access on the Value
            if (instanceValue.hasMember(key)) {
                Object member = instanceValue.getMember(key);
                // If it's a method, wrap it in a ProxyExecutable to make it callable
                if (member instanceof Value && ((Value) member).canExecute()) {
                    return createExecutableProxy(key, member);
                }
                return member;
            }

            return null;
        }

        private ProxyExecutable createExecutableProxy(String methodName, Object member) {
            return (Value... args) -> {
                System.out.println("Executing method: " + methodName);
                if (member instanceof Value && ((Value) member).canExecute()) {
                    return ((Value) member).execute((Object[]) args);
                }
                throw new RuntimeException("Method " + methodName + " is not executable");
            };
        }

        @Override
        public Object getMemberKeys() {
            // Combine keys from addons and the underlying instance.
            List<String> keys = new ArrayList<>(properties.keySet());
            Object[] instanceKeys = (Object[]) instanceWrapper.getMemberKeys();
            for (Object key : instanceKeys) {
                if (!keys.contains(key)) {
                    keys.add((String) key);
                }
            }
            System.out.println("getMemberKeys: " + keys);
            return keys.toArray(new String[0]);
        }

        @Override
        public boolean hasMember(String key) {
            System.out.println("hasMember: " + key);
            return properties.containsKey(key) || instanceWrapper.hasMember(key) || instanceValue.hasMember(key);
        }

        @Override
        public void putMember(String key, Value value) {
            // Allow setting addon properties, otherwise delegate to the instance wrapper.
            if (properties.containsKey(key)) {
                properties.put(key, value);
            } else {
                instanceWrapper.putMember(key, value);
            }
        }
    }
}
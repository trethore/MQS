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
        Value extendFn = context.eval("js", "Java.extend");
        Value typeFn = context.eval("js", "Java.type");

        List<Value> extendArgs = new ArrayList<>();

        Class<?> extendsClass = config.extendsClass().targetClass();
        extendArgs.add(typeFn.execute(extendsClass.getName()));

        for (ScriptManager.MappedClassInfo interfaceInfo : config.implementsClasses()) {
            Class<?> interfaceClass = interfaceInfo.targetClass();
            extendArgs.add(typeFn.execute(interfaceClass.getName()));
        }

        return extendFn.execute((Object[]) extendArgs.toArray(new Value[0]));
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

    private record ArgumentParser(Value overridesValue, Value addonsValue, Value[] constructorArgs) {}

    private ArgumentParser parseArguments(Value[] args) {
        Value lastArg = args[args.length - 1];

        if (!isObjectLike(lastArg)) {
            throw new RuntimeException(
                    "The last argument must be a configuration object with 'overrides' and optionally 'addons' keys.");
        }

        Value[] constructorArgs = new Value[args.length - 1];
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
        List<String> mappedNames = config.extendsClass().methodMappings().get(jsMethodName);
        if (mappedNames != null && !mappedNames.isEmpty()) {
            for (String runtimeName : mappedNames) {
                overrides.put(runtimeName, jsFunction);
            }
            return;
        }

        for (ScriptManager.MappedClassInfo interfaceInfo : config.implementsClasses()) {
            List<String> interfaceMappings = interfaceInfo.methodMappings().get(jsMethodName);
            if (interfaceMappings != null && !interfaceMappings.isEmpty()) {
                for (String runtimeName : interfaceMappings) {
                    overrides.put(runtimeName, jsFunction);
                }
                return;
            }
        }

        overrides.put(jsMethodName, jsFunction);
    }

    private Object createSimpleInstance(Value[] constructorArgs, Map<String, Object> runtimeOverrides) {
        try {
            // Convert Value[] to Object[] properly
            Object[] javaCtorArgs = new Object[constructorArgs.length];
            for (int i = 0; i < constructorArgs.length; i++) {
                javaCtorArgs[i] = convertValueToJavaObject(constructorArgs[i]);
            }

            ProxyObject overridesProxy = ProxyObject.fromMap(runtimeOverrides);
            Object[] finalCtorArgs = appendToArray(javaCtorArgs, overridesProxy);

            return baseAdapterConstructor.newInstance(finalCtorArgs);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance: " + e.getMessage(), e);
        }
    }

    private Object createWrappedInstance(Value[] constructorArgs,
                                         Map<String, Object> runtimeOverrides,
                                         Value addonsArg) {
        try {
            // Convert Value[] to Object[] properly
            Object[] javaCtorArgs = new Object[constructorArgs.length];
            for (int i = 0; i < constructorArgs.length; i++) {
                javaCtorArgs[i] = convertValueToJavaObject(constructorArgs[i]);
            }

            ProxyObject overridesProxy = ProxyObject.fromMap(runtimeOverrides);
            Object[] finalCtorArgs = appendToArray(javaCtorArgs, overridesProxy);

            Object baseInstance = baseAdapterConstructor.newInstance(finalCtorArgs);

            Map<String, Object> wrapperProperties = new HashMap<>();

            // SOLUTION 2: instance = wrapper avec toutes les méthodes héritées mappées
            wrapperProperties.put("instance", new ExtendedInstanceWrapper(baseInstance, context));

            // SOLUTION 2: _self = objet Java direct pour setScreen()
            wrapperProperties.put("_self", baseInstance);

            if (addonsArg != null && addonsArg.hasMembers()) {
                for (String addonKey : addonsArg.getMemberKeys()) {
                    wrapperProperties.put(addonKey, addonsArg.getMember(addonKey));
                }
            }

            ProxyObject wrapper = new CustomProxyWrapper(wrapperProperties, baseInstance);
            bindFunctionsToWrapper(wrapperProperties, wrapper);
            return wrapper;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create wrapped instance: " + e.getMessage(), e);
        }
    }

    private Object convertValueToJavaObject(Value value) {
        if (value == null) {
            return null;
        }

        // If the value is already a Java object, return as-is
        if (value.isHostObject()) {
            return value.asHostObject();
        }

        // Handle primitive types
        if (value.isString()) {
            return value.asString();
        }
        if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            } else if (value.fitsInLong()) {
                return value.asLong();
            } else if (value.fitsInFloat()) {
                return value.asFloat();
            } else {
                return value.asDouble();
            }
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }

        // For complex objects, return the Value itself
        return value;
    }

    private void bindFunctionsToWrapper(Map<String, Object> wrapperProperties, ProxyObject wrapper) {
        Value wrapperVal = context.asValue(wrapper);

        for (Map.Entry<String, Object> entry : wrapperProperties.entrySet()) {
            Object value = entry.getValue();
            if (isJavaScriptFunction(value)) {
                try {
                    Value bound = ((Value) value).invokeMember("bind", wrapperVal);
                    entry.setValue(bound);
                } catch (Exception e) {
                    // If binding fails, keep the original function
                    System.err.println("Warning: Could not bind function " + entry.getKey() + ": " + e.getMessage());
                }
            }
        }
    }

    private boolean isJavaScriptFunction(Object value) {
        if (!(value instanceof Value val)) {
            return false;
        }
        // Check if it's executable and has the bind method (JavaScript function)
        return val.canExecute() && val.hasMember("bind");
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

    public static class CustomProxyWrapper implements ProxyObject {
        private final Map<String, Object> properties;
        private final Object baseInstance;

        public CustomProxyWrapper(Map<String, Object> properties, Object baseInstance) {
            this.properties = properties;
            this.baseInstance = baseInstance;
        }

        @Override
        public Object getMember(String key) {
            if (properties.containsKey(key)) {
                if ("instance".equals(key)) {
                    // Retourne le wrapper avec toutes les méthodes héritées mappées
                    return properties.get(key);
                }
                if ("_self".equals(key)) {
                    // Retourne l'objet Java direct pour setScreen()
                    return baseInstance;
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
            if (properties.containsKey(key) && !"_self".equals(key)) {
                // On ne peut pas modifier _self car c'est l'instance Java directe
                properties.put(key, value);
            } else if ("_self".equals(key)) {
                throw new UnsupportedOperationException("Cannot modify _self reference");
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
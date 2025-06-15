package net.me.scripting.extenders;

import net.me.scripting.config.ExtensionConfig;
import net.me.scripting.config.MappedClassInfo;
import net.me.scripting.extenders.proxies.ExtendedInstanceProxy;
import net.me.scripting.extenders.proxies.MappedInstanceProxy;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyInstantiable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappedClassExtender implements ProxyObject, ProxyInstantiable {
    private final ExtensionConfig config;
    private final Context context;
    private final Value baseAdapterConstructor;
    private final Value parentOverrides;
    private final Value parentAddons;


    public MappedClassExtender(ExtensionConfig config, Context context, Value parentOverrides, Value parentAddons) {
        this.config = config;
        this.context = context;
        this.parentOverrides = parentOverrides;
        this.parentAddons = parentAddons;
        this.baseAdapterConstructor = createBaseAdapter();
    }

    private Value createBaseAdapter() {
        Value extendFn = context.eval("js", "Java.extend");
        List<Object> extendArgs = new ArrayList<>();
        Class<?> extendsClass = config.extendsClass().targetClass();
        extendArgs.add(extendsClass);
        Value typeFn = context.eval("js", "Java.type");
        for (MappedClassInfo interfaceInfo : config.implementsClasses()) {
            Class<?> interfaceClass = interfaceInfo.targetClass();
            extendArgs.add(typeFn.execute(interfaceClass.getName()));
        }
        return extendFn.execute(extendArgs.toArray());
    }

    @Override
    public Object newInstance(Value... args) {
        validateArguments(args);
        ArgumentParser parser = parseArguments(args);
        Value finalOverrides = mergeJSObjects(this.parentOverrides, parser.overridesValue);
        Map<String, Object> runtimeOverrides = buildRuntimeOverrides(finalOverrides);

        if (parser.addonsValue == null && parentAddons == null) {
            return createSimpleInstance(parser.constructorArgs, runtimeOverrides);
        }

        return createWrappedInstance(parser.constructorArgs, runtimeOverrides, this.parentAddons, parser.addonsValue);
    }

    private Object createWrappedInstance(Value[] constructorArgs,
                                         Map<String, Object> runtimeOverrides,
                                         Value parentAddons,
                                         Value childAddons) {
        try {
            Object baseInstance = createBaseJavaInstance(constructorArgs, runtimeOverrides);
            Map<String, Object> wrapperProperties = new HashMap<>();

            // This is the configuration that the *next* child will see as its parent.
            // It is the final, merged state of the instance we are creating *now*.
            Value finalMergedOverrides = mergeJSObjects(this.parentOverrides, runtimeOverridesToValue(runtimeOverrides));
            Value finalMergedAddons = mergeJSObjects(parentAddons, childAddons);

            ExtendedInstanceProxy wrapper = new ExtendedInstanceProxy(wrapperProperties, baseInstance, this.config, finalMergedOverrides, finalMergedAddons);
            Value wrapperVal = context.asValue(wrapper);

            wrapperProperties.put("instance", new MappedInstanceProxy(baseInstance));
            wrapperProperties.put("_self", baseInstance);

            if (parentAddons != null && parentAddons.hasMembers()) {
                Map<String, Object> superAddonsMap = new HashMap<>();
                for (String key : parentAddons.getMemberKeys()) {
                    Value func = parentAddons.getMember(key);
                    Value boundFunc = isJavaScriptFunction(func) ? func.invokeMember("bind", wrapperVal) : func;
                    superAddonsMap.put(key, boundFunc);
                    // Inherit by default
                    wrapperProperties.put(key, boundFunc);
                }
                wrapperProperties.put("_super", ProxyObject.fromMap(superAddonsMap));
            }

            if (childAddons != null && childAddons.hasMembers()) {
                for (String key : childAddons.getMemberKeys()) {
                    Value func = childAddons.getMember(key);
                    // This overwrites the inherited parent addon with the same name.
                    wrapperProperties.put(key, isJavaScriptFunction(func) ? func.invokeMember("bind", wrapperVal) : func);
                }
            }

            return wrapper;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create wrapped instance: " + e.getMessage(), e);
        }
    }

    private Object createBaseJavaInstance(Value[] constructorArgs, Map<String, Object> runtimeOverrides) {
        Object[] javaCtorArgs = convertAllValuesToJava(constructorArgs);
        ProxyObject overridesProxy = ProxyObject.fromMap(runtimeOverrides);
        Object[] finalCtorArgs = appendToArray(javaCtorArgs, overridesProxy);
        Value baseInstanceAsValue = baseAdapterConstructor.newInstance(finalCtorArgs);
        return baseInstanceAsValue.asHostObject();
    }

    // --- Helper methods ---

    private Value runtimeOverridesToValue(Map<String, Object> map) {
        Value obj = context.eval("js", "({})");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            obj.putMember(entry.getKey(), entry.getValue());
        }
        return obj;
    }

    // ... all other helper methods are correct and can remain the same ...
    private void validateArguments(Value[] args) {
        if (args.length == 0) {
            throw new RuntimeException("Cannot extend with mapped names without a configuration object. " +
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
        Value overridesValue = lastArg.getMember("overrides");
        Value addonsValue = lastArg.getMember("addons");
        if (overridesValue == null || overridesValue.isNull()) {
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
        if (overridesArg == null || !overridesArg.hasMembers()) return runtimeOverrides;

        for (String jsMethodName : overridesArg.getMemberKeys()) {
            Value jsValue = overridesArg.getMember(jsMethodName);
            if (jsValue.canExecute()) {
                handleSimpleOverride(jsMethodName, jsValue, runtimeOverrides);
            } else if (jsValue.hasMembers()) {
                handleConflictOverride(jsMethodName, jsValue, runtimeOverrides);
            }
        }
        return runtimeOverrides;
    }
    private void addOverride(Map<String, Object> runtimeOverrides, String jsMethodName, Value jsFunction, MappedClassInfo target) {
        List<String> mappedNames = target.methodMappings().get(jsMethodName);
        if (mappedNames != null && !mappedNames.isEmpty()) {
            for (String runtimeName : mappedNames) {
                runtimeOverrides.put(runtimeName, jsFunction);
            }
        } else {
            runtimeOverrides.put(jsMethodName, jsFunction);
        }
    }
    private void handleSimpleOverride(String jsMethodName, Value jsFunction, Map<String, Object> runtimeOverrides) {
        List<MappedClassInfo> targets = findTargetsForMethod(jsMethodName);
        if (targets.size() > 1) {
            List<String> targetNames = targets.stream().map(MappedClassInfo::yarnName).toList();
            throw new RuntimeException(
                    "Ambiguous override for method '" + jsMethodName + "'. It exists in multiple places: " +
                            targetNames + ". Please specify the target: { '" + targetNames.getFirst() + "': fn, ... }"
            );
        }
        if (targets.isEmpty()) {
            runtimeOverrides.put(jsMethodName, jsFunction);
        } else {
            addOverride(runtimeOverrides, jsMethodName, jsFunction, targets.getFirst());
        }
    }
    private void handleConflictOverride(String jsMethodName, Value fqcnToObject, Map<String, Object> runtimeOverrides) {
        for (String fqcn : fqcnToObject.getMemberKeys()) {
            Value jsFunction = fqcnToObject.getMember(fqcn);
            if (!jsFunction.canExecute()) {
                throw new RuntimeException("Value for FQCN '" + fqcn + "' in override for '" + jsMethodName + "' must be a function.");
            }
            MappedClassInfo target = findTargetByYarnName(fqcn);
            if (target == null) {
                System.err.println("Warning: Override for '" + jsMethodName + "' specified target '" + fqcn +
                        "' which was not found in the list of extended/implemented types.");
                continue;
            }
            addOverride(runtimeOverrides, jsMethodName, jsFunction, target);
        }
    }
    private List<MappedClassInfo> findTargetsForMethod(String jsMethodName) {
        List<MappedClassInfo> found = new ArrayList<>();
        if (config.extendsClass().methodMappings().containsKey(jsMethodName)) {
            found.add(config.extendsClass());
        }
        for (MappedClassInfo interfaceInfo : config.implementsClasses()) {
            if (interfaceInfo.methodMappings().containsKey(jsMethodName)) {
                found.add(interfaceInfo);
            }
        }
        return found;
    }
    private MappedClassInfo findTargetByYarnName(String yarnName) {
        if (config.extendsClass().yarnName().equals(yarnName)) {
            return config.extendsClass();
        }
        return config.implementsClasses().stream()
                .filter(info -> info.yarnName().equals(yarnName))
                .findFirst()
                .orElse(null);
    }
    private Object createSimpleInstance(Value[] constructorArgs, Map<String, Object> runtimeOverrides) {
        try {
            Object[] javaCtorArgs = convertAllValuesToJava(constructorArgs);
            ProxyObject overridesProxy = ProxyObject.fromMap(runtimeOverrides);
            Object[] finalCtorArgs = appendToArray(javaCtorArgs, overridesProxy);
            return baseAdapterConstructor.newInstance(finalCtorArgs);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create simple instance: " + e.getMessage(), e);
        }
    }
    private Object[] convertAllValuesToJava(Value[] values) {
        Object[] javaObjects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            javaObjects[i] = convertValueToJavaObject(values[i]);
        }
        return javaObjects;
    }
    private Object convertValueToJavaObject(Value value) {
        if (value == null || value.isNull()) return null;
        if (value.isHostObject()) return value.asHostObject();
        if (value.isProxyObject()) return value.asProxyObject();
        if (value.isString()) return value.asString();
        if (value.isBoolean()) return value.asBoolean();
        if (value.isNumber()) {
            if (value.fitsInInt()) return value.asInt();
            if (value.fitsInLong()) return value.asLong();
            if (value.fitsInFloat()) return value.asFloat();
            return value.asDouble();
        }
        return value;
    }
    private boolean isJavaScriptFunction(Object value) {
        return (value instanceof Value val) && val.canExecute() && val.hasMember("bind");
    }
    private Value mergeJSObjects(Value parent, Value child) {
        if (parent == null || parent.isNull()) {
            return child;
        }
        if (child == null || child.isNull()) {
            return parent;
        }
        Value merged = context.eval("js", "({})");
        for (String key : parent.getMemberKeys()) {
            merged.putMember(key, parent.getMember(key));
        }
        for (String key : child.getMemberKeys()) {
            merged.putMember(key, child.getMember(key));
        }
        return merged;
    }
    private Object[] appendToArray(Object[] original, Object newElement) {
        Object[] result = new Object[original.length + 1];
        System.arraycopy(original, 0, result, 0, original.length);
        result[original.length] = newElement;
        return result;
    }
    @Override
    public Object getMember(String key) {
        return "prototype".equals(key) ? baseAdapterConstructor.getMember("prototype") : null;
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
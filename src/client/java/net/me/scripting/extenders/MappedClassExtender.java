package net.me.scripting.extenders;

import net.me.scripting.config.ExtensionConfig;
import net.me.scripting.config.MappedClassInfo;
import net.me.scripting.extenders.proxies.ExtendedInstanceProxy;
import net.me.scripting.extenders.proxies.MappedInstanceProxy;
import net.me.scripting.extenders.proxies.RuntimeBinderProxy;
import net.me.scripting.extenders.proxies.SuperProxy;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyInstantiable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.*;
import java.util.stream.Collectors;

public class MappedClassExtender implements ProxyObject, ProxyInstantiable {
    private final ExtensionConfig config;
    private final Context context;
    private final Value baseAdapterConstructor;
    private final Value parentOverrides;
    private final Value parentAddons;
    private final Value parentSuper;

    public MappedClassExtender(ExtensionConfig config, Context context, Value parentOverrides, Value parentAddons, Value parentSuper) {
        this.config = config;
        this.context = context;
        this.parentOverrides = parentOverrides;
        this.parentAddons = parentAddons;
        this.parentSuper = parentSuper;
        this.baseAdapterConstructor = createBaseAdapter();
    }

    private Value createBaseAdapter() {
        Value extendFn = context.eval("js", "Java.extend");
        List<Object> extendArgs = new ArrayList<>();
        extendArgs.add(config.extendsClass().targetClass());
        Value typeFn = context.eval("js", "Java.type");
        for (MappedClassInfo interfaceInfo : config.implementsClasses()) {
            extendArgs.add(typeFn.execute(interfaceInfo.targetClass().getName()));
        }
        return extendFn.execute(extendArgs.toArray());
    }

    @Override
    public Object newInstance(Value... args) {
        validateArguments(args);
        ArgumentParser parser = parseArguments(args);

        Map<String, Object> childRuntimeOverrides = buildRuntimeOverrides(parser.overridesValue);
        RuntimeBinderProxy binder = new RuntimeBinderProxy(childRuntimeOverrides);
        Object baseInstance = createBaseJavaInstanceWithBinder(parser.constructorArgs, binder);

        Map<String, Object> wrapperProperties = new HashMap<>();
        Value finalMergedOverrides = mergeJSObjects(this.parentOverrides, parser.overridesValue);
        Value finalMergedAddons = mergeJSObjects(this.parentAddons, parser.addonsValue);
        ExtendedInstanceProxy wrapper = new ExtendedInstanceProxy(wrapperProperties, baseInstance, this.config, finalMergedOverrides, finalMergedAddons);

        binder.setBindingTarget(wrapper);
        populateWrapper(wrapper, baseInstance, parser.addonsValue);
        return wrapper;
    }

    private Object createBaseJavaInstanceWithBinder(Value[] constructorArgs, RuntimeBinderProxy childBinder) {
        Object[] javaCtorArgs = convertAllValuesToJava(constructorArgs);
        Object[] finalCtorArgs;

        if (this.parentOverrides != null && this.parentOverrides.hasMembers()) {
            Map<String, Object> parentRuntimeOverrides = buildRuntimeOverrides(this.parentOverrides);
            ProxyObject parentOverridesProxy = ProxyObject.fromMap(parentRuntimeOverrides);
            finalCtorArgs = new Object[javaCtorArgs.length + 2];
            System.arraycopy(javaCtorArgs, 0, finalCtorArgs, 0, javaCtorArgs.length);
            finalCtorArgs[javaCtorArgs.length] = parentOverridesProxy;
            finalCtorArgs[javaCtorArgs.length + 1] = childBinder;
        } else {
            finalCtorArgs = appendToArray(javaCtorArgs, childBinder);
        }

        try {
            return baseAdapterConstructor.newInstance(finalCtorArgs).asHostObject();
        } catch (Exception e) {
            String ctorSignature = Arrays.stream(finalCtorArgs).map(a -> a == null ? "null" : a.getClass().getName()).collect(Collectors.joining(", "));
            throw new RuntimeException("Failed to instantiate adapter. Constructor call with signature (" + ctorSignature + ") failed.", e);
        }
    }

    private void populateWrapper(ExtendedInstanceProxy wrapper, Object baseInstance, Value childAddons) {
        Map<String, Object> wrapperProperties = wrapper.getPropertiesForModification();
        Value wrapperVal = context.asValue(wrapper);

        wrapperProperties.put("instance", new MappedInstanceProxy(baseInstance));
        wrapperProperties.put("_self", baseInstance);

        if (this.parentOverrides != null) {
            Map<String, List<String>> parentMethodMappings = this.config.extendsClass().methodMappings();
            wrapperProperties.put("_super", new SuperProxy(this.parentOverrides, this.parentSuper, wrapperVal, parentMethodMappings));
        } else {
            wrapperProperties.put("_super", context.eval("js", "Java.super").execute(baseInstance));
        }

        if (this.parentAddons != null) {
            for (String key : this.parentAddons.getMemberKeys()) {
                Value member = this.parentAddons.getMember(key);
                if (member.canExecute()) {
                    wrapperProperties.put(key, member.invokeMember("bind", wrapperVal));
                } else {
                    wrapperProperties.put(key, member);
                }
            }
        }
        if (childAddons != null) {
            for (String key : childAddons.getMemberKeys()) {
                Value member = childAddons.getMember(key);
                if (member.canExecute()) {
                    wrapperProperties.put(key, member.invokeMember("bind", wrapperVal));
                } else {
                    wrapperProperties.put(key, member);
                }
            }
        }
    }

    private void validateArguments(Value[] args) {
        if (args.length == 0) {
            throw new RuntimeException("Cannot extend with mapped names without a configuration object. Pass at least { overrides: {} } or { overrides: {}, addons: {} }.");
        }
    }

    private record ArgumentParser(Value overridesValue, Value addonsValue, Value[] constructorArgs) {
    }

    private ArgumentParser parseArguments(Value[] args) {
        Value lastArg = args[args.length - 1];
        if (!isObjectLike(lastArg)) {
            throw new RuntimeException("The last argument must be a configuration object with 'overrides' and optionally 'addons' keys.");
        }
        Value[] constructorArgs = new Value[args.length - 1];
        System.arraycopy(args, 0, constructorArgs, 0, args.length - 1);
        Value overridesValue = lastArg.getMember("overrides");
        Value addonsValue = lastArg.getMember("addons");
        if (overridesValue == null || overridesValue.isNull()) {
            throw new RuntimeException("Configuration object must contain an 'overrides' key, even if empty: { overrides: {} }");
        }
        return new ArgumentParser(overridesValue, addonsValue, constructorArgs);
    }

    private boolean isObjectLike(Value value) {
        return value != null && (value.hasMembers() || value.isProxyObject());
    }

    private Map<String, Object> buildRuntimeOverrides(Value overridesArg) {
        Map<String, Object> finalRuntimeOverrides = new HashMap<>();
        if (overridesArg == null || !overridesArg.hasMembers()) return finalRuntimeOverrides;
        for (String jsMethodName : overridesArg.getMemberKeys()) {
            Value jsValue = overridesArg.getMember(jsMethodName);
            if (jsValue.canExecute()) {
                handleSimpleOverride(jsMethodName, jsValue, finalRuntimeOverrides);
            } else if (jsValue.hasMembers()) {
                handleConflictOverride(jsMethodName, jsValue, finalRuntimeOverrides);
            }
        }
        return finalRuntimeOverrides;
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
            throw new RuntimeException("Ambiguous override for method '" + jsMethodName + "'. It exists in multiple places: " + targetNames + ". Please specify the target: { '" + targetNames.getFirst() + "': fn, ... }");
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
                System.err.println("Warning: Override for '" + jsMethodName + "' specified target '" + fqcn + "' which was not found in the list of extended/implemented types.");
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
        return config.implementsClasses().stream().filter(info -> info.yarnName().equals(yarnName)).findFirst().orElse(null);
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
/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Réthoré
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.me.scripting.extenders;

import lombok.Getter;
import net.me.Main;
import net.me.scripting.WrapperConstants;
import net.me.scripting.config.ExtensionConfig;
import net.me.scripting.config.MappedClassInfo;
import net.me.scripting.engine.ScriptConstants;
import net.me.scripting.engine.ScriptingClassResolver;
import net.me.scripting.extenders.proxies.ExtendedInstanceProxy;
import net.me.scripting.extenders.proxies.MappedInstanceProxy;
import net.me.scripting.extenders.proxies.RuntimeBinderProxy;
import net.me.scripting.extenders.proxies.SuperProxy;
import net.me.scripting.utils.ScriptUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyInstantiable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.*;
import java.util.stream.Collectors;

public class MappedClassExtender implements ProxyObject, ProxyInstantiable {
    private static final String BIND_METHOD = "bind";

    @Getter
    private final ExtensionConfig config;
    private final Context context;
    private final Value baseAdapterConstructor;
    private final Value parentOverrides;
    private final Value parentAddons;
    private final Value parentSuper;
    private final ScriptingClassResolver resolver;
    private final Map<String, MappedClassInfo> unambiguousMethodTargets = new HashMap<>();

    private final Value jsImplementation;


    public MappedClassExtender(ExtensionConfig config, Context context, Value parentOverrides, Value parentAddons, Value parentSuper, ScriptingClassResolver resolver, Value jsImplementation) {
        this.config = config;
        this.context = context;
        this.parentOverrides = parentOverrides;
        this.parentAddons = parentAddons;
        this.parentSuper = parentSuper;
        this.resolver = resolver;
        this.jsImplementation = jsImplementation;
        this.baseAdapterConstructor = createBaseAdapter();
        precomputeOverrideTargets();
    }

    private void precomputeOverrideTargets() {
        Set<String> allMethodNames = new HashSet<>(config.extendsClass().methodMappings().keySet());
        config.implementsClasses().forEach(info -> allMethodNames.addAll(info.methodMappings().keySet()));
        for (String methodName : allMethodNames) {
            List<MappedClassInfo> targets = findTargetsForMethod(methodName);
            if (targets.size() == 1) {
                unambiguousMethodTargets.put(methodName, targets.getFirst());
            }
        }
    }

    private Value createBaseAdapter() {
        Value extendFn = context.eval(ScriptConstants.JS, "Java.extend");
        List<Object> extendArgs = new ArrayList<>();
        extendArgs.add(config.extendsClass().targetClass());
        for (MappedClassInfo interfaceInfo : config.implementsClasses()) {
            extendArgs.add(interfaceInfo.targetClass());
        }
        return extendFn.execute(extendArgs.toArray());
    }

    @Override
    public Object newInstance(Value... constructorArgs) {
        Value overridesValue = context.eval(ScriptConstants.JS, "({})");
        Value addonsValue = context.eval(ScriptConstants.JS, "({})");

        if (this.jsImplementation != null && this.jsImplementation.hasMembers()) {
            for (String key : this.jsImplementation.getMemberKeys()) {
                Value member = this.jsImplementation.getMember(key);
                if (member.canExecute() && isMethodAnOverride(key)) {
                    overridesValue.putMember(key, member);
                } else {
                    addonsValue.putMember(key, member);
                }
            }
        }

        Map<String, Object> childRuntimeOverrides = buildRuntimeOverrides(overridesValue);

        Map<String, Object> parentRuntimeOverrides = new HashMap<>();
        if (this.parentOverrides != null && this.parentOverrides.hasMembers()) {
            parentRuntimeOverrides.putAll(buildRuntimeOverrides(this.parentOverrides));
        }

        Map<String, Object> mergedRuntimeOverrides = new HashMap<>(parentRuntimeOverrides);
        mergedRuntimeOverrides.putAll(childRuntimeOverrides);

        RuntimeBinderProxy mergedBinder = new RuntimeBinderProxy(mergedRuntimeOverrides);

        Object baseInstance = createBaseJavaInstanceWithBinder(constructorArgs, mergedBinder);

        Map<String, Object> wrapperProperties = new HashMap<>();
        Value finalMergedOverrides = mergeJSObjects(this.parentOverrides, overridesValue);
        Value finalMergedAddons = mergeJSObjects(this.parentAddons, addonsValue);
        ExtendedInstanceProxy wrapper = new ExtendedInstanceProxy(wrapperProperties, baseInstance, this.config, finalMergedOverrides, finalMergedAddons);

        mergedBinder.setBindingTarget(wrapper);
        populateWrapper(wrapper, baseInstance, addonsValue);
        return wrapper;
    }

    private boolean isMethodAnOverride(String methodName) {
        if (config.extendsClass().methodMappings().containsKey(methodName)) {
            return true;
        }
        for (MappedClassInfo interfaceInfo : config.implementsClasses()) {
            if (interfaceInfo.methodMappings().containsKey(methodName)) {
                return true;
            }
        }
        return false;
    }

    private Object createBaseJavaInstanceWithBinder(Value[] constructorArgs, RuntimeBinderProxy binder) {
        Object[] javaCtorArgs = ScriptUtils.unwrapArgs(constructorArgs, null);

        Object[] finalCtorArgs = appendToArray(javaCtorArgs, binder);

        try {
            return baseAdapterConstructor.newInstance(finalCtorArgs).asHostObject();
        } catch (PolyglotException e) {
            String ctorSignature = Arrays.stream(finalCtorArgs).map(a -> a == null ? "null" : a.getClass().getName()).collect(Collectors.joining(", "));
            throw new IllegalStateException("Failed to instantiate adapter. Constructor call with signature (" + ctorSignature + ") failed.", e);
        }
    }

    private void populateWrapper(ExtendedInstanceProxy wrapper, Object baseInstance, Value childAddons) {
        Map<String, Object> wrapperProperties = wrapper.getPropertiesForModification();
        Value wrapperVal = context.asValue(wrapper);

        MappedInstanceProxy javaProxy = new MappedInstanceProxy(baseInstance, this.resolver);

        wrapper.setJavaInstanceProxy(javaProxy);

        wrapperProperties.put(WrapperConstants.SELF, baseInstance);

        Value actualGrandParentSuper = (this.parentSuper != null) ? this.parentSuper : context.eval(ScriptConstants.JS, "Java.super").execute(baseInstance);
        Map<String, List<String>> currentMethodMappings = this.config.extendsClass().methodMappings();
        wrapperProperties.put(WrapperConstants.SUPER, new SuperProxy(this.parentOverrides, actualGrandParentSuper, wrapperVal, currentMethodMappings));

        if (this.parentAddons != null) {
            for (String key : this.parentAddons.getMemberKeys()) {
                Value member = this.parentAddons.getMember(key);
                wrapperProperties.put(key, member.canExecute() ? member.invokeMember(BIND_METHOD, wrapperVal) : member);
            }
        }

        if (childAddons != null) {
            for (String key : childAddons.getMemberKeys()) {
                Value member = childAddons.getMember(key);
                wrapperProperties.put(key, member.canExecute() ? member.invokeMember(BIND_METHOD, wrapperVal) : member);
            }
        }
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
        MappedClassInfo unambiguousTarget = unambiguousMethodTargets.get(jsMethodName);
        if (unambiguousTarget != null) {
            addOverride(runtimeOverrides, jsMethodName, jsFunction, unambiguousTarget);
        } else {
            List<MappedClassInfo> targets = findTargetsForMethod(jsMethodName);
            if (targets.size() > 1) {
                List<String> targetNames = targets.stream().map(MappedClassInfo::namedClassName).toList();
                throw new IllegalArgumentException("Ambiguous override for method '" + jsMethodName + "'. It exists in multiple places: " + targetNames + ". Please specify the target: { overrides: { '" + jsMethodName + "': { '" + targetNames.getFirst() + "': fn } } }");
            }
            if (targets.isEmpty()) {
                runtimeOverrides.put(jsMethodName, jsFunction);
            } else {
                addOverride(runtimeOverrides, jsMethodName, jsFunction, targets.getFirst());
            }
        }
    }

    private void handleConflictOverride(String jsMethodName, Value fqcnToObject, Map<String, Object> runtimeOverrides) {
        for (String fqcn : fqcnToObject.getMemberKeys()) {
            Value jsFunction = fqcnToObject.getMember(fqcn);
            if (!jsFunction.canExecute()) {
                throw new IllegalArgumentException("Value for FQCN '" + fqcn + "' in override for '" + jsMethodName + "' must be a function.");
            }
            MappedClassInfo target = findTargetByNamedClassName(fqcn);
            if (target == null) {
                Main.LOGGER.warn("Override for '{}' specified target '{}' which was not found in the list of extended/implemented types.", jsMethodName, fqcn);
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

    private MappedClassInfo findTargetByNamedClassName(String namedClassName) {
        if (config.extendsClass().namedClassName().equals(namedClassName)) {
            return config.extendsClass();
        }
        return config.implementsClasses().stream().filter(info -> info.namedClassName().equals(namedClassName)).findFirst().orElse(null);
    }

    private Value mergeJSObjects(Value parent, Value child) {
        if (parent == null || parent.isNull()) return child;
        if (child == null || child.isNull()) return parent;
        Value merged = context.eval(ScriptConstants.JS, "({})");
        for (String key : parent.getMemberKeys()) merged.putMember(key, parent.getMember(key));
        for (String key : child.getMemberKeys()) merged.putMember(key, child.getMember(key));
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
        if (WrapperConstants.PROTOTYPE.equals(key)) {
            return baseAdapterConstructor.getMember(WrapperConstants.PROTOTYPE);
        }

        if (WrapperConstants.HAS_INSTANCE_SYMBOL.equals(key)) {
            return (ProxyExecutable) (Value... args) -> {
                if (args.length != 1) {
                    return false;
                }
                Object instanceToCheck = ScriptUtils.unwrapReceiver(args[0]);

                if (instanceToCheck instanceof ExtendedInstanceProxy proxy) {
                    return proxy.getOriginalConfig() == this.config;
                }
                return false;
            };
        }

        return null;
    }

    @Override
    public Object getMemberKeys() {
        return new String[]{WrapperConstants.PROTOTYPE, WrapperConstants.HAS_INSTANCE_SYMBOL};
    }

    @Override
    public boolean hasMember(String key) {
        return WrapperConstants.PROTOTYPE.equals(key) || WrapperConstants.HAS_INSTANCE_SYMBOL.equals(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot set members on MappedClassExtender function object.");
    }
}

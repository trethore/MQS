/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
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

package net.me.scripting.engine;

import net.me.Main;
import net.me.scripting.WrapperConstants;
import net.me.scripting.config.ExtensionConfig;
import net.me.scripting.config.MappedClassInfo;
import net.me.scripting.extenders.MappedClassExtender;
import net.me.scripting.extenders.proxies.ExtendedInstanceProxy;
import net.me.scripting.extenders.proxies.MappedInstanceProxy;
import net.me.scripting.utils.MappingUtils;
import net.me.scripting.utils.ScriptUtils;
import net.me.scripting.wrappers.JsClassWrapper;
import net.me.scripting.wrappers.JsObjectWrapper;
import net.me.scripting.wrappers.LazyJsClassHolder;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.lang.reflect.Field;
import java.util.*;

public class ScriptingApi {
    private static final String EXTENDS = "extends";
    private static final String IMPLEMENTS = "implements";

    private ScriptingApi() {
    }

    public static ProxyExecutable createImportClassProxy(ScriptingClassResolver resolver, Context context) {
        return args -> {
            if (args.length == 0 || !args[0].isString()) {
                throw new IllegalArgumentException("importClass requires a FQCN string argument (Yarn mappings).");
            }
            String name = args[0].asString();
            if (!resolver.isClassAllowed(name)) {
                throw new SecurityException("Class not allowed: " + name);
            }

            String runtime = resolver.getRuntimeName(name);
            if (runtime != null) {
                return resolver.getOrCreateWrapper(runtime);
            }
            try {
                return context.eval(ScriptConstants.JS, "Java.type('" + name + "')");
            } catch (PolyglotException e) {
                throw new IllegalArgumentException("Unknown class or could not load host class: " + name, e);
            }
        };
    }

    public static ProxyExecutable createWrapProxy(ScriptingClassResolver resolver) {
        return args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("wrap() requires exactly one argument: the instance to wrap.");
            }
            Value v = args[0];
            if (v.isProxyObject()) {
                Object proxy = v.asProxyObject();
                if (proxy instanceof ExtendedInstanceProxy || proxy instanceof JsObjectWrapper || proxy instanceof MappedInstanceProxy) {
                    return v;
                }
            }
            Object javaInstance = ScriptUtils.unwrapReceiver(v);
            if (javaInstance == null) {
                throw new IllegalArgumentException("The instance passed to wrap() was null or could not be unwrapped to a Java object.");
            }
            Class<?> instanceClass = javaInstance.getClass();

            MappingUtils.ClassMappings cm = MappingUtils.combineMappings(instanceClass, resolver.getRuntimeToYarnMap(), resolver.getMethodMap(), resolver.getFieldMap());

            return new JsObjectWrapper(
                    javaInstance,
                    instanceClass,
                    cm.methods(),
                    cm.fields(),
                    resolver.getMappingsManager(),
                    resolver.getScriptManager()
            );
        };
    }

    public static ProxyExecutable createExtendMappedProxy(ScriptingClassResolver resolver, Context context) {
        return args -> {
            ExtendMappedArgs validatedArgs = requireExtendMappedArgs(args);
            ExtendMappedSetup setup = resolveExtendMappedSetup(validatedArgs.configArg(), context, resolver);
            return new MappedClassExtender(
                    setup.config(),
                    context,
                    setup.parentOverrides(),
                    setup.parentAddons(),
                    setup.parentSuper(),
                    resolver,
                    validatedArgs.implementationArg()
            );
        };
    }

    public static ProxyExecutable createExportModuleProxy(ThreadLocal<Map<String, Value>> perFileExports) {
        return args -> {
            Map<String, Value> exportsMap = perFileExports.get();
            if (exportsMap == null) {
                Main.LOGGER.warn("exportModule called outside of a script discovery or enablement context. Ignoring.");
                return null;
            }

            for (Value arg : args) {
                if (arg != null && arg.hasArrayElements()) {
                    for (long i = 0; i < arg.getArraySize(); i++) {
                        addModule(exportsMap, arg.getArrayElement(i));
                    }
                } else {
                    addModule(exportsMap, arg);
                }
            }
            return null;
        };
    }

    private static Class<?> getClassFromValue(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        Object proxy = value.isProxyObject() ? value.asProxyObject() : null;
        if (proxy instanceof LazyJsClassHolder holder) {
            return holder.getWrapper().getTargetClass();
        }
        if (proxy instanceof JsClassWrapper wrapper) {
            return wrapper.getTargetClass();
        }
        Object unwrapped = ScriptUtils.unwrapReceiver(value);
        if (unwrapped instanceof Class) {
            return (Class<?>) unwrapped;
        }
        return null;
    }


    public static ProxyExecutable createIsInstanceOfProxy() {
        return args -> {
            if (args.length != 2) {
                throw new IllegalArgumentException("isInstanceOf(instance, class) requires exactly two arguments.");
            }
            Object rawInstance = ScriptUtils.unwrapReceiver(args[0]);
            Class<?> rawClass = getClassFromValue(args[1]);

            if (rawClass == null) {
                throw new IllegalArgumentException("The second argument to isInstanceOf must be a class.");
            }

            if (rawInstance == null) {
                return false;
            }
            return rawClass.isInstance(rawInstance);
        };
    }


    private static void addModule(Map<String, Value> exportsMap, Value moduleValue) {
        if (moduleValue != null && moduleValue.canInstantiate()) {
            exportsMap.put(moduleValue.getMetaQualifiedName(), moduleValue);
            return;
        }
        Main.LOGGER.warn("An argument to exportModule was not a valid, instantiable class. Ignoring: {}", moduleValue);
    }

    private static ExtensionConfig parseExtensionConfig(Value configArg, Context context, ScriptingClassResolver resolver, Value extendsValueOverride) {
        Value extendsValue = (extendsValueOverride != null) ? extendsValueOverride : configArg.getMember(EXTENDS);
        if (extendsValue == null) {
            throw new IllegalArgumentException("Configuration object must have an '" + EXTENDS + "' property.");
        }

        MappedClassInfo extendsInfo = extractInfoFromValue(extendsValue, resolver);
        List<MappedClassInfo> implementsInfos = new ArrayList<>();
        addImplementsFromConfig(configArg, resolver, implementsInfos);
        return new ExtensionConfig(extendsInfo, filterNonNull(implementsInfos), context);
    }

    private static MappedClassInfo extractInfoFromValue(Value value, ScriptingClassResolver resolver) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isProxyObject()) {
            return extractInfoFromProxy(value.asProxyObject(), resolver);
        }
        return extractInfoFromHostClass(value, resolver);
    }

    private static ExtendMappedArgs requireExtendMappedArgs(Value[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("extendMapped() requires exactly two arguments: extendMapped(config, implementation)");
        }
        Value configArg = args[0];
        Value implementationArg = args[1];

        if (!configArg.hasMembers() || !configArg.hasMember(EXTENDS)) {
            throw new IllegalArgumentException("First argument must be a configuration object with an '" + EXTENDS + "' property.");
        }
        if (!implementationArg.hasMembers() && !implementationArg.isProxyObject()) {
            throw new IllegalArgumentException("Second argument must be an implementation object containing methods and properties.");
        }

        return new ExtendMappedArgs(configArg, implementationArg);
    }

    private static ExtendMappedSetup resolveExtendMappedSetup(Value configArg, Context context, ScriptingClassResolver resolver) {
        Value extendsValue = configArg.getMember(EXTENDS);
        if (extendsValue.isProxyObject()) {
            Object proxy = extendsValue.asProxyObject();
            if (proxy instanceof MappedClassExtender) {
                ExtensionConfig config = parseExtensionConfig(configArg, context, resolver, extendsValue);
                return new ExtendMappedSetup(config, null, null, null);
            }
            if (proxy instanceof ExtendedInstanceProxy parentProxy) {
                return buildSetupFromParentProxy(configArg, parentProxy, extendsValue, context, resolver);
            }
        }
        ExtensionConfig config = parseExtensionConfig(configArg, context, resolver, extendsValue);
        return new ExtendMappedSetup(config, null, null, null);
    }

    private static ExtendMappedSetup buildSetupFromParentProxy(
            Value configArg,
            ExtendedInstanceProxy parentProxy,
            Value extendsValue,
            Context context,
            ScriptingClassResolver resolver
    ) {
        Value parentOverrides = parentProxy.getOriginalOverrides();
        Value parentAddons = parentProxy.getOriginalAddons();
        Value parentSuper = extendsValue.getMember(WrapperConstants.SUPER);

        ExtensionConfig originalConfig = parentProxy.getOriginalConfig();
        MappedClassInfo newExtendsInfo = originalConfig.extendsClass();
        List<MappedClassInfo> allImplements = new ArrayList<>(originalConfig.implementsClasses());
        addImplementsFromConfig(configArg, resolver, allImplements);
        List<MappedClassInfo> finalImplements = dedupeImplements(allImplements);

        ExtensionConfig config = new ExtensionConfig(newExtendsInfo, filterNonNull(finalImplements), context);
        return new ExtendMappedSetup(config, parentOverrides, parentAddons, parentSuper);
    }

    private static void addImplementsFromConfig(Value configArg, ScriptingClassResolver resolver, List<MappedClassInfo> target) {
        if (!configArg.hasMember(IMPLEMENTS)) {
            return;
        }
        Value impl = configArg.getMember(IMPLEMENTS);
        if (impl.hasArrayElements()) {
            for (long i = 0; i < impl.getArraySize(); i++) {
                target.add(extractInfoFromValue(impl.getArrayElement(i), resolver));
            }
            return;
        }
        target.add(extractInfoFromValue(impl, resolver));
    }

    private static List<MappedClassInfo> dedupeImplements(List<MappedClassInfo> implementsInfos) {
        return new ArrayList<>(new LinkedHashSet<>(implementsInfos));
    }

    private static List<MappedClassInfo> filterNonNull(List<MappedClassInfo> infos) {
        return infos.stream().filter(Objects::nonNull).toList();
    }

    private static MappedClassInfo extractInfoFromProxy(Object proxy, ScriptingClassResolver resolver) {
        MappedClassInfo extenderInfo = extractInfoFromExtender(proxy);
        if (extenderInfo != null) {
            return extenderInfo;
        }

        WrapperInfo wrapperInfo = extractWrapperInfo(proxy);
        if (wrapperInfo == null || wrapperInfo.wrapper() == null) {
            return null;
        }
        String yarnName = resolveYarnName(wrapperInfo, resolver);
        JsClassWrapper wrapper = wrapperInfo.wrapper();
        return new MappedClassInfo(yarnName, wrapper.getTargetClass(), wrapper.getMethodMappings(), wrapper.getFieldMappings());
    }

    private static MappedClassInfo extractInfoFromExtender(Object proxy) {
        if (!(proxy instanceof MappedClassExtender extender)) {
            return null;
        }
        try {
            Field configField = MappedClassExtender.class.getDeclaredField("config");
            configField.setAccessible(true);
            ExtensionConfig parentConfig = (ExtensionConfig) configField.get(extender);
            return parentConfig.extendsClass();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not extract config from parent MappedClassExtender.", e);
        }
    }

    private static WrapperInfo extractWrapperInfo(Object proxy) {
        if (proxy instanceof LazyJsClassHolder holder) {
            return new WrapperInfo(holder.getWrapper(), readLazyYarnName(holder));
        }
        if (proxy instanceof JsClassWrapper wrapper) {
            return new WrapperInfo(wrapper, null);
        }
        return null;
    }

    private static String readLazyYarnName(LazyJsClassHolder holder) {
        try {
            Field yarnNameField = LazyJsClassHolder.class.getDeclaredField("yarnName");
            yarnNameField.setAccessible(true);
            return (String) yarnNameField.get(holder);
        } catch (ReflectiveOperationException _) {
            return null;
        }
    }

    private static String resolveYarnName(WrapperInfo wrapperInfo, ScriptingClassResolver resolver) {
        if (wrapperInfo.yarnName() != null) {
            return wrapperInfo.yarnName();
        }
        String runtimeName = wrapperInfo.wrapper().getTargetClass().getName();
        return resolver.getRuntimeToYarnMap().getOrDefault(runtimeName, runtimeName);
    }

    private static MappedClassInfo extractInfoFromHostClass(Value value, ScriptingClassResolver resolver) {
        if (!value.isHostObject() || !(value.asHostObject() instanceof Class)) {
            return null;
        }
        Class<?> clazz = value.as(Class.class);
        String yarnName = resolver.getRuntimeToYarnMap().get(clazz.getName());
        if (yarnName != null) {
            MappingUtils.ClassMappings cm = MappingUtils.combineMappings(clazz, resolver.getRuntimeToYarnMap(), resolver.getMethodMap(), resolver.getFieldMap());
            return new MappedClassInfo(yarnName, clazz, cm.methods(), cm.fields());
        }
        return new MappedClassInfo(clazz.getName(), clazz, Collections.emptyMap(), Collections.emptyMap());
    }

    private record ExtendMappedArgs(Value configArg, Value implementationArg) {
    }

    private record ExtendMappedSetup(ExtensionConfig config, Value parentOverrides, Value parentAddons,
                                     Value parentSuper) {
    }

    private record WrapperInfo(JsClassWrapper wrapper, String yarnName) {
    }
}

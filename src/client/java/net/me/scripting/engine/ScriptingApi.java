package net.me.scripting.engine;

import net.me.Main;
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
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.*;

public class ScriptingApi {

    public static ProxyExecutable createImportClassProxy(ScriptingClassResolver resolver, Context context) {
        return args -> {
            if (args.length == 0 || !args[0].isString())
                throw new RuntimeException("importClass requires a FQCN string argument (Yarn mappings).");
            var name = args[0].asString();
            if (!resolver.isClassAllowed(name)) throw new RuntimeException("Class not allowed: " + name);

            String runtime = resolver.getRuntimeName(name);
            if (runtime != null) return resolver.getOrCreateWrapper(runtime);
            try {
                return context.eval("js", "Java.type('" + name + "')");
            } catch (Exception e) {
                throw new RuntimeException("Unknown class or could not load host class: " + name, e);
            }
        };
    }

    public static ProxyExecutable createWrapProxy(ScriptingClassResolver resolver) {
        return args -> {
            if (args.length != 1) {
                throw new RuntimeException("wrap() requires exactly one argument: the instance to wrap.");
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
                throw new RuntimeException("The instance passed to wrap() was null or could not be unwrapped to a Java object.");
            }
            Class<?> instanceClass = javaInstance.getClass();
            var cm = MappingUtils.combineMappings(instanceClass, resolver.getRuntimeToYarnMap(), resolver.getMethodMap(), resolver.getFieldMap());
            return new JsObjectWrapper(javaInstance, instanceClass, cm.methods(), cm.fields());
        };
    }

    public static ProxyExecutable createExtendMappedProxy(ScriptingClassResolver resolver, Context context) {
        return args -> {
            if (args.length != 1) {
                throw new RuntimeException("extendMapped() requires exactly one configuration object argument.");
            }
            Value configArg = args[0];
            if (!configArg.hasMembers() || !configArg.hasMember("extends")) {
                throw new RuntimeException("Configuration argument must be an object with an 'extends' property.");
            }

            Value extendsValue = configArg.getMember("extends");
            Value parentOverrides = null;
            Value parentAddons = null;
            Value parentSuper = null;
            ExtensionConfig config;

            if (extendsValue.isProxyObject() && extendsValue.asProxyObject() instanceof MappedClassExtender) {
                config = parseExtensionConfig(configArg, context, resolver, extendsValue);
            } else if (extendsValue.isProxyObject() && extendsValue.asProxyObject() instanceof ExtendedInstanceProxy parentProxy) {
                parentOverrides = parentProxy.getOriginalOverrides();
                parentAddons = parentProxy.getOriginalAddons();
                parentSuper = extendsValue.getMember("_super");

                ExtensionConfig originalConfig = parentProxy.getOriginalConfig();
                MappedClassInfo newExtendsInfo = originalConfig.extendsClass();
                List<MappedClassInfo> allImplements = new ArrayList<>(originalConfig.implementsClasses());
                if (configArg.hasMember("implements")) {
                    Value impl = configArg.getMember("implements");
                    if (impl.hasArrayElements()) {
                        for (long i = 0; i < impl.getArraySize(); i++) {
                            allImplements.add(extractInfoFromValue(impl.getArrayElement(i), resolver));
                        }
                    } else {
                        allImplements.add(extractInfoFromValue(impl, resolver));
                    }
                }
                List<MappedClassInfo> finalImplements = new ArrayList<>(new LinkedHashSet<>(allImplements));
                config = new ExtensionConfig(newExtendsInfo, finalImplements.stream().filter(Objects::nonNull).toList(), context);
            } else {
                config = parseExtensionConfig(configArg, context, resolver, extendsValue);
            }

            return new MappedClassExtender(config, context, parentOverrides, parentAddons, parentSuper);
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

    private static void addModule(Map<String, Value> exportsMap, Value moduleValue) {
        if (moduleValue != null && moduleValue.canInstantiate()) {
            Value nameValue = moduleValue.getMember("name");
            if (nameValue != null && nameValue.isString()) {
                String moduleName = nameValue.asString();
                if (moduleName != null && !moduleName.isEmpty()) {
                    exportsMap.put(moduleName, moduleValue);
                    return;
                }
            }
        }
        Main.LOGGER.warn("An argument to exportModule was not a valid, named, instantiable class. Ignoring: {}", moduleValue);
    }

    private static ExtensionConfig parseExtensionConfig(Value configArg, Context context, ScriptingClassResolver resolver, Value extendsValueOverride) {
        Value extendsValue = (extendsValueOverride != null) ? extendsValueOverride : configArg.getMember("extends");
        if (extendsValue == null) {
            throw new RuntimeException("Configuration object must have an 'extends' property.");
        }

        MappedClassInfo extendsInfo = extractInfoFromValue(extendsValue, resolver);
        List<MappedClassInfo> implementsInfos = new ArrayList<>();
        if (configArg.hasMember("implements")) {
            Value impl = configArg.getMember("implements");
            if (impl.hasArrayElements()) {
                for (long i = 0; i < impl.getArraySize(); i++) {
                    implementsInfos.add(extractInfoFromValue(impl.getArrayElement(i), resolver));
                }
            } else {
                implementsInfos.add(extractInfoFromValue(impl, resolver));
            }
        }
        return new ExtensionConfig(extendsInfo, implementsInfos.stream().filter(Objects::nonNull).toList(), context);
    }

    private static MappedClassInfo extractInfoFromValue(Value value, ScriptingClassResolver resolver) {
        if (value.isProxyObject()) {
            Object proxy = value.asProxyObject();
            if (proxy instanceof MappedClassExtender extender) {
                try {
                    java.lang.reflect.Field configField = MappedClassExtender.class.getDeclaredField("config");
                    configField.setAccessible(true);
                    ExtensionConfig parentConfig = (ExtensionConfig) configField.get(extender);
                    return parentConfig.extendsClass();
                } catch (Exception e) {
                    throw new RuntimeException("Could not extract config from parent MappedClassExtender.", e);
                }
            }

            JsClassWrapper wrapper = null;
            String yarnName = null;
            if (proxy instanceof LazyJsClassHolder holder) {
                wrapper = holder.getWrapper();
                try {
                    java.lang.reflect.Field yarnNameField = LazyJsClassHolder.class.getDeclaredField("yarnName");
                    yarnNameField.setAccessible(true);
                    yarnName = (String) yarnNameField.get(holder);
                } catch (Exception ignored) {
                }
            } else if (proxy instanceof JsClassWrapper w) {
                wrapper = w;
            }

            if (wrapper != null) {
                if (yarnName == null) {
                    yarnName = resolver.getRuntimeToYarnMap().getOrDefault(wrapper.getTargetClass().getName(), wrapper.getTargetClass().getName());
                }
                return new MappedClassInfo(yarnName, wrapper.getTargetClass(), wrapper.getMethodMappings(), wrapper.getFieldMappings());
            }
        } else if (value.isHostObject() && value.asHostObject() instanceof Class) {
            Class<?> clazz = value.as(Class.class);
            String yarnName = resolver.getRuntimeToYarnMap().get(clazz.getName());
            if (yarnName != null) {
                var cm = MappingUtils.combineMappings(clazz, resolver.getRuntimeToYarnMap(), resolver.getMethodMap(), resolver.getFieldMap());
                return new MappedClassInfo(yarnName, clazz, cm.methods(), cm.fields());
            } else {
                return new MappedClassInfo(clazz.getName(), clazz, Collections.emptyMap(), Collections.emptyMap());
            }
        }
        return null;
    }
}
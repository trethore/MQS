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

package net.me.scripting.engine;

import net.me.Main;
import net.me.scripting.WrapperConstants;
import net.me.scripting.config.ExtensionConfig;
import net.me.scripting.config.MappedClassInfo;
import net.me.scripting.extenders.MappedClassExtender;
import net.me.scripting.extenders.proxies.ExtendedInstanceProxy;
import net.me.scripting.extenders.proxies.MappedInstanceProxy;
import net.me.scripting.typings.MqsApiFragment;
import net.me.scripting.typings.TypingsConstants;
import net.me.scripting.typings.schema.TsObject;
import net.me.scripting.utils.MappingUtils;
import net.me.scripting.utils.ScriptUtils;
import net.me.scripting.wrappers.JsClassWrapper;
import net.me.scripting.wrappers.JsObjectWrapper;
import net.me.scripting.wrappers.LazyJsClassHolder;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.*;

import static net.me.scripting.typings.schema.TsDescriptors.*;

public class ScriptingApi {
    private static final String EXTENDS = "extends";
    private static final String IMPLEMENTS = "implements";

    private ScriptingApi() {
    }

    public static MqsApiFragment describeTypeScript() {
        return new MqsApiFragment(
                List.of(
                        alias("MQSDisposer", "() => void"),
                        alias("MQSAnyFunction", "(...args: any[]) => unknown")
                ),
                List.of(
                        globalFunction("importClass", fn("<K extends keyof MQSClassRegistry>", "MQSClassRegistry[K]", p("name", "K"))),
                        globalFunction("importClass", fn(TypingsConstants.JAVA_CLASS_ANY, p("name", TypingsConstants.STRING))),
                        globalFunction("wrap", fn("<T = JavaInstance>", "T", p("instance", TypingsConstants.UNKNOWN))),
                        globalFunction("extendMapped", fn(TypingsConstants.JAVA_CLASS_ANY, p("config", "MQSExtendMappedConfig"), p("implementation", "Record<string, unknown>"))),
                        globalFunction("exportModule", fn(TypingsConstants.VOID, rest("modules", TypingsConstants.JAVA_CLASS_ANY + " | " + TypingsConstants.JAVA_CLASS_ANY + "[]"))),
                        globalFunction("isInstanceOf", fn(TypingsConstants.BOOLEAN, p("instance", TypingsConstants.UNKNOWN), p("clazz", TypingsConstants.JAVA_CLASS_ANY)))
                ),
                List.of(),
                List.of(
                        describeExtendMappedConfig(),
                        new TsObject("MQSClassRegistry", List.of())
                )
        );
    }

    private static TsObject describeExtendMappedConfig() {
        return new TsObject(
                "MQSExtendMappedConfig",
                List.of(
                        prop(EXTENDS, TypingsConstants.UNKNOWN),
                        optProp(IMPLEMENTS, TypingsConstants.UNKNOWN + " | " + TypingsConstants.UNKNOWN + "[]")
                )
        );
    }

    public static ProxyExecutable createImportClassProxy(ScriptingClassResolver resolver, Context context) {
        return args -> {
            if (args.length == 0 || !args[0].isString()) {
                throw new IllegalArgumentException("importClass requires a class name string argument.");
            }
            String requestedName = args[0].asString().trim();
            if (requestedName.isEmpty()) {
                throw new IllegalArgumentException("importClass requires a non-empty class name.");
            }

            String resolvedName = resolveImportClassName(requestedName, resolver);
            if (!resolver.isClassAllowed(resolvedName)) {
                throw new SecurityException("Class not allowed: " + resolvedName);
            }

            String runtime = resolver.getRuntimeName(resolvedName);
            if (runtime != null) {
                return resolver.getOrCreateWrapper(runtime);
            }
            try {
                Value javaInterop = context.getBindings(ScriptConstants.JS).getMember("Java");
                return javaInterop.invokeMember("type", resolvedName);
            } catch (PolyglotException e) {
                throw new IllegalArgumentException("Unknown class or could not load host class: " + resolvedName, e);
            }
        };
    }

    private static String resolveImportClassName(String requestedName, ScriptingClassResolver resolver) {
        if (requestedName.indexOf('.') >= 0) {
            return requestedName;
        }

        List<String> matches = resolver.findNamedClassesBySimpleName(requestedName);
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Unknown class name '" + requestedName + "'. Use a fully qualified class name.");
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("Ambiguous class name '" + requestedName + "'. Possible matches: " + String.join(", ", matches) + ". Use a fully qualified class name.");
        }
        return matches.getFirst();
    }

    public static ProxyExecutable createWrapProxy(ScriptingClassResolver resolver) {
        return args -> {
            if (args.length != 1) {
                throw new IllegalArgumentException("wrap() requires exactly one argument: the instance to wrap.");
            }
            Value v = args[0];
            if (isAlreadyWrapped(v)) {
                return v;
            }
            return wrapJavaInstance(v, resolver);
        };
    }

    private static boolean isAlreadyWrapped(Value v) {
        if (!v.isProxyObject()) {
            return false;
        }

        Object o = v.asProxyObject();
        return o instanceof ExtendedInstanceProxy
                || o instanceof MappedInstanceProxy
                || o instanceof JsObjectWrapper;
    }

    private static JsObjectWrapper wrapJavaInstance(Value v, ScriptingClassResolver resolver) {
        Object javaInstance = ScriptUtils.unwrapReceiver(v);
        if (javaInstance == null) {
            throw new IllegalArgumentException("The instance passed to wrap() was null or could not be unwrapped to a Java object.");
        }
        Class<?> instanceClass = javaInstance.getClass();
        MappingUtils.ClassMappings cm = MappingUtils.combineMappings(
                instanceClass, resolver.getRuntimeToNamedMap(), resolver.getMethodMap(), resolver.getFieldMap());

        return new JsObjectWrapper(
                javaInstance,
                instanceClass,
                cm.methods(),
                cm.fields(),
                resolver.getMappingsManager(),
                resolver.getScriptManager()
        );
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
                processExportArg(arg, exportsMap);
            }
            return null;
        };
    }

    private static void processExportArg(Value arg, Map<String, Value> exportsMap) {
        if (arg == null) {
            addModule(exportsMap, null);
            return;
        }
        if (!arg.hasArrayElements()) {
            addModule(exportsMap, arg);
            return;
        }
        for (long i = 0; i < arg.getArraySize(); i++) {
            addModule(exportsMap, arg.getArrayElement(i));
        }
    }

    private static Class<?> getClassFromValue(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }

        if (value.isProxyObject()) {
            return getClassFromProxy(value.asProxyObject());
        }

        Object unwrapped = ScriptUtils.unwrapReceiver(value);
        return unwrapped instanceof Class<?> clazz ? clazz : null;
    }

    private static Class<?> getClassFromProxy(Object proxy) {
        return switch (proxy) {
            case LazyJsClassHolder holder -> holder.getWrapper().getTargetClass();
            case JsClassWrapper wrapper -> wrapper.getTargetClass();
            default -> null;
        };
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

        if (extendsValue.isProxyObject() && extendsValue.asProxyObject() instanceof ExtendedInstanceProxy parentProxy) {
            return buildSetupFromParentProxy(configArg, parentProxy, extendsValue, context, resolver);
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
        String namedClassName = resolveNamedClassName(wrapperInfo, resolver);
        JsClassWrapper wrapper = wrapperInfo.wrapper();
        return new MappedClassInfo(namedClassName, wrapper.getTargetClass(), wrapper.getMethodMappings(), wrapper.getFieldMappings());
    }

    private static MappedClassInfo extractInfoFromExtender(Object proxy) {
        if (!(proxy instanceof MappedClassExtender extender)) {
            return null;
        }
        return extender.getConfig().extendsClass();
    }

    private static WrapperInfo extractWrapperInfo(Object proxy) {
        return switch (proxy) {
            case LazyJsClassHolder holder -> new WrapperInfo(holder.getWrapper(), holder.getNamedClassName());
            case JsClassWrapper wrapper -> new WrapperInfo(wrapper, null);
            default -> null;
        };
    }

    private static String resolveNamedClassName(WrapperInfo wrapperInfo, ScriptingClassResolver resolver) {
        if (wrapperInfo.namedClassName() != null) {
            return wrapperInfo.namedClassName();
        }
        String runtimeName = wrapperInfo.wrapper().getTargetClass().getName();
        return resolver.getRuntimeToNamedMap().getOrDefault(runtimeName, runtimeName);
    }

    private static MappedClassInfo extractInfoFromHostClass(Value value, ScriptingClassResolver resolver) {
        if (!value.isHostObject() || !(value.asHostObject() instanceof Class)) {
            return null;
        }
        Class<?> clazz = value.as(Class.class);
        String namedClassName = resolver.getRuntimeToNamedMap().get(clazz.getName());
        if (namedClassName != null) {
            MappingUtils.ClassMappings cm = MappingUtils.combineMappings(clazz, resolver.getRuntimeToNamedMap(), resolver.getMethodMap(), resolver.getFieldMap());
            return new MappedClassInfo(namedClassName, clazz, cm.methods(), cm.fields());
        }
        return new MappedClassInfo(clazz.getName(), clazz, Collections.emptyMap(), Collections.emptyMap());
    }

    private record ExtendMappedArgs(Value configArg, Value implementationArg) {
    }

    private record ExtendMappedSetup(ExtensionConfig config, Value parentOverrides, Value parentAddons,
                                     Value parentSuper) {
    }

    private record WrapperInfo(JsClassWrapper wrapper, String namedClassName) {
    }
}

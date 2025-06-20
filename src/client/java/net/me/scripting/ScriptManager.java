// path: java/net/me/scripting/ScriptManager.java

package net.me.scripting;

import net.me.Main;
import net.me.scripting.config.ExtensionConfig;
import net.me.scripting.config.MappedClassInfo;
import net.me.scripting.extenders.MappedClassExtender;
import net.me.scripting.extenders.proxies.ExtendedInstanceProxy;
import net.me.scripting.extenders.proxies.MappedInstanceProxy;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.module.ScriptDescriptor;
import net.me.scripting.utils.MappingUtils;
import net.me.scripting.utils.ScriptUtils;
import net.me.scripting.wrappers.JsClassWrapper;
import net.me.scripting.wrappers.JsObjectWrapper;
import net.me.scripting.wrappers.LazyJsClassHolder;
import net.me.scripting.wrappers.LazyPackageProxy;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ScriptManager {
    private static ScriptManager instance;
    private final Map<String, JsClassWrapper> wrapperCache = new WeakHashMap<>();
    private final Map<String, ScriptDescriptor> availableScripts = new HashMap<>();
    private final Map<String, RunningScript> runningScripts = new HashMap<>();
    private Context scriptContext;

    // A ThreadLocal to hold modules exported during a single file's evaluation
    private final ThreadLocal<Map<String, Value>> perFileExports = new ThreadLocal<>();

    private Map<String, String> classMap;
    private Map<String, Map<String, List<String>>> methodMap;
    private Map<String, Map<String, String>> fieldMap;
    private Map<String, String> runtimeToYarn;
    private static final Set<String> EXCLUDED = Set.of();
    private Set<String> knownPackagePrefixes;

    private ScriptManager() {
    }

    public static ScriptManager getInstance() {
        if (instance == null) instance = new ScriptManager();
        return instance;
    }

    public void init() {
        ensureScriptDirectory();
        loadMappings();
        this.scriptContext = createDefaultScriptContext();
        discoverScripts();
    }

    public void enableAllScripts() {
        Main.LOGGER.info("Enabling all discovered scripts...");
        for(String scriptId : availableScripts.keySet()){
            enableScript(scriptId);
        }
    }

    public Context createDefaultScriptContext() {
        Main.LOGGER.info("Creating new default script context (ECMAScript 2024)...");
        long startTime = System.currentTimeMillis();
        Context newContext = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(this::isClassAllowed)
                .option("js.ecmascript-version", "2024")
                .option("js.esm-eval-returns-exports", "true")
                .build();
        configureContext(newContext);
        long endTime = System.currentTimeMillis();
        Main.LOGGER.info("New default script context (ECMAScript 2024) created in {}ms.", (endTime - startTime));
        return newContext;
    }

    private void configureContext(Context contextToConfigure) {
        registerPackages(contextToConfigure);
        bindJavaFunctions(contextToConfigure);
        bindImportClass(contextToConfigure);
        bindExtendMapped(contextToConfigure);
        bindWrap(contextToConfigure);
        bindExportModule(contextToConfigure);
    }

    private void bindExportModule(Context contextToConfigure) {
        ProxyExecutable exportFunction = args -> {
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

        contextToConfigure.getBindings("js").putMember("exportModule", exportFunction);
    }

    private void addModule(Map<String, Value> exportsMap, Value moduleValue) {
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

    // --- Unchanged methods from original file ---
    private void bindWrap(Context contextToConfigure) {
        contextToConfigure.getBindings("js").putMember("wrap", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new RuntimeException("wrap() requires exactly one argument: the instance.");
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
                throw new RuntimeException("The instance passed to wrap() was null or could not be unwrapped.");
            }

            Class<?> instanceClass = javaInstance.getClass();
            var cm = MappingUtils.combineMappings(instanceClass, runtimeToYarn, methodMap, fieldMap);
            return new JsObjectWrapper(javaInstance, instanceClass, cm.methods(), cm.fields());
        });
    }

    private void loadMappings() {
        MappingsManager.getInstance().init();
        var mm = MappingsManager.getInstance();
        classMap = mm.getClassMap();
        methodMap = mm.getMethodMap();
        fieldMap = mm.getFieldMap();
        runtimeToYarn = mm.getRuntimeToYarnClassMap();
        precomputePackagePrefixes();
    }

    private void precomputePackagePrefixes() {
        knownPackagePrefixes = new HashSet<>();
        if (classMap == null) return;

        for (String fqcn : classMap.keySet()) {
            String[] parts = fqcn.split("\\.");
            StringBuilder currentPath = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) {
                    currentPath.append('.');
                }
                currentPath.append(parts[i]);
                knownPackagePrefixes.add(currentPath.toString());
            }
        }
    }

    public boolean isFullClassPath(String path) {
        return classMap.containsKey(path);
    }

    public boolean isPackage(String path) {
        return knownPackagePrefixes.contains(path);
    }

    public String getRuntimeName(String yarnName) {
        return classMap.get(yarnName);
    }

    private void registerPackages(Context contextToConfigure) {
        Set<String> topLevelPackages = new HashSet<>();
        if (knownPackagePrefixes != null) {
            for (String prefix : knownPackagePrefixes) {
                if (isClassInMc(prefix)) {
                    topLevelPackages.add(prefix.split("\\.")[0]);
                }
            }
        }

        var bindings = contextToConfigure.getBindings("js");
        for (String pkg : topLevelPackages) {
            if (!bindings.hasMember(pkg)) {
                bindings.putMember(pkg, new LazyPackageProxy(pkg, this));
            }
        }
    }

    protected boolean isClassInMc(String name) {
        return isClassIncluded(name) &&
                (name.startsWith("net.minecraft.") || name.startsWith("com.mojang."));
    }

    protected boolean isClassAllowed(String name) {
        return isClassIncluded(name)
                && (name.startsWith("java.")
                || name.startsWith("net.minecraft.")
                || name.startsWith("com.mojang.")
                || name.startsWith("net.me")
                || name.startsWith("com.oracle.truffle.host.adapters."));
    }

    private boolean isClassIncluded(String name) {
        return !EXCLUDED.contains(name);
    }

    private void bindJavaFunctions(Context contextToConfigure) {
        var bindings = contextToConfigure.getBindings("js");
        bindings.putMember("println", (ProxyExecutable) args -> {
            for (Value arg : args) System.out.println(arg);
            return null;
        });
        bindings.putMember("print", (ProxyExecutable) args -> {
            for (Value arg : args) System.out.print(arg);
            return null;
        });
    }

    private void bindImportClass(Context contextToConfigure) {
        contextToConfigure.getBindings("js").putMember("importClass", (ProxyExecutable) args -> {
            if (args.length == 0 || !args[0].isString())
                throw new RuntimeException("importClass requires Yarn FQCN string");
            var name = args[0].asString();
            if (EXCLUDED.contains(name)) throw new RuntimeException("Class excluded: " + name);
            String runtime = classMap.get(name);
            if (runtime != null) return createWrapper(runtime);
            if (isClassAllowed(name)) {
                try {
                    return contextToConfigure.eval("js", "Java.type('" + name + "')");
                } catch (Exception e) {
                    throw new RuntimeException("Cannot load host class: " + name, e);
                }
            }
            throw new RuntimeException("Unknown class: " + name);
        });
    }


    private void bindExtendMapped(Context contextToConfigure) {
        contextToConfigure.getBindings("js").putMember("extendMapped", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new RuntimeException("extendMapped() requires exactly one configuration object");
            }
            Value configArg = args[0];
            if (!configArg.hasMembers() || !configArg.hasMember("extends")) {
                throw new RuntimeException("Configuration argument must be an object with 'extends' property");
            }

            Value extendsValue = configArg.getMember("extends");
            Value parentOverrides = null;
            Value parentAddons = null;
            Value parentSuper = null;
            ExtensionConfig config;

            if (extendsValue.isProxyObject() && extendsValue.asProxyObject() instanceof MappedClassExtender) {
                config = parseExtensionConfig(configArg, contextToConfigure, extendsValue);

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
                            allImplements.add(extractInfoFromValue(impl.getArrayElement(i)));
                        }
                    } else {
                        allImplements.add(extractInfoFromValue(impl));
                    }
                }
                List<MappedClassInfo> finalImplements = new ArrayList<>(new LinkedHashSet<>(allImplements));
                config = new ExtensionConfig(newExtendsInfo, finalImplements.stream().filter(Objects::nonNull).toList(), contextToConfigure);

            } else {
                config = parseExtensionConfig(configArg, contextToConfigure, extendsValue);
            }

            return new MappedClassExtender(config, contextToConfigure, parentOverrides, parentAddons, parentSuper);
        });
    }

    private ExtensionConfig parseExtensionConfig(Value configArg, Context context, Value extendsValueOverride) {
        Value extendsValue = (extendsValueOverride != null) ? extendsValueOverride : configArg.getMember("extends");

        if (extendsValue == null) {
            throw new RuntimeException("Configuration object must have an 'extends' property");
        }
        MappedClassInfo extendsInfo = extractInfoFromValue(extendsValue);
        List<MappedClassInfo> implementsInfos = new ArrayList<>();
        if (configArg.hasMember("implements")) {
            Value impl = configArg.getMember("implements");
            if (impl.hasArrayElements()) {
                for (long i = 0; i < impl.getArraySize(); i++) {
                    implementsInfos.add(extractInfoFromValue(impl.getArrayElement(i)));
                }
            } else {
                implementsInfos.add(extractInfoFromValue(impl));
            }
        }
        return new ExtensionConfig(extendsInfo, implementsInfos.stream().filter(Objects::nonNull).toList(), context);
    }

    private MappedClassInfo extractInfoFromValue(Value value) {
        if (value.isProxyObject()) {
            Object proxy = value.asProxyObject();
            if (proxy instanceof MappedClassExtender extender) {
                // This allows extending a script class definition.
                try {
                    java.lang.reflect.Field configField = MappedClassExtender.class.getDeclaredField("config");
                    configField.setAccessible(true);
                    ExtensionConfig parentConfig = (ExtensionConfig) configField.get(extender);
                    return parentConfig.extendsClass();
                } catch (Exception e) {
                    throw new RuntimeException("Could not extract config from parent MappedClassExtender", e);
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
                } catch (Exception ignored) {}
            } else if (proxy instanceof JsClassWrapper w) {
                wrapper = w;
            }

            if (wrapper != null) {
                if (yarnName == null) {
                    yarnName = runtimeToYarn.getOrDefault(wrapper.getTargetClass().getName(), wrapper.getTargetClass().getName());
                }
                return new MappedClassInfo(yarnName, wrapper.getTargetClass(), wrapper.getMethodMappings(), wrapper.getFieldMappings());
            }
        } else if (value.isHostObject() && value.asHostObject() instanceof Class) {
            Class<?> clazz = value.as(Class.class);
            String yarnName = runtimeToYarn.get(clazz.getName());
            if (yarnName != null) {
                var cm = MappingUtils.combineMappings(clazz, runtimeToYarn, methodMap, fieldMap);
                return new MappedClassInfo(yarnName, clazz, cm.methods(), cm.fields());
            } else {
                return new MappedClassInfo(clazz.getName(), clazz, Collections.emptyMap(), Collections.emptyMap());
            }
        }
        return null;
    }

    private JsClassWrapper createWrapper(String runtime) {
        return wrapperCache.computeIfAbsent(runtime, r -> {
            try {
                return createActualJsClassWrapper(r);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public JsClassWrapper createActualJsClassWrapper(String runtime) throws ClassNotFoundException {
        Class<?> cls = Class.forName(runtime, false, getClass().getClassLoader());
        var cm = MappingUtils.combineMappings(cls, runtimeToYarn, methodMap, fieldMap);
        return new JsClassWrapper(runtime, cm.methods(), cm.fields());
    }

    private void ensureScriptDirectory() {
        Path p = Main.MOD_DIR.resolve("scripts");
        try {
            if (!Files.exists(p)) Files.createDirectories(p);
        } catch (IOException e) {
            Main.LOGGER.error("Failed create scripts dir: {}", p, e);
        }
    }
    // --- End of unchanged methods ---

    private void discoverScripts() {
        availableScripts.clear();
        Path scriptsDir = Main.MOD_DIR.resolve("scripts");
        try (Stream<Path> paths = Files.walk(scriptsDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".js"))
                    .forEach(this::discoverModulesInFile);
        } catch (IOException e) {
            Main.LOGGER.error("Error discovering scripts in {}", scriptsDir, e);
        }
        Main.LOGGER.info("Discovered {} available script modules.", availableScripts.size());
    }

    private void discoverModulesInFile(Path path) {
        perFileExports.set(new HashMap<>());
        try {
            Source source = Source.newBuilder("js", path.toFile()).mimeType("application/javascript+module").build();
            this.scriptContext.eval(source);

            Map<String, Value> discoveredModules = perFileExports.get();
            for (String moduleName : discoveredModules.keySet()) {
                ScriptDescriptor descriptor = new ScriptDescriptor(path, moduleName);
                availableScripts.put(descriptor.getId(), descriptor);
            }
        } catch (Exception e) {
            Main.LOGGER.error("Failed to parse script file for modules: {}", path, e);
        } finally {
            perFileExports.remove();
        }
    }

    public void enableScript(String scriptId) {
        if (runningScripts.containsKey(scriptId)) {
            Main.LOGGER.warn("Script '{}' is already running.", scriptId);
            return;
        }
        ScriptDescriptor descriptor = availableScripts.get(scriptId);
        if (descriptor == null) {
            Main.LOGGER.error("Cannot enable unknown script '{}'", scriptId);
            return;
        }

        perFileExports.set(new HashMap<>());
        try {
            Source source = Source.newBuilder("js", descriptor.path().toFile())
                    .mimeType("application/javascript+module")
                    .build();

            this.scriptContext.eval(source);
            Map<String, Value> fileExports = perFileExports.get();
            Value scriptClass = fileExports.get(descriptor.moduleName());

            if (scriptClass == null || !scriptClass.canInstantiate()) {
                throw new IllegalStateException("Module '" + descriptor.moduleName() + "' was not found or is not an instantiable class. Did you use exportModule()?");
            }
            Value jsInstance = scriptClass.newInstance();
            RunningScript runningScript = new RunningScript(descriptor, jsInstance);

            runningScripts.put(scriptId, runningScript);
            runningScript.onEnable();
            Main.LOGGER.info("Enabled script: {}", runningScript.getName());
        } catch (Exception e) {
            Main.LOGGER.error("Failed to enable script '{}'", scriptId, e);
        } finally {
            perFileExports.remove();
        }
    }

    public void disableScript(String scriptId) {
        RunningScript script = runningScripts.remove(scriptId);
        if (script != null) {
            script.onDisable();
            Main.LOGGER.info("Disabled script: {}", script.getName());
        }
    }

    public boolean isRunning(String scriptId) {
        return runningScripts.containsKey(scriptId);
    }

    public Collection<ScriptDescriptor> getAvailableScripts() {
        return Collections.unmodifiableCollection(availableScripts.values());
    }

    public Collection<RunningScript> getRunningScripts() {
        return Collections.unmodifiableCollection(runningScripts.values());
    }
}
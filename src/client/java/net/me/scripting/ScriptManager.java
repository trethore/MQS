package net.me.scripting;

import net.fabricmc.loader.api.FabricLoader;
import net.me.Main;
import net.me.mappings.MappingsManager;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyInstantiable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScriptManager {
    private static ScriptManager inst;
    private Context ctx;
    private boolean contextInitialized = false;


    private static final Set<String> EXCLUDED_YARN_CLASSES = Set.of(
            // "net.minecraft.server.ServerLinks",
            // "net.minecraft.network.state.ConfigurationStates"
    );

    private Map<String, String> classMap;
    private Map<String, Map<String, List<String>>> methodMap;
    private Map<String, Map<String, String>> fieldMap;
    private Map<String, String> runtimeToYarnClassMap;

    private ScriptManager() {}

    public static ScriptManager getInstance() {
        if (inst == null) inst = new ScriptManager();
        return inst;
    }

    public void init() {
        createDir();
        initializeContext();
    }

    private synchronized void initializeContext() {
        if (contextInitialized) return;
        ctx = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(name -> name.startsWith("java.") || name.startsWith("net.me."))
                .option("js.ecmascript-version", "2021")
                .build();

        var mm = MappingsManager.getInstance();
        this.classMap = mm.getClassMap();
        this.methodMap = mm.getMethodMap();
        this.fieldMap = mm.getFieldMap();
        this.runtimeToYarnClassMap = mm.getRuntimeToYarnClassMap();

        JsPackage globalPackageScope = new JsPackage();

        this.classMap.keySet().stream()
                .filter(yarnName -> !EXCLUDED_YARN_CLASSES.contains(yarnName))
                .filter(yarnName -> yarnName.startsWith("net.minecraft.") || yarnName.startsWith("com.mojang."))
                .forEach(yarnName -> {
                    String runtimeName = this.classMap.get(yarnName);
                    if (runtimeName == null) {
                        Main.LOGGER.warn("No runtime name found for yarn class: {}", yarnName);
                        return;
                    }
                    LazyJsClassHolder holder = new LazyJsClassHolder(yarnName, runtimeName, this);
                    ScriptUtils.insertIntoPackageHierarchy(globalPackageScope, yarnName, holder);
                });

        var jsBindings = ctx.getBindings("js");
        for (String topLevelPackageName : (String[]) globalPackageScope.getMemberKeys()) {
            jsBindings.putMember(topLevelPackageName, globalPackageScope.getMember(topLevelPackageName));
        }

        exposeStandardJavaTypes();
        exposeImportClassFunction();
        exposeExtendsFromFunction();

        contextInitialized = true;
        Main.LOGGER.info("Scripting context initialized with lazy class holders.");
    }



    public JsClassWrapper createActualJsClassWrapper(String runtimeName) throws ClassNotFoundException {
        if (this.runtimeToYarnClassMap == null || this.methodMap == null || this.fieldMap == null) {
            throw new IllegalStateException("Mappings not available for creating JsClassWrapper.");
        }
        Class<?> cls = Class.forName(runtimeName, false, ScriptManager.class.getClassLoader());
        ScriptUtils.ClassMappings combinedMappings = ScriptUtils.combineMappingsForClassAndSuperclasses(cls, this.runtimeToYarnClassMap, this.methodMap, this.fieldMap);
        return new JsClassWrapper(runtimeName, combinedMappings.methods(), combinedMappings.fields());
    }


    private void createDir() {
        Path path = FabricLoader.getInstance().
                getGameDir().resolve(Main.MOD_ID).resolve("scripts");
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                Main.LOGGER.info("Created scripts directory: {}", path);
            }
        } catch (IOException e) {
            Main.LOGGER.error("Failed to create scripts directory: {}", path, e);
        }
    }




    public Value run(String source) {
        if (!contextInitialized) {
            Main.LOGGER.error("ScriptManager context not initialized. Cannot run script.");
            throw new IllegalStateException("ScriptManager context not initialized.");
        }
        try {
            return ctx.eval("js", source);
        } catch (PolyglotException e) {
            Main.LOGGER.error("Error executing script: {}", e.getMessage());
            if (e.isGuestException()) {
                Value guestException = e.getGuestObject();
                if (guestException != null && guestException.hasMember("stack")) {
                    Main.LOGGER.error("JS Stacktrace: {}", guestException.getMember("stack").asString());
                }
            }
            throw e;
        }
    }

    private void exposeStandardJavaTypes() {
        try {
            Value systemClass = ctx.eval("js", "Java.type('java.lang.System')");
            Value threadClass = ctx.eval("js", "Java.type('java.lang.Thread')");
            ctx.getBindings("js").putMember("java.lang.System", systemClass);
            ctx.getBindings("js").putMember("java.lang.Thread", threadClass);

        } catch (PolyglotException e) {
            Main.LOGGER.error("Couldn’t expose standard Java types like System/Thread via Java.type() for pre-binding", e);
        }
    }

    private void exposeImportClassFunction() {
        ctx.getBindings("js").putMember("importClass", (ProxyExecutable) args -> {
            if (args.length == 0 || !args[0].isString()) {
                throw new RuntimeException("importClass requires a String argument (Yarn FQCN)");
            }
            String yarnName = args[0].asString();

            if (EXCLUDED_YARN_CLASSES.contains(yarnName)) {
                throw new RuntimeException("Class " + yarnName + " is excluded from wrapping.");
            }

            String runtimeName = this.classMap.get(yarnName);
            if (runtimeName == null) {
                throw new RuntimeException("Class not found in mappings (or not a known mapped class): " + yarnName);
            }
            try {
                return createActualJsClassWrapper(runtimeName);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException("Runtime class not found for " + yarnName + " (mapped to " + runtimeName + ")", ex);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create class wrapper for " + yarnName + ": " + e.getMessage(), e);
            }
        });
    }
    private void exposeExtendsFromFunction() {
        ctx.getBindings("js").putMember("extendsFrom", (ProxyExecutable) args -> {
            // ... (argument checking and baseClassWrapper setup remains the same)
            if (args.length != 2) {
                throw new RuntimeException("extendsFrom(BaseClassWrapper, OverridesObject) requires 2 arguments.");
            }
            Value baseClassWrapperValue = args[0];
            Value overridesValue = args[1];

            if (!baseClassWrapperValue.isProxyObject()) {
                throw new RuntimeException("First argument to extendsFrom() must be a wrapped Java class (e.g., net.minecraft.util.math.BlockPos).");
            }
            // No strict check needed for overridesValue here, JS will handle it.

            Object baseProxy = baseClassWrapperValue.asProxyObject();
            JsClassWrapper actualBaseClassWrapper;

            if (baseProxy instanceof LazyJsClassHolder) {
                actualBaseClassWrapper = ((LazyJsClassHolder) baseProxy).resolve();
            } else if (baseProxy instanceof JsClassWrapper) {
                actualBaseClassWrapper = (JsClassWrapper) baseProxy;
            } else {
                throw new RuntimeException("First argument to extendsFrom() must be a JsClassWrapper or LazyJsClassHolder.");
            }

            Object classMemberObject = actualBaseClassWrapper.getMember("_class");
            if (!(classMemberObject instanceof Class<?> javaBaseClass)) { // Using pattern variable
                String typeInfo = (classMemberObject == null) ? "null" : classMemberObject.getClass().getName();
                throw new RuntimeException("Could not retrieve underlying Java Class from the base class wrapper. Member _class was: " + typeInfo);
            }

            ScriptUtils.ClassMappings baseMappings = ScriptUtils.combineMappingsForClassAndSuperclasses(
                    javaBaseClass,
                    MappingsManager.getInstance().getRuntimeToYarnClassMap(),
                    MappingsManager.getInstance().getMethodMap(),
                    MappingsManager.getInstance().getFieldMap()
            );

            // Return an object that implements ProxyInstantiable
            // This object IS the constructor function for the JS side.
            return new ProxyInstantiable() {
                @Override
                public Object newInstance(Value... constructorArgs) {
                    Object newMemberObject = actualBaseClassWrapper.getMember("new");
                    if (!(newMemberObject instanceof ProxyExecutable)) {
                        throw new RuntimeException("Base class wrapper 'new' member is not a ProxyExecutable.");
                    }
                    ProxyExecutable newProxyExecutable = (ProxyExecutable) newMemberObject;
                    Object javaInstanceResultObject;
                    try {
                        javaInstanceResultObject = newProxyExecutable.execute(constructorArgs);
                    } catch (PolyglotException e) {
                        throw new RuntimeException("Error constructing base Java class '" + javaBaseClass.getName() + "': " + e.getMessage(), e);
                    }

                    Object rawJavaInstance;
                    Value javaInstanceWrapperValue = Value.asValue(javaInstanceResultObject);

                    if (javaInstanceWrapperValue.isProxyObject()) {
                        Object proxiedInstance = javaInstanceWrapperValue.asProxyObject();
                        if (proxiedInstance instanceof JsObjectWrapper) {
                            rawJavaInstance = ((JsObjectWrapper) proxiedInstance).getJavaInstance();
                        } else if (proxiedInstance instanceof JsExtendedObjectWrapper) {
                            rawJavaInstance = ((JsExtendedObjectWrapper) proxiedInstance).getJavaInstance();
                        } else {
                            throw new RuntimeException("Constructor of base class returned an unexpected proxy object type: " + proxiedInstance.getClass().getName());
                        }
                    } else if (javaInstanceWrapperValue.isHostObject()) {
                        rawJavaInstance = javaInstanceWrapperValue.asHostObject();
                    } else {
                        throw new RuntimeException("Constructor of base class did not return a recognizable Java instance wrapper (not a Proxy or Host object). Returned: " + javaInstanceWrapperValue);
                    }

                    return new JsExtendedObjectWrapper(
                            rawJavaInstance,
                            javaBaseClass,
                            baseMappings.methods(),
                            baseMappings.fields(),
                            overridesValue
                    );
                }
            };
        });
    }
}
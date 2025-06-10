package net.me.scripting;

import net.me.Main;
import net.me.scripting.mappings.MappingsManager;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ScriptManager {
    private static ScriptManager instance;
    private final Map<String, JsClassWrapper> wrapperCache = new WeakHashMap<>();
    private final Map<String, Script> scripts = new HashMap<>();

    private Map<String, String> classMap;
    private Map<String, Map<String, List<String>>> methodMap;
    private Map<String, Map<String, String>> fieldMap;
    private Map<String, String> runtimeToYarn;

    private static final Set<String> EXCLUDED = Set.of();

    private ScriptManager() {}

    public static ScriptManager getInstance() {
        if (instance == null) instance = new ScriptManager();
        return instance;
    }

    public void init() {
        ensureScriptDirectory();
        loadMappings();
    }

    public Context createDefaultScriptContext() {
        Main.LOGGER.info("Creating new default script context (ECMAScript 2024)...");
        long startTime = System.currentTimeMillis();
        Context newContext = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(this::isClassAllowed)
                .option("js.ecmascript-version", "2024")
                .build();

        configureContext(newContext);

        long endTime = System.currentTimeMillis();
        Main.LOGGER.info("New default script context (ECMAScript 2024) created in {}ms.", (endTime - startTime));
        return newContext;
    }

    private void configureContext(Context contextToConfigure) {
        registerPackages(contextToConfigure);
        bindJavaTypes(contextToConfigure);
        bindImportClass(contextToConfigure);
        bindExtendMapped(contextToConfigure);
        bindThisOf(contextToConfigure);
        bindSuperOf(contextToConfigure);
    }

    private void bindSuperOf(Context contextToConfigure) {
        contextToConfigure.getBindings("js").putMember("superOf", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new RuntimeException("superOf() requires exactly one argument: the instance.");
            }

            Object javaInstance = ScriptUtils.unwrapReceiver(args[0]);

            if (javaInstance == null) {
                throw new RuntimeException("The instance passed to superOf() was null or could not be unwrapped.");
            }

            Class<?> superClass = javaInstance.getClass().getSuperclass();
            if (superClass == null) {
                throw new RuntimeException("Instance " + javaInstance + " does not have a superclass.");
            }

            var cm = ScriptUtils.combineMappings(superClass, runtimeToYarn, methodMap, fieldMap);

            return new JsSuperObjectWrapper(javaInstance, cm.methods(), contextToConfigure);
        });
    }

    private void bindThisOf(Context contextToConfigure) {
        contextToConfigure.getBindings("js").putMember("thisOf", (ProxyExecutable) args -> {
            if (args.length != 1) {
                throw new RuntimeException("thisOf() requires exactly one argument: the instance.");
            }

            Object javaInstance = ScriptUtils.unwrapReceiver(args[0]);

            if (javaInstance == null) {
                throw new RuntimeException("The instance passed to thisOf() was null or could not be unwrapped.");
            }

            Class<?> instanceClass = javaInstance.getClass();
            var cm = ScriptUtils.combineMappings(instanceClass, runtimeToYarn, methodMap, fieldMap);

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
    }

    private void registerPackages(Context contextToConfigure) {
        JsPackage root = new JsPackage();
        classMap.entrySet().stream()
                .filter(e -> !EXCLUDED.contains(e.getKey()))
                .filter(e -> isClassInMc(e.getKey()))
                .forEach(e -> {
                    var holder = new LazyJsClassHolder(e.getKey(), e.getValue(), this);
                    ScriptUtils.insertIntoPackageHierarchy(root, e.getKey(), holder);
                });

        var bindings = contextToConfigure.getBindings("js");

        Arrays.stream((String[]) root.getMemberKeys())
                .forEach(key -> bindings.putMember(key, root.getMember(key)));
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
                || name.startsWith("net.me"));
    }
    private boolean isClassIncluded(String name) {
        return !EXCLUDED.contains(name);
    }

    private void bindJavaTypes(Context contextToConfigure) {
        try {
            Value sys = contextToConfigure.eval("js", "Java.type('java.lang.System')");
            Value thr = contextToConfigure.eval("js", "Java.type('java.lang.Thread')");
            var b = contextToConfigure.getBindings("js");
            b.putMember("java.lang.System", sys);
            b.putMember("java.lang.Thread", thr);
        } catch (PolyglotException e) {
            Main.LOGGER.error("Failed exposing standard Java types", e);
        }
    }

    private void bindImportClass(Context contextToConfigure) {
        contextToConfigure.getBindings("js").putMember("importClass", (ProxyExecutable) args -> {
            if (args.length == 0 || !args[0].isString())
                throw new RuntimeException("importClass requires Yarn FQCN string");
            var name = args[0].asString();
            if (EXCLUDED.contains(name))
                throw new RuntimeException("Class excluded: " + name);
            String runtime = classMap.get(name);
            if (runtime != null) {
                return createWrapper(runtime);
            }
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
        Value javaObj = contextToConfigure.eval("js", "Java");
        javaObj.putMember("extendMapped", (ProxyExecutable) args -> {
            JsClassWrapper wrapper = getJsClassWrapper(args);

            if (wrapper == null) {
                throw new RuntimeException("First argument must be a JsClassWrapper");
            }
            Class<?> rawClass = wrapper.getTargetClass();
            Value hostClassValue = contextToConfigure.asValue(rawClass);

            Value extendFn = javaObj.getMember("extend");
            Value baseAdapterConstructor = extendFn.execute(hostClassValue);

            return new MappedClassExtender(baseAdapterConstructor, wrapper);
        });
    }

    private static @Nullable JsClassWrapper getJsClassWrapper(Value[] args) {
        if (args.length != 1) {
            throw new RuntimeException("extendMapped needs exactly one argument: the JsClassWrapper");
        }
        Value wrapperVal = args[0];
        JsClassWrapper wrapper;
        Object obj = wrapperVal.asProxyObject();
        if (obj instanceof LazyJsClassHolder holder) {
            wrapper = holder.getWrapper();
        } else if (obj instanceof JsClassWrapper w) {
            wrapper = w;
        } else {
            wrapper = null;
        }
        return wrapper;
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
        var cm = ScriptUtils.combineMappings(cls, runtimeToYarn, methodMap, fieldMap);
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

    public void addScript(Script script) {
        scripts.put(script.getName(), script);
    }

    public Script getScript(String name) {
        return scripts.get(name);
    }

    public void removeScript(String name) {
        scripts.remove(name);
    }

    public Collection<Script> getAllScripts() {
        return scripts.values();
    }
}

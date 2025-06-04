package net.me.scripting;

import net.me.Main;
import net.me.scripting.mappings.MappingsManager;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

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

        classMap.forEach((k, v) -> Main.LOGGER.info("Loaded: {} -> {}", k, v));
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
        bindExtendsOf(contextToConfigure);
    }



    private void bindExtendsOf(Context contextToConfigure) {
        contextToConfigure.getBindings("js").putMember("extendsOf", (ProxyExecutable) args -> {
            List<Object> javaExtendArgs = new ArrayList<>();
            Value implementation = null;
            int lastArgIndex = args.length - 1;
            if (args.length > 0) {
                Value potentialImpl = args[lastArgIndex];
                if (potentialImpl.hasMembers() && !potentialImpl.isHostObject() && !potentialImpl.isProxyObject()) {
                    implementation = potentialImpl;
                } else if (potentialImpl.canExecute() && potentialImpl.getMetaObject().canExecute()) {
                    implementation = potentialImpl;
                } else if (potentialImpl.isHostObject() && !(potentialImpl.asHostObject() instanceof Class)) {
                    implementation = potentialImpl;
                }
            }

            int numTypeArgs = (implementation != null) ? args.length - 1 : args.length;

            for (int i = 0; i < numTypeArgs; i++) {
                Value arg = args[i];
                Class<?> javaClass = null;

                if (arg.isString()) {
                    String yarnFqn = arg.asString();
                    String runtimeFqn = classMap.get(yarnFqn);
                    if (runtimeFqn == null) {
                        runtimeFqn = yarnFqn;
                    }

                    if (!isClassAllowed(runtimeFqn) && !isClassInMc(runtimeFqn)) {
                        throw new RuntimeException("Class '" + yarnFqn + "' (runtime: " + runtimeFqn + ") is not allowed for extension or implementation.");
                    }
                    try {
                        javaClass = Class.forName(runtimeFqn, false, getClass().getClassLoader());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to find class '" + yarnFqn + "' (runtime: " + runtimeFqn + ")", e);
                    }
                } else if (arg.isProxyObject()) {
                    Object proxy = arg.asProxyObject();
                    if (proxy instanceof LazyJsClassHolder holder) {
                        javaClass = holder.getTargetClass();
                    } else if (proxy instanceof JsClassWrapper wrapper) {
                        javaClass = wrapper.getTargetClass();
                    } else {
                        throw new IllegalArgumentException("Unsupported proxy object as class type: " + proxy.getClass().getName());
                    }
                } else if (arg.isHostObject()) {
                    Object hostObj = arg.asHostObject();
                    if (hostObj instanceof Class<?> cls) {
                        javaClass = cls;
                    } else {
                        throw new IllegalArgumentException("Unsupported host object as class type: " + hostObj.getClass().getName());
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported argument type for extendsOf. Expected string FQN, JsClassWrapper, LazyJsClassHolder, or raw Class<?>. Got: " + arg.getMetaObject().getMetaQualifiedName());
                }

                if (javaClass != null) {
                    javaExtendArgs.add(javaClass);
                }
            }

            if (javaExtendArgs.isEmpty() && implementation == null) {
                throw new IllegalArgumentException("extendsOf requires at least one class/interface or an implementation object.");
            }

            if (implementation != null) {
                javaExtendArgs.add(implementation);
            }

            Value javaExtendFunction = contextToConfigure.getBindings("js").getMember("Java").getMember("extend");
            Value[] finalArgs = javaExtendArgs.stream().map(Value::asValue).toArray(Value[]::new);

            return javaExtendFunction.execute(finalArgs);
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
        return isClassIncluded(name) &&
                (name.startsWith("java.") || name.startsWith("net.me"));
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

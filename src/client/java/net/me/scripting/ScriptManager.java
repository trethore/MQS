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
import org.graalvm.polyglot.proxy.ProxyObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ScriptManager {
    private static ScriptManager instance;
    private Context context;
    private boolean initialized;

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
        initializeContext();
    }

    private synchronized void initializeContext() {
        if (initialized) return;
        context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(name -> name.startsWith("java.") || name.startsWith("net.me."))
                .option("js.ecmascript-version", "2021")
                .build();

        loadMappings();
        registerPackages();
        bindJavaTypes();
        bindImportClass();
        bindExtendsFrom();

        initialized = true;
        Main.LOGGER.info("Scripting context initialized with lazy class holders.");
    }

    private void loadMappings() {
        var mm = MappingsManager.getInstance();
        classMap = mm.getClassMap();
        methodMap = mm.getMethodMap();
        fieldMap = mm.getFieldMap();
        runtimeToYarn = mm.getRuntimeToYarnClassMap();
    }

    private void registerPackages() {
        JsPackage root = new JsPackage();
        classMap.entrySet().stream()
                .filter(e -> !EXCLUDED.contains(e.getKey()))
                .filter(e -> e.getKey().startsWith("net.minecraft.") ||
                        e.getKey().startsWith("com.mojang."))
                .forEach(e -> {
                    var holder = new LazyJsClassHolder(e.getKey(), e.getValue(), this);
                    ScriptUtils.insertIntoPackageHierarchy(root, e.getKey(), holder);
                });

        var bindings = context.getBindings("js");

        // root.getMemberKeys() returns a String[]
        Arrays.stream((String[]) root.getMemberKeys())
                .forEach(key -> bindings.putMember(key, root.getMember(key)));
    }
    private void bindJavaTypes() {
        try {
            Value sys = context.eval("js", "Java.type('java.lang.System')");
            Value thr = context.eval("js", "Java.type('java.lang.Thread')");
            var b = context.getBindings("js");
            b.putMember("java.lang.System", sys);
            b.putMember("java.lang.Thread", thr);
        } catch (PolyglotException e) {
            Main.LOGGER.error("Failed exposing standard Java types", e);
        }
    }

    private void bindImportClass() {
        context.getBindings("js").putMember("importClass", (ProxyExecutable) args -> {
            if (args.length == 0 || !args[0].isString())
                throw new RuntimeException("importClass requires Yarn FQCN string");
            var yarn = args[0].asString();
            if (EXCLUDED.contains(yarn))
                throw new RuntimeException("Class excluded: " + yarn);
            var runtime = classMap.get(yarn);
            if (runtime == null)
                throw new RuntimeException("Unknown class: " + yarn);
            return createWrapper(runtime);
        });
    }

    private void bindExtendsFrom() {
        context.getBindings("js").putMember("extendsFrom", (ProxyExecutable) args -> {
            if (args.length < 2) {
                throw new RuntimeException("extendsFrom requires at least one base and an overrides object");
            }

            // 1) Last arg is the overrides object
            Value overrides = args[args.length - 1];

            // 2) Resolve all base JsClassWrappers + their mappings
            List<JsClassWrapper>    wrappers     = new ArrayList<>();
            List<ScriptUtils.ClassMappings> mappingsList = new ArrayList<>();

            for (int i = 0; i < args.length - 1; i++) {
                Value v = args[i];
                JsClassWrapper wrap;

                // a) Java.type(...) → HostObject(Class<?>)
                if (v.isHostObject()) {
                    Object ho = v.asHostObject();
                    if (!(ho instanceof Class<?> cls)) {
                        throw new RuntimeException("Unsupported host object as base: " + ho);
                    }
                    try {
                        wrap = createActualJsClassWrapper(cls.getName());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                // b) LazyJsClassHolder or JsClassWrapper
                else if (v.isProxyObject()) {
                    ProxyObject po = v.asProxyObject();
                    if      (po instanceof LazyJsClassHolder lj) wrap = lj.getWrapper();
                    else if (po instanceof JsClassWrapper    cw) wrap = cw;
                    else throw new RuntimeException("Unsupported proxy as base: " + po.getClass().getSimpleName());
                }
                else {
                    throw new RuntimeException("Invalid base for extendsFrom(): " + v);
                }

                wrappers.add(wrap);
                Class<?> javaBase = (Class<?>) wrap.getMember("_class");
                mappingsList.add(
                        ScriptUtils.combineMappings(javaBase, runtimeToYarn, methodMap, fieldMap)
                );
            }

            // 3) If only one base, return the old single-super wrapper:
            if (wrappers.size() == 1) {
                JsClassWrapper onlyWrap = wrappers.getFirst();
                ScriptUtils.ClassMappings cm = mappingsList.getFirst();

                return (ProxyInstantiable) ctorArgs -> {
                    Object inst = onlyWrap.newInstance(ctorArgs);
                    Object javaInst = (inst instanceof JsObjectWrapper jw)
                            ? jw.getJavaInstance()
                            : inst;

                    // _super will be a single SuperAccessWrapper:
                    return new JsExtendedObjectWrapper(
                            javaInst,
                            javaInst.getClass(),
                            cm.methods(),
                            cm.fields(),
                            overrides
                    );
                };
            }

            // 4) Multiple bases → return multi-super wrapper
            return (ProxyInstantiable) ctorArgs -> {
                Object inst = wrappers.getFirst().newInstance(ctorArgs);
                Object javaInst = (inst instanceof JsObjectWrapper jw)
                        ? jw.getJavaInstance()
                        : inst;

                // _super will be an array of SuperAccessWrappers:
                return new MultiExtendedObjectWrapper(javaInst, mappingsList, overrides);
            };
        });
    }

    private JsClassWrapper createWrapper(String runtime) {
        try {
            return createActualJsClassWrapper(runtime);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JsClassWrapper createActualJsClassWrapper(String runtime) throws ClassNotFoundException {
        Class<?> cls = Class.forName(runtime, false, getClass().getClassLoader());
        var cm = ScriptUtils.combineMappings(cls, runtimeToYarn, methodMap, fieldMap);
        return new JsClassWrapper(runtime, cm.methods(), cm.fields());
    }

    public Value run(String src) {
        if (!initialized) throw new IllegalStateException("Context not initialized");
        try { return context.eval("js", src); }
        catch (PolyglotException e) {
            handleException(e);
            throw e;
        }
    }

    private void handleException(PolyglotException e) {
        Main.LOGGER.error("Script error: {}", e.getMessage());
        if (e.isGuestException()) {
            Value ge = e.getGuestObject();
            if (ge != null && ge.hasMember("stack"))
                Main.LOGGER.error("JS Stack: {}", ge.getMember("stack").asString());
        }
    }

    private void ensureScriptDirectory() {
        Path p = FabricLoader.getInstance().getGameDir().resolve(Main.MOD_ID).resolve("scripts");
        try {
            if (!Files.exists(p)) Files.createDirectories(p);
        } catch (IOException e) {
            Main.LOGGER.error("Failed create scripts dir: {}", p, e);
        }
    }
}

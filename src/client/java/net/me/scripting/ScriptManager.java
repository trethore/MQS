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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            if (args.length != 2)
                throw new RuntimeException("extendsFrom(Base, Overrides) requires 2 args");
            Value base = args[0];
            Value overrides = args[1];
            LazyJsClassHolder lazy = base.asProxyObject() instanceof LazyJsClassHolder lj ? lj : null;
            JsClassWrapper wrapper = lazy != null ? lazy.getWrapper(): base.asProxyObject() instanceof JsClassWrapper cw ? cw : null;
            if (wrapper == null)
                throw new RuntimeException("First arg must be a class wrapper");
            Class<?> javaBase = (Class<?>) wrapper.getMember("_class");
            var cm = ScriptUtils.combineMappings(javaBase, runtimeToYarn, methodMap, fieldMap);
            return (ProxyInstantiable) ctorArgs -> instantiateExtended(wrapper, cm, overrides, ctorArgs);
        });
    }

    private Object instantiateExtended(
            JsClassWrapper base,
            ScriptUtils.ClassMappings cm,
            Value overrides,
            Value[] args
    ) {
        // 1) Invoke the “new” on your class wrapper, which currently returns a JsObjectWrapper proxy…
        Object wrapped = base.newInstance(args);

        // 2) …unwrap it to the raw Java instance if necessary
        Object javaInstance;
        if (wrapped instanceof net.me.scripting.JsObjectWrapper jw) {
            javaInstance = jw.getJavaInstance();
        } else {
            javaInstance = wrapped;
        }

        // 3) Build and return your extended‐object proxy around the real Java instance
        Class<?> cls = (Class<?>) base.getMember("_class");
        return new net.me.scripting.JsExtendedObjectWrapper(
                javaInstance,
                cls,
                cm.methods(),
                cm.fields(),
                overrides
        );
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

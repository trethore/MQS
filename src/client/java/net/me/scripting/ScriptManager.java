package net.me.scripting;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScriptManager {
    private static ScriptManager instance;
    private final Map<String, JsClassWrapper> wrapperCache = new WeakHashMap<>();
    private Context context;
    private volatile boolean contextInitialized = false; // Changed from 'initialized' and made volatile

    private final ExecutorService scriptExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ScriptManager-Executor");
        t.setDaemon(true);
        return t;
    });

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
    }

    private synchronized void ensureContextInitialized() {
        if (contextInitialized) return;
        Main.LOGGER.info("Initializing GraalVM context for ScriptManager...");
        long startTime = System.currentTimeMillis();

        context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(this::isClassAllowed)
                .option("js.ecmascript-version", "2021")
                .build();

        loadMappings(); // Depends on MappingsManager being initialized
        registerPackages();
        bindJavaTypes();
        bindImportClass();
        bindExtendsFrom();

        contextInitialized = true;
        long endTime = System.currentTimeMillis();
        Main.LOGGER.info("Scripting context initialized in {}ms with lazy class holders.", (endTime - startTime));
    }

    private void loadMappings() {
        MappingsManager.getInstance().init();
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
                .filter(e -> isClassInMc(e.getKey()))
                .forEach(e -> {
                    var holder = new LazyJsClassHolder(e.getKey(), e.getValue(), this);
                    ScriptUtils.insertIntoPackageHierarchy(root, e.getKey(), holder);
                });

        var bindings = context.getBindings("js");

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
            var name = args[0].asString();
            if (EXCLUDED.contains(name))
                throw new RuntimeException("Class excluded: " + name);
            String runtime = classMap.get(name);
            if (runtime != null) {
                return createWrapper(runtime);
            }
            if (isClassAllowed(name)) {
                try {
                    return context.eval("js", "Java.type('" + name + "')");
                } catch (Exception e) {
                    throw new RuntimeException("Cannot load host class: " + name, e);
                }
            }
            throw new RuntimeException("Unknown class: " + name);
        });
    }

    private void bindExtendsFrom() {
        context.getBindings("js").putMember("extendsFrom", (ProxyExecutable) args -> {
            if (args.length < 2) {
                throw new RuntimeException("extendsFrom requires at least one base and an overrides object");
            }
            Value overrides = args[args.length - 1];

            List<JsClassWrapper>    wrappers     = new ArrayList<>();
            List<ScriptUtils.ClassMappings> mappingsList = new ArrayList<>();

            for (int i = 0; i < args.length - 1; i++) {
                Value v = args[i];
                JsClassWrapper wrap;

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

            if (wrappers.size() == 1) {
                JsClassWrapper onlyWrap = wrappers.getFirst();
                ScriptUtils.ClassMappings cm = mappingsList.getFirst();

                return (ProxyInstantiable) ctorArgs -> {
                    Object inst = onlyWrap.newInstance(ctorArgs);
                    Object javaInst = (inst instanceof JsObjectWrapper jw)
                            ? jw.getJavaInstance()
                            : inst;

                    return new JsExtendedObjectWrapper(
                            javaInst,
                            javaInst.getClass(),
                            cm.methods(),
                            cm.fields(),
                            overrides
                    );
                };
            }

            return (ProxyInstantiable) ctorArgs -> {
                Object inst = wrappers.getFirst().newInstance(ctorArgs);
                Object javaInst = (inst instanceof JsObjectWrapper jw)
                        ? jw.getJavaInstance()
                        : inst;

                return new MultiExtendedObjectWrapper(javaInst, mappingsList, overrides);
            };
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

    private Value runSync(String src) {
        try {
            return context.eval("js", src);
        } catch (PolyglotException e) {
            handlePolyglotException(e);
            throw e;
        } catch (Exception e) {
            Main.LOGGER.error("A non-Polyglot exception occurred during script execution:", e);
            throw new RuntimeException("Script execution failed with an unexpected error.", e);
        }
    }

    public CompletableFuture<Value> run(String src) {
        return CompletableFuture.supplyAsync(() -> {
            ensureContextInitialized();
            return runSync(src);
        }, scriptExecutor).exceptionally(ex -> {
            if (ex.getCause() instanceof PolyglotException polyglotException) {
                Main.LOGGER.error("Async script execution failed due to PolyglotException: {}", polyglotException.getMessage());
                throw polyglotException;
            } else {
                Main.LOGGER.error("Async script execution failed with an unexpected error", ex);
                throw new RuntimeException("Async script execution failed.", ex);
            }
        });
    }


    private void handlePolyglotException(PolyglotException e) {
        StringBuilder msg = new StringBuilder("Script error: ");

        if (e.isHostException()) {
            Throwable hostException = e.asHostException();
            msg.append("HostException: ").append(hostException.getMessage());
        } else {
            msg.append(e.getMessage());
        }

        if (e.getSourceLocation() != null) {
            msg.append(" at ").append(e.getSourceLocation().toString());
        }

        if (e.isGuestException()) {
            Value ge = e.getGuestObject();
            if (ge != null && ge.hasMember("stack")) {
                msg.append("\nJS Stack:\n").append(ge.getMember("stack").asString());
            }
        }
        Main.LOGGER.error(msg.toString());
    }

    private void ensureScriptDirectory() {
        Path p = Main.MOD_DIR.resolve("scripts");
        try {
            if (!Files.exists(p)) Files.createDirectories(p);
        } catch (IOException e) {
            Main.LOGGER.error("Failed create scripts dir: {}", p, e);
        }
    }
}

package net.me.scripting;

import net.me.Main;
import net.me.scripting.mappings.MappingsManager;
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
        bindExtendsFrom(contextToConfigure);
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

    private void bindExtendsFrom(Context contextToConfigure) {
        contextToConfigure.getBindings("js").putMember("extendsFrom", (ProxyExecutable) args -> {
            if (args.length == 0) {
                throw new RuntimeException("extendsFrom requires at least one base class or interface.");
            }

            // Le dernier argument est l'objet des surcharges (overrides)
            // Tous les arguments précédents sont les classes/interfaces à étendre/implémenter.
            Value overrides = null;
            int baseTypesEndIndex = args.length;

            if (args[args.length - 1].hasMembers() && !isPotentialJavaType(args[args.length - 1])) {
                overrides = args[args.length - 1];
                baseTypesEndIndex = args.length - 1;
            } else {
                // Pas d'objet d'overrides explicite, ou le dernier est un type
                try {
                    overrides = contextToConfigure.eval("js", "({})"); // Objet vide pour les surcharges
                } catch (PolyglotException e) { // Should not happen for "({})"
                    throw new RuntimeException("Failed to create empty overrides object", e);
                }
            }

            if (baseTypesEndIndex == 0) {
                throw new RuntimeException("extendsFrom requires at least one base class or interface before the overrides object.");
            }

            Value[] baseTypesValues = new Value[baseTypesEndIndex];
            for (int i = 0; i < baseTypesEndIndex; i++) {
                Value baseArg = args[i];
                Object javaType;
                if (baseArg.isHostObject() && baseArg.asHostObject() instanceof Class) {
                    javaType = baseArg.asHostObject();
                } else if (baseArg.isProxyObject()) {
                    ProxyObject po = baseArg.asProxyObject();
                    if (po instanceof LazyJsClassHolder lj) javaType = lj.getWrapper().getMember("_class");
                    else if (po instanceof JsClassWrapper cw) javaType = cw.getMember("_class");
                    else throw new RuntimeException("Unsupported proxy as base for extendsFrom(): " + po.getClass().getSimpleName());
                } else {
                    throw new RuntimeException("Invalid base type for extendsFrom(): " + baseArg + ". Expected host class or JS class wrapper.");
                }
                if (!(javaType instanceof Class)) {
                    throw new RuntimeException("Base for extendsFrom is not a class: " + javaType);
                }
                baseTypesValues[i] = Value.asValue(javaType); // Value.asValue pour passer des objets host comme arguments
            }

            Main.LOGGER.debug("ExtendsFrom: Attempting to extend {} with JS overrides.", (Object)baseTypesValues);

            Value javaExtendFunc = contextToConfigure.getBindings("js").getMember("Java").getMember("extend");
            if (javaExtendFunc == null || !javaExtendFunc.canExecute()) {
                throw new RuntimeException("Java.extend is not available in the JS context. Is HostClassLookup allowed and working?");
            }

            // Préparer les arguments pour Java.extend(Type... types, Object implementations)
            Value[] extendArgs = new Value[baseTypesValues.length + 1];
            System.arraycopy(baseTypesValues, 0, extendArgs, 0, baseTypesValues.length);
            extendArgs[baseTypesValues.length] = overrides;

            // Java.extend retourne un constructeur pour la nouvelle classe proxy.
            // Ce constructeur est déjà ProxyInstantiable par nature.
            return javaExtendFunc.execute(extendArgs);
        });
    }

    private boolean isPotentialJavaType(Value val) {
        if (val.isHostObject() && val.asHostObject() instanceof Class) return true;
        if (val.isProxyObject()) {
            ProxyObject po = val.asProxyObject();
            return (po instanceof LazyJsClassHolder || po instanceof JsClassWrapper);
        }
        return false;
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

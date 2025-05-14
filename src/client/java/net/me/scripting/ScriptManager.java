package net.me.scripting;

import net.fabricmc.loader.api.FabricLoader;
import net.me.Main;
import net.me.mappings.MappingsManager;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Set; // Ajout pour la liste d'exclusion

public class ScriptManager {
    private static ScriptManager inst;
    private Context ctx;
    private boolean contextInitialized = false;

    // Optionnel : Liste d'exclusion pour les classes connues pour poser problème
    // même avec le chargement paresseux si elles sont appelées trop tôt par un script.
    private static final Set<String> EXCLUDED_YARN_CLASSES = Set.of(
            // "net.minecraft.server.ServerLinks", // Exemple
            // "net.minecraft.network.state.ConfigurationStates" // Exemple
    );

    // Champs pour stocker les mappings une fois récupérés, pour les utiliser dans createActualJsClassWrapper
    private Map<String, String> classMap; // Yarn FQCN -> Runtime FQCN
    private Map<String, Map<String, List<String>>> methodMap;
    private Map<String, Map<String, String>> fieldMap;
    private Map<String, String> runtimeToYarnClassMap;

    private ScriptManager() {
    }

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
        this.classMap = mm.getClassMap(); // Stocker pour usage ultérieur
        this.methodMap = mm.getMethodMap();
        this.fieldMap = mm.getFieldMap();
        this.runtimeToYarnClassMap = mm.getRuntimeToYarnClassMap();

        JsPackage globalPackageScope = new JsPackage();

        this.classMap.keySet().stream()
                .filter(yarnName -> !EXCLUDED_YARN_CLASSES.contains(yarnName)) // Appliquer l'exclusion
                .filter(yarnName -> yarnName.startsWith("net.minecraft.") || yarnName.startsWith("com.mojang."))
                .forEach(yarnName -> {
                    String runtimeName = this.classMap.get(yarnName);
                    if (runtimeName == null) {
                        Main.LOGGER.warn("No runtime name found for yarn class: {}", yarnName);
                        return;
                    }
                    // Au lieu de créer JsClassWrapper, on crée LazyJsClassHolder
                    LazyJsClassHolder holder = new LazyJsClassHolder(yarnName, runtimeName, this);
                    insertIntoPackageHierarchy(globalPackageScope, yarnName, holder);
                });

        var jsBindings = ctx.getBindings("js");
        for (String topLevelPackageName : (String[]) globalPackageScope.getMemberKeys()) {
            jsBindings.putMember(topLevelPackageName, globalPackageScope.getMember(topLevelPackageName));
        }

        exposeStandardJavaTypes();
        // exposeImportClassFunction devra aussi potentiellement être paresseux ou utiliser createActualJsClassWrapper
        exposeImportClassFunction();


        contextInitialized = true;
        Main.LOGGER.info("Scripting context initialized with lazy class holders.");
    }

    // Nouvelle méthode pour la création réelle du JsClassWrapper, appelée par LazyJsClassHolder
    // Cette méthode est publique ou package-private pour que LazyJsClassHolder puisse l'appeler
    public JsClassWrapper createActualJsClassWrapper(String yarnName, String runtimeName) throws ClassNotFoundException {
        // S'assurer que les mappings sont disponibles (déjà fait via le stockage des champs)
        if (this.runtimeToYarnClassMap == null || this.methodMap == null || this.fieldMap == null) {
            // Cela ne devrait pas arriver si init a bien fonctionné
            throw new IllegalStateException("Mappings not available for creating JsClassWrapper.");
        }
        Class<?> cls = Class.forName(runtimeName, false, ScriptManager.class.getClassLoader());
        ClassMappings combinedMappings = combineMappingsForClassAndSuperclasses(cls, this.runtimeToYarnClassMap, this.methodMap, this.fieldMap);
        return new JsClassWrapper(runtimeName, combinedMappings.methods, combinedMappings.fields);
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

    public static Object[] unwrapArguments(Value[] polyglotArgs, Class<?>[] javaParamTypes) {
        // ... (code inchangé)
        if (polyglotArgs == null) return new Object[0];
        Object[] javaArgs = new Object[polyglotArgs.length];
        for (int i = 0; i < polyglotArgs.length; i++) {
            Value v = polyglotArgs[i];
            Class<?> expectedType = (i < javaParamTypes.length) ? javaParamTypes[i] : null;

            if (v == null || v.isNull()) {
                javaArgs[i] = null;
            } else if (v.isBoolean()) {
                javaArgs[i] = v.asBoolean();
            } else if (v.isNumber()) {
                if (expectedType != null) {
                    if (expectedType == int.class || expectedType == Integer.class) javaArgs[i] = v.asInt();
                    else if (expectedType == long.class || expectedType == Long.class) javaArgs[i] = v.asLong();
                    else if (expectedType == double.class || expectedType == Double.class) javaArgs[i] = v.asDouble();
                    else if (expectedType == float.class || expectedType == Float.class) javaArgs[i] = v.asFloat();
                    else if (expectedType == short.class || expectedType == Short.class) javaArgs[i] = v.asShort();
                    else if (expectedType == byte.class || expectedType == Byte.class) javaArgs[i] = v.asByte();
                    else javaArgs[i] = v.asDouble();
                } else {
                    javaArgs[i] = v.asDouble();
                }
            } else if (v.isString()) {
                javaArgs[i] = v.asString();
            } else if (v.isHostObject()) {
                javaArgs[i] = v.asHostObject();
            } else if (v.isProxyObject()) {
                Object proxied = v.asProxyObject();
                if (proxied instanceof JsObjectWrapper) {
                    javaArgs[i] = ((JsObjectWrapper) proxied).getJavaInstance();
                } else {
                    javaArgs[i] = proxied;
                }
            }
            else {
                Main.LOGGER.warn("Cannot convert JS value to Java: {} (Meta: {})", v, v.getMetaObject());
                try {
                    javaArgs[i] = v.asHostObject();
                } catch (PolyglotException e) {
                    Main.LOGGER.error("Failed to convert value to host object: {}", v, e);
                    javaArgs[i] = null;
                }
            }
        }
        return javaArgs;
    }

    public static Object wrapReturnValue(Object javaRetVal) {
        // ... (code essentiellement inchangé, mais s'assurer qu'il utilise les mappings stockés du ScriptManager si besoin)
        if (javaRetVal == null || javaRetVal instanceof String || javaRetVal instanceof Number || javaRetVal instanceof Boolean || javaRetVal.getClass().isPrimitive()) {
            return javaRetVal;
        }

        Class<?> retClass = javaRetVal.getClass();
        String runtimeFqcn = retClass.getName();

        // Accéder aux mappings via l'instance de ScriptManager si on ne les passe pas partout
        // ou s'assurer que cette méthode a accès aux mappings corrects.
        // Pour l'instant, elle utilise MappingsManager.getInstance() ce qui est ok.
        MappingsManager mm = MappingsManager.getInstance();
        Map<String, String> currentRuntimeToYarn = mm.getRuntimeToYarnClassMap();
        Map<String, Map<String, List<String>>> currentAllMethodMap = mm.getMethodMap();
        Map<String, Map<String, String>> currentAllFieldMap = mm.getFieldMap();

        String yarnFqcn = currentRuntimeToYarn.get(runtimeFqcn);
        if (yarnFqcn != null || retClass.isArray()) {
            ClassMappings combinedMappings = combineMappingsForClassAndSuperclasses(retClass, currentRuntimeToYarn, currentAllMethodMap, currentAllFieldMap);
            try {
                return new JsObjectWrapper(
                        javaRetVal,
                        retClass,
                        combinedMappings.methods,
                        combinedMappings.fields
                );
            } catch (Exception e) {
                Main.LOGGER.warn("Failed to wrap return value of type {}: {}. Falling back to raw host object.", runtimeFqcn, e.getMessage());
            }
        }
        return Value.asValue(javaRetVal);
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
            // Pour déboguer les erreurs JS, il est utile de voir la stacktrace JS
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
        // ... (code inchangé)
        try {
            Value systemClass = ctx.eval("js", "Java.type('java.lang.System')");
            Value threadClass = ctx.eval("js", "Java.type('java.lang.Thread')");
            ctx.getBindings("js").putMember("java.lang.System", systemClass);
            ctx.getBindings("js").putMember("java.lang.Thread", threadClass);

        } catch (PolyglotException e) {
            Main.LOGGER.error("Couldn’t expose standard Java types like System/Thread via Java.type() for pre-binding", e);
        }
    }

    // exposeImportClassFunction modifiée pour utiliser la logique de chargement paresseux
    private void exposeImportClassFunction() {
        ctx.getBindings("js").putMember("importClass", (ProxyExecutable) args -> {
            if (args.length == 0 || !args[0].isString()) {
                throw new RuntimeException("importClass requires a String argument (Yarn FQCN)");
            }
            String yarnName = args[0].asString();

            if (EXCLUDED_YARN_CLASSES.contains(yarnName)) {
                throw new RuntimeException("Class " + yarnName + " is excluded from wrapping.");
            }

            String runtimeName = this.classMap.get(yarnName); // Utilise la map stockée
            if (runtimeName == null) {
                throw new RuntimeException("Class not found in mappings (or not a known mapped class): " + yarnName);
            }
            try {
                // Directement créer le JsClassWrapper ici car importClass implique une utilisation immédiate.
                return createActualJsClassWrapper(yarnName, runtimeName);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException("Runtime class not found for " + yarnName + " (mapped to " + runtimeName + ")", ex);
            } catch (Exception e) { // Attraper d'autres erreurs de createActualJsClassWrapper
                throw new RuntimeException("Failed to create class wrapper for " + yarnName + ": " + e.getMessage(), e);
            }
        });
    }

    // Déplacer ClassMappings et combineMappingsForClassAndSuperclasses pour qu'ils soient statiques
    // ou accessibles par createActualJsClassWrapper si ce dernier n'est pas statique.
    // Ici, ils sont statiques, donc c'sest bon.
    record ClassMappings(
            Map<String, List<String>> methods,
            Map<String, String> fields
    ) {}

    static ClassMappings combineMappingsForClassAndSuperclasses(
            Class<?> cls,
            Map<String, String> runtimeToYarnMap,
            Map<String, Map<String, List<String>>> allYarnMethodsMap,
            Map<String, Map<String, String>> allYarnFieldsMap
    ) {
        // ... (code inchangé)
        Map<String, List<String>> combinedMethods = new HashMap<>();
        Map<String, String> combinedFields = new HashMap<>();

        for (Class<?> currentClass = cls; currentClass != null; currentClass = currentClass.getSuperclass()) {
            String currentRuntimeFqcn = currentClass.getName();
            String currentYarnFqcn = runtimeToYarnMap.get(currentRuntimeFqcn);

            if (currentYarnFqcn != null) {
                var yarnMethodsForClass = allYarnMethodsMap.get(currentYarnFqcn);
                if (yarnMethodsForClass != null) {
                    yarnMethodsForClass.forEach(combinedMethods::putIfAbsent);
                }

                var yarnFieldsForClass = allYarnFieldsMap.get(currentYarnFqcn);
                if (yarnFieldsForClass != null) {
                    yarnFieldsForClass.forEach(combinedFields::putIfAbsent);
                }
            }
        }
        return new ClassMappings(combinedMethods, combinedFields);
    }


    // insertIntoPackageHierarchy modifiée pour insérer des LazyJsClassHolder
    private static void insertIntoPackageHierarchy(JsPackage rootPackage, String fullYarnName, LazyJsClassHolder classHolder) {
        String[] nameParts = fullYarnName.split("\\.");
        if (nameParts.length == 0) {
            Main.LOGGER.warn("Cannot insert class holder for empty or invalid name: {}", fullYarnName);
            return;
        }

        JsPackage currentPackage = rootPackage;
        // Itère jusqu'à l'avant-dernier segment (les segments de package)
        for (int i = 0; i < nameParts.length - 1; i++) {
            String packageSegment = nameParts[i];
            Object existingMember = currentPackage.getMember(packageSegment);

            if (existingMember instanceof JsPackage) {
                currentPackage = (JsPackage) existingMember;
            } else if (existingMember == null) {
                JsPackage newPackage = new JsPackage();
                currentPackage.put(packageSegment, newPackage); // put(String, Object)
                currentPackage = newPackage;
            } else if (existingMember instanceof LazyJsClassHolder || existingMember instanceof JsClassWrapper) {
                // Conflit: un nom de package est déjà utilisé par une classe/holder
                Main.LOGGER.error("Name conflict in package structure: tried to create package segment '{}' but a class/holder already exists at this path for FQCN '{}'.", packageSegment, fullYarnName);
                return;
            } else {
                 Main.LOGGER.error("Unexpected object type in package hierarchy: {} for segment '{}'", existingMember.getClass().getName(), packageSegment);
                 return;
            }
        }

        // Le dernier segment est le nom de la classe simple
        String className = nameParts[nameParts.length - 1];
        if (currentPackage.hasMember(className)) {
            // Conflit: une classe/holder avec ce nom simple existe déjà dans ce package
            Main.LOGGER.warn("Class holder (or package with same name) '{}' already exists in package structure for FQCN '{}'. Skipping.", className, fullYarnName);
            return;
        }
        currentPackage.put(className, classHolder); // put(String, Object)
    }
}
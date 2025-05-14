package net.me.scripting;
import net.fabricmc.loader.api.FabricLoader;
import net.me.Main;
import org.graalvm.polyglot.*;
import net.me.mappings.MappingsManager;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.me.scripting.JsClassWrapper;
import net.me.scripting.JsObjectWrapper;

public class ScriptManager {
    private static ScriptManager inst;
    private Context ctx; // Make it an instance variable, not final if re-init is needed
    private boolean contextInitialized = false;

    private ScriptManager() {
        // Don't build context here initially, or make it lighter
    }

    public static ScriptManager getInstance() {
        if (inst == null) inst = new ScriptManager();
        return inst;
    }

    public void initializeContextAndExposeGlobals(Object mcClientJavaInstance) {
        if (contextInitialized) return;

        ctx = Context.newBuilder("js")
                .allowExperimentalOptions(true)
                .option("js.ecmascript-version", "2021")
                .allowHostAccess(HostAccess.NONE)
                .allowHostClassLookup(name -> name.startsWith("net.minecraft.") || name.startsWith("java."))
                .build();

        // grab all the mappings
        MappingsManager mm = MappingsManager.getInstance();
        Map<String, String> classMap       = mm.getClassMap();
        Map<String, Map<String, List<String>>> methodMap = mm.getMethodMap();
        Map<String, Map<String, String>>     fieldMap  = mm.getFieldMap();
        Map<String, String>                  runtimeToYarn = mm.getRuntimeToYarnClassMap();

        JsPackage root = new JsPackage();   // for net.minecraft.*

        // loop over every Yarn class, but skip if it fails to load or init
        for (String yarnFqcn : classMap.keySet()) {
            if (!yarnFqcn.startsWith("net.")) continue;

            String offFqcn = classMap.get(yarnFqcn);
            try {
                // load without running <clinit>
                Class<?> cls = Class.forName(offFqcn, false, getClass().getClassLoader());

                // merge in all inherited method & field info
                Map<String, List<String>> combinedMethods = new HashMap<>();
                Map<String, String>       combinedFields  = new HashMap<>();
                Class<?> current = cls;
                while (current != null) {
                    String rtName = current.getName();
                    String pyName = runtimeToYarn.get(rtName);
                    if (pyName != null) {
                        Map<String, List<String>> pm = methodMap.get(pyName);
                        Map<String, String>       pf = fieldMap .get(pyName);
                        if (pm != null) combinedMethods.putAll(pm);
                        if (pf != null) combinedFields .putAll(pf);
                    }
                    current = current.getSuperclass();
                }

                // find the right JsPackage
                String[] parts = yarnFqcn.split("\\.");
                JsPackage pkg = root;
                for (int i = 0; i < parts.length - 1; i++) {
                    String seg = parts[i];
                    if (i == 0 && seg.equals("net")) continue;
                    if (!pkg.hasMember(seg)) pkg.put(seg, new JsPackage());
                    pkg = (JsPackage) pkg.getMember(seg);
                }
                String simple = parts[parts.length - 1];

                // wrap it (skip on missing class or bad init)
                try {
                    pkg.put(simple, new JsClassWrapper(offFqcn, combinedMethods, combinedFields));
                } catch (ClassNotFoundException
                         | ExceptionInInitializerError
                         | NoClassDefFoundError ignored) {
                    // just skip
                }
            } catch (Exception e) {
                Main.LOGGER.warn("Error wrapping {}: {}", yarnFqcn, e.toString());
            }
        }

        // bind your 'net' root namespace
        ctx.getBindings("js").putMember("net", root.getMember("minecraft"));

        // expose System and Thread
        try {
            Value sys    = ctx.eval("js", "Java.type('java.lang.System')");
            Value thread = ctx.eval("js", "Java.type('java.lang.Thread')");
            ctx.getBindings("js").putMember("System", sys);
            ctx.getBindings("js").putMember("Thread", thread);
        } catch (Exception e) {
            Main.LOGGER.error("Failed to expose System/Thread", e);
        }

        // importClass helper, with same inheritance logic
        ctx.getBindings("js").putMember("importClass", (ProxyExecutable) args -> {
            String yarnName = args[0].asString();
            String offName  = classMap.get(yarnName);
            if (offName == null) throw new RuntimeException("Unknown class: " + yarnName);
            try {
                Class<?> cls = Class.forName(offName, false, getClass().getClassLoader());
                Map<String, List<String>> combinedMethods = new HashMap<>();
                Map<String, String>       combinedFields  = new HashMap<>();
                Class<?> current = cls;
                while (current != null) {
                    String rtName = current.getName();
                    String pyName = runtimeToYarn.get(rtName);
                    if (pyName != null) {
                        Map<String, List<String>> pm = methodMap.get(pyName);
                        Map<String, String>       pf = fieldMap .get(pyName);
                        if (pm != null) combinedMethods.putAll(pm);
                        if (pf != null) combinedFields .putAll(pf);
                    }
                    current = current.getSuperclass();
                }
                return new JsClassWrapper(offName, combinedMethods, combinedFields);
            } catch (ClassNotFoundException e) {
                return null;
            }
        });

        // expose the live MinecraftClient instance as 'client'
        try {
            String mcYarn = "net.minecraft.client.MinecraftClient";
            if (classMap.containsKey(mcYarn)) {
                Class<?> mcClass = Class.forName(classMap.get(mcYarn));
                JsObjectWrapper clientWrapper = new JsObjectWrapper(
                        mcClientJavaInstance,
                        mcClass,
                        methodMap.get(mcYarn),
                        fieldMap .get(mcYarn)
                );
                ctx.getBindings("js").putMember("client", clientWrapper);
                Main.LOGGER.info("Exposed MinecraftClient as 'client'");
            } else {
                Main.LOGGER.error("Mappings for MinecraftClient not found");
            }
        } catch (Exception e) {
            Main.LOGGER.error("Failed to expose MinecraftClient", e);
        }

        contextInitialized = true;
    }


    public void init() {
        createDir();
    }

    private void createDir() {
        Path path = FabricLoader.getInstance().
                getGameDir().resolve(Main.MOD_ID).resolve("scripts");
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                Main.LOGGER.info("Created scripts directory");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Helper method to unwrap Polyglot Value arguments to Java Objects
    public static Object[] unwrapArguments(Value[] polyglotArgs, Class<?>[] javaParamTypes) {
        if (polyglotArgs == null) return new Object[0];
        Object[] javaArgs = new Object[polyglotArgs.length];
        for (int i = 0; i < polyglotArgs.length; i++) {
            Value v = polyglotArgs[i];
            Class<?> expectedType = (i < javaParamTypes.length) ? javaParamTypes[i] : null; // Be careful with varargs

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
                    else javaArgs[i] = v.asDouble(); // Default for numbers if type unknown
                } else {
                     javaArgs[i] = v.asDouble(); // Or v.isIntegralNumber() ? v.asLong() : v.asDouble();
                }
            } else if (v.isString()) {
                javaArgs[i] = v.asString();
            } else if (v.isHostObject()) { // If it's already a Java object (e.g., passed from another Java call)
                javaArgs[i] = v.asHostObject();
            } else if (v.isProxyObject()) { // If it's one of our own wrappers
                Object proxied = v.asProxyObject();
                if (proxied instanceof JsObjectWrapper) {
                    javaArgs[i] = ((JsObjectWrapper) proxied).getJavaInstance();
                } else {
                    // Potentially handle other proxy types or throw error
                    javaArgs[i] = proxied;
                }
            }
            // Add more conversions if needed (e.g., for JS arrays to Java Lists/arrays)
            else {
                // Fallback or error
                Main.LOGGER.warn("Cannot convert JS value to Java: " + v.toString());
                javaArgs[i] = v.asHostObject(); // Try this as a last resort
            }
        }
        return javaArgs;
    }

    // Helper method to wrap Java Objects to Polyglot Values or JsObjectWrappers
    public static Object wrapReturnValue(Object javaRetVal) {
        if (javaRetVal == null ||
                javaRetVal instanceof String ||
                javaRetVal instanceof Number ||
                javaRetVal instanceof Boolean ||
                javaRetVal.getClass().isPrimitive()) {
            return javaRetVal;
        }

        Class<?> retClass = javaRetVal.getClass();
        String runtimeFqcn = retClass.getName();

        MappingsManager mm = MappingsManager.getInstance();
        Map<String, String> runtimeToYarn = mm.getRuntimeToYarnClassMap();
        Map<String, Map<String, List<String>>> allMethodMap = mm.getMethodMap();
        Map<String, Map<String, String>>       allFieldMap  = mm.getFieldMap();

        String yarnFqcn = runtimeToYarn.get(runtimeFqcn);
        if (yarnFqcn != null) {
            // Merge inherited methods & fields
            Map<String, List<String>> combinedMethods = new HashMap<>();
            Map<String, String>       combinedFields  = new HashMap<>();
            Class<?> current = retClass;
            while (current != null) {
                String parentRuntime = current.getName();
                String parentYarn    = runtimeToYarn.get(parentRuntime);
                if (parentYarn != null) {
                    Map<String, List<String>> pm = allMethodMap.get(parentYarn);
                    Map<String, String>       pf = allFieldMap .get(parentYarn);
                    if (pm != null) combinedMethods.putAll(pm);
                    if (pf != null) combinedFields .putAll(pf);
                }
                current = current.getSuperclass();
            }

            // wrap with the _combined_ maps
            try {
                return new JsObjectWrapper(
                        javaRetVal,
                        retClass,
                        combinedMethods,
                        combinedFields
                );
            } catch (Exception e) {
                // fallback to raw host object if something goes wrong
            }
        }

        // if no Yarn mapping, or wrapper failed, fall back to raw host object
        return Value.asValue(javaRetVal);
    }


    public Value run(String source) {
        return ctx.eval("js", source);
    }
}

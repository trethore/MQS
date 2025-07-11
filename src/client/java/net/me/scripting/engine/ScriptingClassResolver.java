package net.me.scripting.engine;

import net.me.Main;
import net.me.scripting.ScriptManager;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.utils.MappingUtils;
import net.me.scripting.wrappers.JsClassWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ScriptingClassResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptingClassResolver.class);
    private static final Set<String> EXCLUDED = Set.of();

    private final Map<String, JsClassWrapper> wrapperCache = new WeakHashMap<>();

    private MappingsManager mappingsManager;
    private ScriptManager scriptManager;

    private Map<String, String> classMap;
    private Map<String, Map<String, List<String>>> methodMap;
    private Map<String, Map<String, String>> fieldMap;
    private Map<String, String> runtimeToYarn;
    private Set<String> knownPackagePrefixes;

    public ScriptingClassResolver() {
    }

    public void init(MappingsManager mappingsManager, ScriptManager scriptManager) {
        this.mappingsManager = mappingsManager;
        this.scriptManager = scriptManager;
        loadMappings(mappingsManager);
        precomputePackagePrefixes();
    }

    private void loadMappings(MappingsManager mappingsManager) {
        classMap = mappingsManager.getClassMap();
        methodMap = mappingsManager.getMethodMap();
        fieldMap = mappingsManager.getFieldMap();
        runtimeToYarn = mappingsManager.getRuntimeToYarnClassMap();
    }

    private void precomputePackagePrefixes() {
        knownPackagePrefixes = new HashSet<>();
        if (classMap == null) return;

        for (String fqcn : classMap.keySet()) {
            int lastDotIndex = -1;
            while ((lastDotIndex = fqcn.indexOf('.', lastDotIndex + 1)) != -1) {
                knownPackagePrefixes.add(fqcn.substring(0, lastDotIndex));
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

    public Map<String, String> getRuntimeToYarnMap() {
        return runtimeToYarn;
    }

    public Map<String, Map<String, List<String>>> getMethodMap() {
        return methodMap;
    }

    public Map<String, Map<String, String>> getFieldMap() {
        return fieldMap;
    }

    public Set<String> getKnownPackagePrefixes() {
        return Collections.unmodifiableSet(knownPackagePrefixes);
    }

    private boolean isClassIncluded(String name) {
        return !EXCLUDED.contains(name);
    }

    public boolean isClassInMc(String name) {
        return isClassIncluded(name) &&
                (name.startsWith("net.minecraft.") || name.startsWith("com.mojang."));
    }

    public boolean isClassAllowed(String name) {
        if (Main.getInstance().getGlobalConfigManager().areAllClassesAllowed()) {
            return true;
        }

        if (EXCLUDED.contains(name)) return false;

        return name.startsWith("java.")
                || name.startsWith("net.minecraft.")
                || name.startsWith("com.mojang.")
                || name.startsWith("net.me")
                || name.startsWith("com.oracle.truffle.host.adapters.")
                || name.startsWith("net.fabricmc.");
    }

    public JsClassWrapper getOrCreateWrapper(String runtime) {
        return wrapperCache.computeIfAbsent(runtime, r -> {
            try {
                return createActualJsClassWrapper(r);
            } catch (Exception e) {
                LOGGER.error("Failed to create JsClassWrapper for {}", runtime, e);
                throw new RuntimeException("Failed to create class wrapper for " + runtime, e);
            }
        });
    }

    public JsClassWrapper createActualJsClassWrapper(String runtime) throws ClassNotFoundException {
        Class<?> cls = Class.forName(runtime, false, getClass().getClassLoader());
        var cm = MappingUtils.combineMappings(cls, runtimeToYarn, methodMap, fieldMap);
        return new JsClassWrapper(runtime, cm.methods(), cm.fields(), this.mappingsManager, this.scriptManager);
    }

    public MappingsManager getMappingsManager() {
        return mappingsManager;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

}
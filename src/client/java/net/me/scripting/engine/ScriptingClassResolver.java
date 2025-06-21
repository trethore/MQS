package net.me.scripting.engine;

import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.utils.MappingUtils;
import net.me.scripting.wrappers.JsClassWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ScriptingClassResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptingClassResolver.class);
    private final Map<String, JsClassWrapper> wrapperCache = new WeakHashMap<>();
    private Map<String, String> classMap;
    private Map<String, Map<String, List<String>>> methodMap;
    private Map<String, Map<String, String>> fieldMap;
    private Map<String, String> runtimeToYarn;
    private Set<String> knownPackagePrefixes;
    private static final Set<String> EXCLUDED = Set.of();

    public ScriptingClassResolver() {
    }

    public void init() {
        loadMappings();
        precomputePackagePrefixes();
    }

    private void loadMappings() {
        MappingsManager.getInstance().init();
        var mm = MappingsManager.getInstance();
        classMap = mm.getClassMap();
        methodMap = mm.getMethodMap();
        fieldMap = mm.getFieldMap();
        runtimeToYarn = mm.getRuntimeToYarnClassMap();
    }

    private void precomputePackagePrefixes() {
        knownPackagePrefixes = new HashSet<>();
        if (classMap == null) return;

        for (String fqcn : classMap.keySet()) {
            String[] parts = fqcn.split("\\.");
            StringBuilder currentPath = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) {
                    currentPath.append('.');
                }
                currentPath.append(parts[i]);
                knownPackagePrefixes.add(currentPath.toString());
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
        if (EXCLUDED.contains(name)) return false;

        return name.startsWith("java.")
                || name.startsWith("net.minecraft.")
                || name.startsWith("com.mojang.")
                || name.startsWith("net.me")
                || name.startsWith("com.oracle.truffle.host.adapters.");
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
        return new JsClassWrapper(runtime, cm.methods(), cm.fields());
    }
}
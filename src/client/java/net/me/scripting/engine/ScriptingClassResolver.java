/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Réthoré
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.me.scripting.engine;

import lombok.Getter;
import net.me.Main;
import net.me.scripting.ScriptManager;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.utils.MappingUtils;
import net.me.scripting.wrappers.JsClassWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptingClassResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptingClassResolver.class);
    private static final Set<String> EXCLUDED = Set.of();
    private static final String NET_MINECRAFT_PREFIX = "net.minecraft.";
    private static final String COM_MOJANG_PREFIX = "com.mojang.";
    private static final String JAVA_PREFIX = "java.";
    private static final String NET_ME_PREFIX = "net.me";
    private static final String TRUFFLE_PREFIX = "com.oracle.truffle.host.adapters.";
    private static final String FABRIC_PREFIX = "net.fabricmc.";

    private final Map<String, JsClassWrapper> wrapperCache = new WeakHashMap<>();
    private final Map<Class<?>, Boolean> mcRelatedCache = new ConcurrentHashMap<>();

    @Getter
    private MappingsManager mappingsManager;
    @Getter
    private ScriptManager scriptManager;

    private Map<String, String> classMap;
    @Getter
    private Map<String, Map<String, List<String>>> methodMap;
    @Getter
    private Map<String, Map<String, String>> fieldMap;
    private Map<String, String> runtimeToNamed;
    private Set<String> knownPackagePrefixes;

    private ScriptingClassResolver() {
    }

    public static ScriptingClassResolver create(MappingsManager mappingsManager, ScriptManager scriptManager) {
        ScriptingClassResolver resolver = new ScriptingClassResolver();
        resolver.init(mappingsManager, scriptManager);
        return resolver;
    }

    private void init(MappingsManager mappingsManager, ScriptManager scriptManager) {
        this.mappingsManager = mappingsManager;
        this.scriptManager = scriptManager;
        loadMappings(mappingsManager);
        precomputePackagePrefixes();
    }

    private void loadMappings(MappingsManager mappingsManager) {
        classMap = mappingsManager.getClassMap();
        methodMap = mappingsManager.getMethodMap();
        fieldMap = mappingsManager.getFieldMap();
        runtimeToNamed = mappingsManager.getRuntimeToNamedClassMap();
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

    public boolean isMcRelated(Class<?> cls) {
        if (cls == null) {
            return false;
        }
        return mcRelatedCache.computeIfAbsent(cls, this::isMcRelatedUncached);
    }

    private boolean isMcRelatedUncached(Class<?> cls) {
        if (cls == null || cls == Object.class) {
            return false;
        }

        Queue<Class<?>> toCheck = new ArrayDeque<>();
        Set<Class<?>> visited = new HashSet<>();
        enqueueIfValid(toCheck, cls);

        while (!toCheck.isEmpty()) {
            Class<?> currentClass = toCheck.poll();
            if (currentClass == null || !visited.add(currentClass)) {
                continue;
            }

            if (isMinecraftNamespace(currentClass)) {
                return true;
            }

            Class<?> superclass = currentClass.getSuperclass();
            if (isCachedMcRelated(superclass)) {
                return true;
            }
            enqueueIfValid(toCheck, superclass);

            for (Class<?> iface : currentClass.getInterfaces()) {
                if (isCachedMcRelated(iface)) {
                    return true;
                }
                enqueueIfValid(toCheck, iface);
            }
        }

        return false;
    }

    private boolean isMinecraftNamespace(Class<?> cls) {
        String name = cls.getName();
        return name.startsWith(NET_MINECRAFT_PREFIX) || name.startsWith(COM_MOJANG_PREFIX);
    }

    private boolean isCachedMcRelated(Class<?> cls) {
        if (cls == null) {
            return false;
        }
        Boolean cached = mcRelatedCache.get(cls);
        return cached != null && cached;
    }

    private void enqueueIfValid(Queue<Class<?>> queue, Class<?> cls) {
        if (cls != null && cls != Object.class) {
            queue.add(cls);
        }
    }

    public boolean isFullClassPath(String path) {
        return classMap.containsKey(path);
    }

    public boolean isPackage(String path) {
        return knownPackagePrefixes.contains(path);
    }

    public String getRuntimeName(String namedName) {
        return classMap.get(namedName);
    }

    public Map<String, String> getRuntimeToNamedMap() {
        return runtimeToNamed;
    }

    public Set<String> getKnownPackagePrefixes() {
        return Collections.unmodifiableSet(knownPackagePrefixes);
    }

    private boolean isClassIncluded(String name) {
        return !EXCLUDED.contains(name);
    }

    public boolean isClassInMc(String name) {
        return isClassIncluded(name) &&
                (name.startsWith(NET_MINECRAFT_PREFIX) || name.startsWith(COM_MOJANG_PREFIX));
    }

    public boolean isClassAllowed(String name) {
        if (Main.getInstance().getGlobalConfigManager().areAllClassesAllowed()) {
            return true;
        }

        if (EXCLUDED.contains(name)) return false;

        return name.startsWith(JAVA_PREFIX)
                || name.startsWith(NET_MINECRAFT_PREFIX)
                || name.startsWith(COM_MOJANG_PREFIX)
                || name.startsWith(NET_ME_PREFIX)
                || name.startsWith(TRUFFLE_PREFIX)
                || name.startsWith(FABRIC_PREFIX);
    }

    public JsClassWrapper getOrCreateWrapper(String runtime) {
        return wrapperCache.computeIfAbsent(runtime, r -> {
            try {
                return createActualJsClassWrapper(r);
            } catch (ClassNotFoundException _) {
                LOGGER.error("Failed to create JsClassWrapper for {}", runtime);
                throw new IllegalStateException("Failed to create class wrapper for " + runtime);
            }
        });
    }

    public JsClassWrapper createActualJsClassWrapper(String runtime) throws ClassNotFoundException {
        Class<?> cls = Class.forName(runtime, false, getClass().getClassLoader());
        MappingUtils.ClassMappings cm = MappingUtils.combineMappings(cls, runtimeToNamed, methodMap, fieldMap);
        return new JsClassWrapper(runtime, cm.methods(), cm.fields(), this.mappingsManager, this.scriptManager);
    }

}

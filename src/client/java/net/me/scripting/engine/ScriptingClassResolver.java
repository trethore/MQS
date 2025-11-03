/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
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

    public boolean isMcRelated(Class<?> cls) {
        return mcRelatedCache.computeIfAbsent(cls, c -> {
            if (c == null || c == Object.class) {
                return false;
            }

            Queue<Class<?>> toCheck = new LinkedList<>();
            Set<Class<?>> visited = new HashSet<>();
            toCheck.add(c);

            while (!toCheck.isEmpty()) {
                Class<?> currentClass = toCheck.poll();
                if (currentClass == null || !visited.add(currentClass)) {
                    continue;
                }

                String name = currentClass.getName();
                if (name.startsWith(NET_MINECRAFT_PREFIX) || name.startsWith(COM_MOJANG_PREFIX)) {
                    return true;
                }

                if (currentClass.getSuperclass() != null) {
                    Boolean isSuperMcRelated = mcRelatedCache.get(currentClass.getSuperclass());
                    if (isSuperMcRelated != null && isSuperMcRelated) {
                        return true;
                    }
                    toCheck.add(currentClass.getSuperclass());
                }

                for (Class<?> iface : currentClass.getInterfaces()) {
                    Boolean isIfaceMcRelated = mcRelatedCache.get(iface);
                    if (isIfaceMcRelated != null && isIfaceMcRelated) {
                        return true;
                    }
                    toCheck.add(iface);
                }
            }

            return false;
        });
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

}

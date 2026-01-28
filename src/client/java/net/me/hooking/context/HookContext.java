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

package net.me.hooking.context;

import net.me.scripting.ScriptManager;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.utils.ScriptUtils;
import org.graalvm.polyglot.HostAccess;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class HookContext {
    private final Object instance;
    private final Method method;
    private final StackWalker stackWalker;
    private final MappingsManager mappingsManager;
    private final ScriptManager scriptManager;

    private String namedMethodName;
    private String namedClassName;

    public HookContext(Object instance, Method method, StackWalker stackWalker, MappingsManager mappingsManager, ScriptManager scriptManager) {
        this.instance = instance;
        this.method = method;
        this.stackWalker = stackWalker;
        this.mappingsManager = mappingsManager;
        this.scriptManager = scriptManager;
    }

    @HostAccess.Export
    public Object getInstance() {
        return ScriptUtils.wrapReturn(instance, mappingsManager, scriptManager);
    }

    @HostAccess.Export
    public List<CallerInfo> getCallers(int depth) {
        if (depth <= 0) {
            return Collections.emptyList();
        }
        return this.stackWalker.walk(frames -> frames
                .skip(3)
                .limit(depth)
                .map(frame -> new CallerInfo(frame.toStackTraceElement()))
                .toList());
    }

    @HostAccess.Export
    public CallerInfo getCaller() {
        List<CallerInfo> singleCallerList = getCallers(1);
        return (singleCallerList != null && !singleCallerList.isEmpty()) ? singleCallerList.getFirst() : null;
    }

    @HostAccess.Export
    public MethodInfo getMethod() {
        return new MethodInfo(method, mappingsManager, scriptManager);
    }

    @HostAccess.Export
    public boolean isStatic() {
        return new MethodInfo(method, mappingsManager, scriptManager).isStatic();
    }

    @HostAccess.Export
    public Object getMethodClass() {
        return scriptManager.getClassResolver().getOrCreateWrapper(method.getDeclaringClass().getName());
    }

    @HostAccess.Export
    public String getNamedMethodClass() {
        if (namedClassName == null) {
            namedClassName = mappingsManager.getRuntimeToNamedClassMap().getOrDefault(method.getDeclaringClass().getName(), method.getDeclaringClass().getName());
        }
        return namedClassName;
    }

    @HostAccess.Export
    public String getNamedMethod() {
        if (namedMethodName != null) {
            return namedMethodName;
        }

        String declaringClassNamedName = getNamedMethodClass();
        Map<String, List<String>> methodsForClass = mappingsManager.getMethodMap().get(declaringClassNamedName);

        this.namedMethodName = findNamedMethodName(methodsForClass, method.getName());
        return this.namedMethodName;
    }

    private String findNamedMethodName(Map<String, List<String>> methodsForClass, String runtimeName) {
        if (methodsForClass == null) {
            return runtimeName;
        }
        return methodsForClass.entrySet().stream()
                .filter(entry -> entry.getValue().contains(runtimeName))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(runtimeName);
    }
}

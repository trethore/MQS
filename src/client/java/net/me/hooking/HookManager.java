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

package net.me.hooking;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import net.me.Main;
import net.me.scripting.ScriptManager;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.MappingUtils;
import org.graalvm.polyglot.Value;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class HookManager {
    private final Instrumentation instrumentation;
    private final Map<RunningScript, Set<HookIdentifier>> scriptOwnedHooks = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> nameToClassMap = new ConcurrentHashMap<>();
    private final Map<Class<?>, Set<HookIdentifier>> hookedMethods = new ConcurrentHashMap<>();
    private final Map<Class<?>, ElementMatcher.Junction<MethodDescription>> matcherCache = new ConcurrentHashMap<>();
    private final ScriptManager scriptManager;
    private final MappingsManager mappingsManager;

    public HookManager(ScriptManager scriptManager, MappingsManager mappingsManager) {
        this.instrumentation = ByteBuddyAgent.install();
        this.scriptManager = scriptManager;
        this.mappingsManager = mappingsManager;
        installAgent();
    }

    public void hook(RunningScript owner, Class<?> targetClass, String namedMethodName, Value jsCallback, HookOptions options) {
        if (!isValidCallback(jsCallback)) {
            Main.LOGGER.error("Script '{}' attempted to hook method '{}' in class '{}' with an invalid callback.",
                    owner.getName(), namedMethodName, targetClass.getName());
            return;
        }

        HookIdentifier hookId = createHookIdentifier(targetClass, namedMethodName, options);

        if (isAlreadyHookedByScript(owner, hookId)) {
            Main.LOGGER.warn("Script '{}' has already hooked '{}' with argCount {} in mode {}. Unhook first to replace.",
                    owner.getName(), namedMethodName, hookId.argCount(), hookId.mode());
            return;
        }

        String[] runtimeNames = resolveRuntimeMethodNames(targetClass, namedMethodName);
        if (runtimeNames.length == 0) {
            Main.LOGGER.error("Could not resolve named method '{}' for class '{}'. Hooking failed.",
                    namedMethodName, targetClass.getName());
            return;
        }

        registerHook(owner, targetClass, hookId, runtimeNames, jsCallback);
        retransformClass(targetClass, owner, namedMethodName, hookId);
    }

    public void unhookSingle(RunningScript owner, Class<?> targetClass, String namedMethodName, Integer argCount, HookExecutionMode mode) {
        HookIdentifier toRemove = new HookIdentifier(targetClass, namedMethodName, argCount, mode);
        Main.LOGGER.debug("HookManager.unhookSingle: Received request for {}", toRemove);

        if (!isOwnedByScript(owner, toRemove)) {
            Main.LOGGER.warn("Script '{}' attempted to unhook '{}' (argCount: {}, mode: {}), which it does not own.",
                    owner.getName(), namedMethodName, argCount, mode);
            return;
        }

        unregisterFromInterceptor(targetClass, namedMethodName, owner, argCount, mode);
        removeFromScriptOwnership(owner, toRemove);
        cleanupClassHooks(targetClass, toRemove, namedMethodName, mode);
    }

    public void unhookAllForMethod(RunningScript owner, Class<?> targetClass, String namedMethodName) {
        Set<HookIdentifier> owned = scriptOwnedHooks.get(owner);
        if (owned == null) {
            return;
        }

        List<HookIdentifier> hooksToRemove = owned.stream()
                .filter(id -> id.targetClass().equals(targetClass) && id.namedMethodName().equals(namedMethodName))
                .toList();

        if (hooksToRemove.isEmpty()) {
            Main.LOGGER.warn("Script '{}' attempted to unhook method '{}', but no matching hooks were found.",
                    owner.getName(), namedMethodName);
            return;
        }

        hooksToRemove.forEach(id -> unhookSingle(owner, targetClass, namedMethodName, id.argCount(), id.mode()));
    }

    public void unhookAllForScript(RunningScript owner) {
        Set<HookIdentifier> ownedHooks = scriptOwnedHooks.remove(owner);
        if (ownedHooks == null || ownedHooks.isEmpty()) {
            return;
        }

        Map<Class<?>, Set<HookIdentifier>> hooksByClass = groupHooksByClass(ownedHooks);
        hooksByClass.forEach((targetClass, hooks) -> processClassUnhook(owner, targetClass, hooks));

        Main.LOGGER.info("Unhooked all {} hooks for script '{}'.", ownedHooks.size(), owner.getName());
    }

    private void installAgent() {
        ByteBuddy byteBuddy = new ByteBuddy().with(TypeValidation.DISABLED);

        new AgentBuilder.Default(byteBuddy)
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly())
                .type(this::shouldTransformType)
                .transform(this::applyTransformation)
                .installOn(instrumentation);
    }

    private boolean shouldTransformType(net.bytebuddy.description.type.TypeDescription typeDescription,
                                        ClassLoader classLoader, JavaModule module, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain) {
        boolean shouldTransform = nameToClassMap.containsKey(typeDescription.getName());
        if (shouldTransform) {
            Main.LOGGER.debug("AgentBuilder: Found matching type to transform: {}", typeDescription.getName());
        }
        return shouldTransform;
    }

    private DynamicType.Builder<?> applyTransformation(DynamicType.Builder<?> builder,
                                                       net.bytebuddy.description.type.TypeDescription typeDescription,
                                                       ClassLoader classLoader, JavaModule module,
                                                       ProtectionDomain protectionDomain) {
        try {
            return transformClass(builder, typeDescription);
        } catch (Exception e) {
            Main.LOGGER.error("Unexpected error during transformation of {}", typeDescription.getName(), e);
            return builder;
        }
    }

    private DynamicType.Builder<?> transformClass(DynamicType.Builder<?> builder,
                                                  net.bytebuddy.description.type.TypeDescription typeDescription) {
        Main.LOGGER.debug("Transform: Starting transformation for {}", typeDescription.getSimpleName());

        Class<?> type = nameToClassMap.get(typeDescription.getName());
        if (type == null) {
            return builder;
        }

        ElementMatcher.Junction<MethodDescription> methodMatcher = matcherCache.get(type);
        if (methodMatcher == null) {
            Main.LOGGER.debug("Transform: No active matcher found for {}, removing advice.", type.getSimpleName());
            return builder;
        }

        Main.LOGGER.debug("Transform: Applying advice to {} with matcher: {}", type.getSimpleName(), methodMatcher);
        Advice advice = Advice.withCustomMapping().to(HookInterceptor.class);
        return builder.visit(advice.on(methodMatcher));
    }

    private void updateMatcherForClass(Class<?> targetClass) {
        Set<HookIdentifier> hooksForClass = hookedMethods.get(targetClass);

        if (hooksForClass == null || hooksForClass.isEmpty()) {
            matcherCache.remove(targetClass);
            Main.LOGGER.debug("No hooks for class {}, removing matcher.", targetClass.getSimpleName());
            return;
        }

        ElementMatcher.Junction<MethodDescription> combinedMatcher = hooksForClass.stream()
                .map(hook -> buildMatcherForHook(targetClass, hook))
                .reduce(ElementMatchers.none(), ElementMatcher.Junction::or);

        Main.LOGGER.debug("Updated matcher for {}: {}", targetClass.getSimpleName(), combinedMatcher);
        matcherCache.put(targetClass, combinedMatcher);
    }

    private ElementMatcher.Junction<MethodDescription> buildMatcherForHook(Class<?> targetClass, HookIdentifier hook) {
        String[] runtimeNames = resolveRuntimeMethodNames(targetClass, hook.namedMethodName());
        Main.LOGGER.debug("Updating matcher: processing hook for '{}'. Resolved runtime names: {}",
                hook.namedMethodName(), Arrays.asList(runtimeNames));

        ElementMatcher.Junction<MethodDescription> matcher = ElementMatchers.namedOneOf(runtimeNames);

        if (hook.argCount() != null) {
            matcher = matcher.and(ElementMatchers.takesArguments(hook.argCount()));
            Main.LOGGER.debug("Updating matcher: filtering for {} arguments.", hook.argCount());
        }

        return matcher;
    }

    private boolean isValidCallback(Value jsCallback) {
        return jsCallback != null && jsCallback.canExecute();
    }

    private HookIdentifier createHookIdentifier(Class<?> targetClass, String namedMethodName, HookOptions options) {
        HookOptions resolved = options != null ? options : HookOptions.builder().mode(HookExecutionMode.BEFORE).build();
        HookExecutionMode mode = resolved.mode() != null ? resolved.mode() : HookExecutionMode.BEFORE;
        return new HookIdentifier(targetClass, namedMethodName, resolved.argCount(), mode);
    }

    private boolean isAlreadyHookedByScript(RunningScript owner, HookIdentifier hookId) {
        return scriptOwnedHooks.getOrDefault(owner, Collections.emptySet()).contains(hookId);
    }

    private boolean isOwnedByScript(RunningScript owner, HookIdentifier hookId) {
        Set<HookIdentifier> owned = scriptOwnedHooks.get(owner);
        return owned != null && owned.contains(hookId);
    }

    private void registerHook(RunningScript owner, Class<?> targetClass, HookIdentifier hookId,
                              String[] runtimeNames, Value jsCallback) {
        if (!nameToClassMap.containsKey(targetClass.getName())) {
            nameToClassMap.put(targetClass.getName(), targetClass);
        }

        for (String runtimeName : runtimeNames) {
            String interceptorId = generateInterceptorId(targetClass, runtimeName);
            HookInterceptor.register(interceptorId, jsCallback, owner, scriptManager, hookId.argCount(), hookId.mode());
        }

        hookedMethods.computeIfAbsent(targetClass, _ -> ConcurrentHashMap.newKeySet()).add(hookId);
        scriptOwnedHooks.computeIfAbsent(owner, _ -> ConcurrentHashMap.newKeySet()).add(hookId);
        updateMatcherForClass(targetClass);
    }

    private void retransformClass(Class<?> targetClass, RunningScript owner, String namedMethodName, HookIdentifier hookId) {
        try {
            Main.LOGGER.debug("HookManager.hook: Requesting re-transformation of class: {}", targetClass.getName());
            instrumentation.retransformClasses(targetClass);
            Main.LOGGER.info("Successfully requested hook for method '{}' (argCount: {}, mode: {}) in class '{}' for script '{}'.",
                    namedMethodName, hookId.argCount() == null ? "any" : hookId.argCount(),
                    hookId.mode(), targetClass.getSimpleName(), owner.getName());
        } catch (Exception e) {
            Main.LOGGER.error("Failed to trigger hook for method '{}' in class '{}' for script '{}'. Cleaning up...",
                    namedMethodName, targetClass.getName(), owner.getName(), e);
            unhookSingle(owner, targetClass, namedMethodName, hookId.argCount(), hookId.mode());
        }
    }

    private void unregisterFromInterceptor(Class<?> targetClass, String namedMethodName, RunningScript owner,
                                           Integer argCount, HookExecutionMode mode) {
        String[] runtimeNames = resolveRuntimeMethodNames(targetClass, namedMethodName);
        for (String runtimeName : runtimeNames) {
            String interceptorId = generateInterceptorId(targetClass, runtimeName);
            HookInterceptor.unregister(interceptorId, owner, argCount, mode);
        }
    }

    private void removeFromScriptOwnership(RunningScript owner, HookIdentifier hookId) {
        Set<HookIdentifier> owned = scriptOwnedHooks.get(owner);
        if (owned == null) {
            return;
        }
        owned.remove(hookId);
        if (owned.isEmpty()) {
            scriptOwnedHooks.remove(owner);
        }
    }

    private void cleanupClassHooks(Class<?> targetClass, HookIdentifier removed, String namedMethodName, HookExecutionMode mode) {
        Set<HookIdentifier> methodsOnClass = hookedMethods.get(targetClass);
        if (methodsOnClass == null) {
            return;
        }

        methodsOnClass.remove(removed);
        updateMatcherForClass(targetClass);

        if (methodsOnClass.isEmpty()) {
            Main.LOGGER.debug("HookManager.unhookSingle: Last hook for class {} removed. It will be un-advised.",
                    targetClass.getName());
            removeClassFromTracking(targetClass);
        } else {
            Main.LOGGER.info("Successfully unregistered hook for '{}' (mode: {}) on class '{}'. Other hooks remain.",
                    namedMethodName, mode, targetClass.getSimpleName());
        }

        safeRetransform(targetClass);
    }

    private void removeClassFromTracking(Class<?> targetClass) {
        hookedMethods.remove(targetClass);
        nameToClassMap.remove(targetClass.getName());
        matcherCache.remove(targetClass);
    }

    private void safeRetransform(Class<?> targetClass) {
        try {
            Main.LOGGER.debug("Requesting re-transformation of class: {}", targetClass.getName());
            instrumentation.retransformClasses(targetClass);
        } catch (Exception e) {
            Main.LOGGER.error("Failed to re-transform class '{}' after unhooking.", targetClass.getName(), e);
        }
    }

    private Map<Class<?>, Set<HookIdentifier>> groupHooksByClass(Set<HookIdentifier> hooks) {
        return hooks.stream().collect(Collectors.groupingBy(HookIdentifier::targetClass, Collectors.toSet()));
    }

    private void processClassUnhook(RunningScript owner, Class<?> targetClass, Set<HookIdentifier> hooksToRemove) {
        Set<HookIdentifier> allHooksForClass = hookedMethods.get(targetClass);
        if (allHooksForClass != null) {
            allHooksForClass.removeAll(hooksToRemove);
        }

        for (HookIdentifier id : hooksToRemove) {
            unregisterFromInterceptor(targetClass, id.namedMethodName(), owner, id.argCount(), id.mode());
        }

        updateMatcherForClass(targetClass);

        if (allHooksForClass == null || allHooksForClass.isEmpty()) {
            removeClassFromTracking(targetClass);
        }

        safeRetransform(targetClass);
    }

    private String[] resolveRuntimeMethodNames(Class<?> targetClass, String namedMethodName) {
        MappingUtils.ClassMappings classMappings = MappingUtils.combineMappings(
                targetClass,
                mappingsManager.getRuntimeToNamedClassMap(),
                mappingsManager.getMethodMap(),
                mappingsManager.getFieldMap()
        );

        List<String> runtimeNames = classMappings.methods().get(namedMethodName);
        if (runtimeNames != null && !runtimeNames.isEmpty()) {
            return runtimeNames.toArray(new String[0]);
        }

        return Arrays.stream(targetClass.getMethods())
                .map(Method::getName)
                .filter(name -> name.equals(namedMethodName))
                .distinct()
                .toArray(String[]::new);
    }

    private String generateInterceptorId(Class<?> targetClass, String methodName) {
        return targetClass.getName() + "::" + methodName;
    }

    private record HookIdentifier(Class<?> targetClass, String namedMethodName, Integer argCount,
                                  HookExecutionMode mode) {
    }
}

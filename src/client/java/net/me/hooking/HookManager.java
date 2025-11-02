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

package net.me.hooking;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.me.Main;
import net.me.scripting.ScriptManager;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.MappingUtils;
import org.graalvm.polyglot.Value;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
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


    private void installAgent() {
        ByteBuddy byteBuddy = new ByteBuddy().with(TypeValidation.DISABLED);

        new AgentBuilder.Default(byteBuddy)
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly())
                .type((typeDescription, classLoader, module, classBeingRedefined, protectionDomain) -> {
                    boolean shouldTransform = nameToClassMap.containsKey(typeDescription.getName());
                    if (shouldTransform) {
                        Main.LOGGER.debug("AgentBuilder: Found matching type to transform: {}", typeDescription.getName());
                    }
                    return shouldTransform;
                })
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                    try {
                        Main.LOGGER.debug("Transform: Starting transformation for {}", typeDescription.getSimpleName());
                        Class<?> type = nameToClassMap.get(typeDescription.getName());
                        if (type == null) return builder;

                        ElementMatcher.Junction<MethodDescription> methodMatcher = matcherCache.get(type);
                        if (methodMatcher == null) {
                            Main.LOGGER.debug("Transform: No active matcher found for {}, removing advice.", type.getSimpleName());
                            return builder;
                        }

                        Main.LOGGER.debug("Transform: Applying advice to {} with matcher: {}", type.getSimpleName(), methodMatcher);
                        Advice advice = Advice.withCustomMapping().to(HookInterceptor.class);
                        return builder.visit(advice.on(methodMatcher));
                    } catch (Exception e) {
                        Main.LOGGER.error("Unexpected error during transformation of {}", typeDescription.getName(), e);
                        return builder;
                    }
                })
                .installOn(instrumentation);
    }

    private void updateMatcherForClass(Class<?> targetClass) {
        Set<HookIdentifier> hooksForClass = hookedMethods.get(targetClass);

        if (hooksForClass == null || hooksForClass.isEmpty()) {
            matcherCache.remove(targetClass);
            Main.LOGGER.debug("No hooks for class {}, removing matcher.", targetClass.getSimpleName());
            return;
        }

        ElementMatcher.Junction<MethodDescription> methodMatcher = ElementMatchers.none();
        for (HookIdentifier hookId : hooksForClass) {
            List<String> runtimeNames = Arrays.asList(resolveRuntimeMethodNames(targetClass, hookId.yarnMethodName()));
            Main.LOGGER.debug("Updating matcher: processing hook for '{}'. Resolved runtime names: {}", hookId.yarnMethodName(), runtimeNames);

            ElementMatcher.Junction<MethodDescription> currentMatcher = ElementMatchers.namedOneOf(runtimeNames.toArray(new String[0]));

            if (hookId.argCount() != null) {
                currentMatcher = currentMatcher.and(ElementMatchers.takesArguments(hookId.argCount()));
                Main.LOGGER.debug("Updating matcher:   ...and filtering for {} arguments.", hookId.argCount());
            }
            methodMatcher = methodMatcher.or(currentMatcher);
        }

        Main.LOGGER.debug("Updated matcher for {}: {}", targetClass.getSimpleName(), methodMatcher);
        matcherCache.put(targetClass, methodMatcher);
    }

    public void hook(RunningScript owner, Class<?> targetClass, String yarnMethodName, Value jsCallback, Value options, HookExecutionMode mode) {
        if (jsCallback == null || !jsCallback.canExecute()) {
            Main.LOGGER.error("Script '{}' attempted to hook method '{}' in class '{}' with an invalid or non-executable callback.",
                    owner.getName(), yarnMethodName, targetClass.getName());
            return;
        }

        Integer argCount = null;
        if (options != null && options.hasMembers()) {
            if (options.hasMember("args") && options.getMember("args").isNumber()) {
                argCount = options.getMember("args").asInt();
            }
        }

        HookIdentifier hookId = new HookIdentifier(targetClass, yarnMethodName, argCount, mode);

        boolean alreadyHookedByThisScript = scriptOwnedHooks.getOrDefault(owner, Collections.emptySet()).contains(hookId);

        if (alreadyHookedByThisScript) {
            Main.LOGGER.warn("Script '{}' has already hooked '{}' with argCount {} in mode {}. Unhook it first if you wish to replace it.", owner.getName(), yarnMethodName, argCount, mode);
            return;
        }

        String[] runtimeNames = resolveRuntimeMethodNames(targetClass, yarnMethodName);
        if (runtimeNames.length == 0) {
            Main.LOGGER.error("Could not resolve yarn method name '{}' for class '{}'. Hooking failed.", yarnMethodName, targetClass.getName());
            return;
        }

        boolean isFirstHookForClass = !nameToClassMap.containsKey(targetClass.getName());
        if (isFirstHookForClass) {
            nameToClassMap.put(targetClass.getName(), targetClass);
        }

        for (String runtimeName : runtimeNames) {
            String interceptorId = generateInterceptorId(targetClass, runtimeName);
            HookInterceptor.register(interceptorId, jsCallback, owner, scriptManager, argCount, mode);
        }

        hookedMethods.computeIfAbsent(targetClass, k -> ConcurrentHashMap.newKeySet()).add(hookId);
        scriptOwnedHooks.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet()).add(hookId);
        updateMatcherForClass(targetClass);

        try {
            Main.LOGGER.debug("HookManager.hook: Requesting re-transformation of class: {}", targetClass.getName());
            instrumentation.retransformClasses(targetClass);
            Main.LOGGER.info("Successfully requested hook for method '{}' (argCount: {}, mode: {}) in class '{}' for script '{}'.",
                    yarnMethodName, (argCount == null ? "any" : argCount), mode, targetClass.getSimpleName(), owner.getName());
        } catch (Throwable e) {
            Main.LOGGER.error("Failed to trigger hook for method '{}' in class '{}' for script '{}'. Cleaning up...",
                    yarnMethodName, targetClass.getName(), owner.getName(), e);
            unhookSingle(owner, targetClass, yarnMethodName, argCount, mode);
        }
    }

    public void unhookSingle(RunningScript owner, Class<?> targetClass, String yarnMethodName, Integer argCount, HookExecutionMode mode) {
        HookIdentifier toRemove = new HookIdentifier(targetClass, yarnMethodName, argCount, mode);
        Main.LOGGER.debug("HookManager.unhookSingle: Received request for {}", toRemove);

        Set<HookIdentifier> ownedByScript = scriptOwnedHooks.get(owner);
        if (ownedByScript == null || !ownedByScript.contains(toRemove)) {
            Main.LOGGER.warn("Script '{}' attempted to unhook '{}' (argCount: {}, mode: {}), which it does not own or is not hooked.",
                    owner.getName(), yarnMethodName, argCount, mode);
            return;
        }

        String[] runtimeNames = resolveRuntimeMethodNames(targetClass, yarnMethodName);
        for (String runtimeName : runtimeNames) {
            String interceptorId = generateInterceptorId(targetClass, runtimeName);
            HookInterceptor.unregister(interceptorId, owner, argCount, mode);
        }

        ownedByScript.remove(toRemove);
        if (ownedByScript.isEmpty()) {
            scriptOwnedHooks.remove(owner);
        }

        Set<HookIdentifier> methodsOnClass = hookedMethods.get(targetClass);
        if (methodsOnClass != null) {
            methodsOnClass.remove(toRemove);
            updateMatcherForClass(targetClass);

            if (methodsOnClass.isEmpty()) {
                Main.LOGGER.debug("HookManager.unhookSingle: Last hook for class {} removed. It will be un-advised.", targetClass.getName());
                hookedMethods.remove(targetClass);
                nameToClassMap.remove(targetClass.getName());
                matcherCache.remove(targetClass);
            } else {
                Main.LOGGER.info("Successfully unregistered hook for '{}' (mode: {}) on class '{}'. Other hooks remain.",
                        yarnMethodName, mode, targetClass.getSimpleName());
            }

            try {
                Main.LOGGER.debug("HookManager.unhookSingle: Requesting re-transformation of class: {}", targetClass.getName());
                instrumentation.retransformClasses(targetClass);
            } catch (Exception e) {
                Main.LOGGER.error("Failed to re-transform class '{}' after unhooking.", targetClass.getName(), e);
            }
        }
    }

    public void unhookAllForMethod(RunningScript owner, Class<?> targetClass, String yarnMethodName) {
        Set<HookIdentifier> owned = scriptOwnedHooks.get(owner);
        if (owned == null) return;

        List<HookIdentifier> hooksToRemove = owned.stream()
                .filter(id -> id.targetClass().equals(targetClass) && id.yarnMethodName().equals(yarnMethodName))
                .toList();

        if (hooksToRemove.isEmpty()) {
            Main.LOGGER.warn("Script '{}' attempted to unhook method '{}', but no matching hooks were found.", owner.getName(), yarnMethodName);
            return;
        }

        for (HookIdentifier id : hooksToRemove) {
            unhookSingle(owner, targetClass, yarnMethodName, id.argCount(), id.mode());
        }
    }

    public void unhookAllForScript(RunningScript owner) {
        Set<HookIdentifier> ownedHooks = scriptOwnedHooks.remove(owner);
        if (ownedHooks == null || ownedHooks.isEmpty()) {
            return;
        }

        Map<Class<?>, Set<HookIdentifier>> unhookedByClass = ownedHooks.stream()
                .collect(Collectors.groupingBy(HookIdentifier::targetClass, Collectors.toSet()));

        for (Map.Entry<Class<?>, Set<HookIdentifier>> entry : unhookedByClass.entrySet()) {
            Class<?> targetClass = entry.getKey();
            Set<HookIdentifier> hooksToRemove = entry.getValue();

            Set<HookIdentifier> allHooksForClass = hookedMethods.get(targetClass);
            if (allHooksForClass != null) {
                allHooksForClass.removeAll(hooksToRemove);
            }

            for (HookIdentifier id : hooksToRemove) {
                String[] runtimeNames = resolveRuntimeMethodNames(targetClass, id.yarnMethodName());
                for (String runtimeMethodName : runtimeNames) {
                    HookInterceptor.unregister(generateInterceptorId(targetClass, runtimeMethodName), owner, id.argCount(), id.mode());
                }
            }

            updateMatcherForClass(targetClass);

            if (allHooksForClass == null || allHooksForClass.isEmpty()) {
                hookedMethods.remove(targetClass);
                nameToClassMap.remove(targetClass.getName());
                matcherCache.remove(targetClass);
            }

            try {
                instrumentation.retransformClasses(targetClass);
            } catch (Exception e) {
                Main.LOGGER.error("Failed to re-transform class '{}' after unhooking all hooks for script '{}'.", targetClass.getName(), owner.getName(), e);
            }
        }
        Main.LOGGER.info("Unhooked all {} hooks for script '{}'.", ownedHooks.size(), owner.getName());
    }


    private String[] resolveRuntimeMethodNames(Class<?> targetClass, String yarnMethodName) {
        MappingUtils.ClassMappings classMappings = MappingUtils.combineMappings(
                targetClass,
                mappingsManager.getRuntimeToYarnClassMap(),
                mappingsManager.getMethodMap(),
                mappingsManager.getFieldMap()
        );

        var runtimeNames = classMappings.methods().get(yarnMethodName);
        if (runtimeNames != null && !runtimeNames.isEmpty()) {
            return runtimeNames.toArray(new String[0]);
        } else {
            return Arrays.stream(targetClass.getMethods())
                    .map(Method::getName)
                    .filter(name -> name.equals(yarnMethodName))
                    .distinct()
                    .toArray(String[]::new);
        }
    }

    private String generateInterceptorId(Class<?> targetClass, String methodName) {
        return targetClass.getName() + "::" + methodName;
    }

    private record HookIdentifier(Class<?> targetClass, String yarnMethodName, Integer argCount,
                                  HookExecutionMode mode) {
    }
}

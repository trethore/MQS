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
import java.util.concurrent.CopyOnWriteArrayList;
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

    public void hook(RunningScript owner, Class<?> targetClass, String yarnMethodName, Value jsCallback, Value options) {
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

        HookIdentifier hookId = new HookIdentifier(targetClass, yarnMethodName, argCount);
        String interceptorHookId = generateInterceptorId(targetClass, yarnMethodName);

        Main.LOGGER.debug("HookManager.hook: Received request for {}", hookId);

        boolean alreadyHookedByThisScript = HookInterceptor.HOOKS
                .getOrDefault(interceptorHookId, new CopyOnWriteArrayList<>())
                .stream()
                .anyMatch(data -> data.owner().equals(owner) && Objects.equals(data.argCount(), hookId.argCount()));

        if (alreadyHookedByThisScript) {
            Main.LOGGER.warn("Script '{}' has already hooked '{}' with argCount {}. Unhook it first if you wish to replace it.", owner.getName(), yarnMethodName, argCount);
            return;
        }

        boolean isFirstHookForClass = !nameToClassMap.containsKey(targetClass.getName());
        if (isFirstHookForClass) {
            nameToClassMap.put(targetClass.getName(), targetClass);
        }

        HookInterceptor.register(interceptorHookId, jsCallback, owner, scriptManager, argCount);

        String[] runtimeNames = resolveRuntimeMethodNames(targetClass, yarnMethodName);
        if (runtimeNames.length == 0) {
            HookInterceptor.unregister(interceptorHookId, owner, argCount);
            if (isFirstHookForClass) nameToClassMap.remove(targetClass.getName());
            return;
        }

        for (String runtimeName : runtimeNames) {
            if (!runtimeName.equals(yarnMethodName)) {
                HookInterceptor.register(generateInterceptorId(targetClass, runtimeName), jsCallback, owner, scriptManager, argCount);
            }
        }

        hookedMethods.computeIfAbsent(targetClass, k -> ConcurrentHashMap.newKeySet()).add(hookId);
        scriptOwnedHooks.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet()).add(hookId);
        updateMatcherForClass(targetClass);

        try {
            Main.LOGGER.debug("HookManager.hook: Requesting re-transformation of class: {}", targetClass.getName());
            instrumentation.retransformClasses(targetClass);
            Main.LOGGER.info("Successfully requested hook for method '{}' (argCount: {}) in class '{}' for script '{}'.",
                    yarnMethodName, (argCount == null ? "any" : argCount), targetClass.getSimpleName(), owner.getName());
        } catch (Throwable e) {
            Main.LOGGER.error("Failed to trigger hook for method '{}' in class '{}' for script '{}'. Cleaning up...",
                    yarnMethodName, targetClass.getName(), owner.getName(), e);
            unhookSingle(owner, targetClass, yarnMethodName, argCount);
        }
    }

    public void unhookSingle(RunningScript owner, Class<?> targetClass, String yarnMethodName, Integer argCount) {
        HookIdentifier toRemove = new HookIdentifier(targetClass, yarnMethodName, argCount);
        String interceptorId = generateInterceptorId(targetClass, yarnMethodName);

        Main.LOGGER.debug("HookManager.unhookSingle: Received request for {}", toRemove);

        CopyOnWriteArrayList<HookInterceptor.HookData> hookList = HookInterceptor.HOOKS.get(interceptorId);
        boolean isOwner = hookList != null && hookList.stream().anyMatch(d -> d.owner().equals(owner) && Objects.equals(d.argCount(), argCount));

        if (!isOwner) {
            Main.LOGGER.warn("Script '{}' attempted to unhook '{}' (argCount: {}), which it does not own or is not hooked.",
                    owner.getName(), yarnMethodName, argCount);
            return;
        }

        HookInterceptor.unregister(interceptorId, owner, argCount);
        for (String runtimeMethodName : resolveRuntimeMethodNames(targetClass, yarnMethodName)) {
            if (!runtimeMethodName.equals(yarnMethodName)) {
                HookInterceptor.unregister(generateInterceptorId(targetClass, runtimeMethodName), owner, argCount);
            }
        }

        Set<HookIdentifier> owned = scriptOwnedHooks.get(owner);
        if (owned != null) {
            owned.remove(toRemove);
            if (owned.isEmpty()) {
                scriptOwnedHooks.remove(owner);
            }
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
                Main.LOGGER.info("Successfully unregistered hook for '{}' on class '{}'. Other hooks remain.",
                        yarnMethodName, targetClass.getSimpleName());
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
            unhookSingle(owner, targetClass, yarnMethodName, id.argCount());
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
                String interceptorId = generateInterceptorId(targetClass, id.yarnMethodName());
                HookInterceptor.unregister(interceptorId, owner, id.argCount());

                for (String runtimeMethodName : resolveRuntimeMethodNames(targetClass, id.yarnMethodName())) {
                    if (!runtimeMethodName.equals(id.yarnMethodName())) {
                        HookInterceptor.unregister(generateInterceptorId(targetClass, runtimeMethodName), owner, id.argCount());
                    }
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

    private record HookIdentifier(Class<?> targetClass, String yarnMethodName, Integer argCount) {
    }
}
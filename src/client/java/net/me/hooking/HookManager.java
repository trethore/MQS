package net.me.hooking;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatchers;
import net.fabricmc.loader.api.FabricLoader;
import net.me.Main;
import net.me.scripting.ScriptManager;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.MappingUtils;
import org.graalvm.polyglot.Value;

import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HookManager {
    private final Instrumentation instrumentation;

    private record HookIdentifier(Class<?> targetClass, String yarnMethodName) {
    }

    private final Map<RunningScript, Set<HookIdentifier>> scriptOwnedHooks = new ConcurrentHashMap<>();

    private final Map<String, Class<?>> nameToClassMap = new ConcurrentHashMap<>();
    private final Map<Class<?>, Set<String>> hookedMethods = new ConcurrentHashMap<>();

    private ScriptManager scriptManager;
    private MappingsManager mappingsManager;

    public HookManager() {
        this.instrumentation = ByteBuddyAgent.install();
        installAgent();
    }

    public void init(ScriptManager scriptManager, MappingsManager mappingsManager) {
        this.scriptManager = scriptManager;
        this.mappingsManager = mappingsManager;
    }

    private void installAgent() {
        ByteBuddy byteBuddy = new ByteBuddy().with(TypeValidation.DISABLED);

        new AgentBuilder.Default(byteBuddy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly())
                .type((typeDescription, classLoader, module, classBeingRedefined, protectionDomain) ->
                        nameToClassMap.containsKey(typeDescription.getName()))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                    Class<?> type = nameToClassMap.get(typeDescription.getName());
                    if (type == null) return builder;

                    Set<String> yarnMethodNames = hookedMethods.get(type);
                    if (yarnMethodNames == null || yarnMethodNames.isEmpty()) {
                        Main.LOGGER.info("Agent is un-advising class: {}", type.getSimpleName());
                        return builder;
                    }

                    List<String> runtimeMethodNames = yarnMethodNames.stream()
                            .flatMap(yarn -> Stream.of(resolveRuntimeMethodNames(type, yarn)))
                            .distinct()
                            .collect(Collectors.toList());


                    Main.LOGGER.info("Agent is advising {} for methods: {}", type.getSimpleName(), String.join(", ", runtimeMethodNames));

                    return builder.visit(Advice.to(HookInterceptor.class)
                            .on(ElementMatchers.namedOneOf(runtimeMethodNames.toArray(new String[0]))));
                })
                .installOn(instrumentation);
    }

    public void hook(RunningScript owner, Class<?> targetClass, String yarnMethodName, Value jsCallback) {
        String yarnHookId = generateHookId(targetClass, yarnMethodName);
        if (HookInterceptor.hasHook(yarnHookId)) {
            Main.LOGGER.warn("Hook for {} is already active. Unhook it first.", yarnHookId);
            return;
        }

        nameToClassMap.put(targetClass.getName(), targetClass);
        HookInterceptor.register(yarnHookId, jsCallback, owner, scriptManager);

        String[] runtimeNames = resolveRuntimeMethodNames(targetClass, yarnMethodName);
        if (runtimeNames.length == 0) {
            HookInterceptor.unregister(yarnHookId);
            return;
        }

        for (String runtimeName : runtimeNames) {
            if (!runtimeName.equals(yarnMethodName)) {
                HookInterceptor.register(generateHookId(targetClass, runtimeName), jsCallback, owner, scriptManager);
            }
        }

        hookedMethods.computeIfAbsent(targetClass, k -> ConcurrentHashMap.newKeySet()).add(yarnMethodName);
        scriptOwnedHooks.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet()).add(new HookIdentifier(targetClass, yarnMethodName));

        try {
            instrumentation.retransformClasses(targetClass);
            Main.LOGGER.info("Successfully requested hook for method '{}' in class '{}' for script '{}'.",
                    yarnMethodName, targetClass.getSimpleName(), owner.getName());
        } catch (Throwable e) {
            Main.LOGGER.error("Failed to trigger hook for method '{}' in class '{}' for script '{}'. Cleaning up...",
                    yarnMethodName, targetClass.getName(), owner.getName(), e);

            unhook(owner, targetClass, yarnMethodName);
        }
    }

    public void unhook(RunningScript owner, Class<?> targetClass, String yarnMethodName) {
        String yarnHookId = generateHookId(targetClass, yarnMethodName);
        HookInterceptor.HookData data = HookInterceptor.HOOKS.get(yarnHookId);
        if (data == null || !data.owner().equals(owner)) {
            Main.LOGGER.warn("Script '{}' attempted to unhook '{}', which it does not own or is not hooked.",
                    owner.getName(), yarnHookId);
            return;
        }

        HookInterceptor.unregister(yarnHookId);
        for (String runtimeMethodName : resolveRuntimeMethodNames(targetClass, yarnMethodName)) {
            if (!runtimeMethodName.equals(yarnMethodName)) {
                HookInterceptor.unregister(generateHookId(targetClass, runtimeMethodName));
            }
        }

        Set<HookIdentifier> owned = scriptOwnedHooks.get(owner);
        if (owned != null) {
            owned.remove(new HookIdentifier(targetClass, yarnMethodName));
            if (owned.isEmpty()) {
                scriptOwnedHooks.remove(owner);
            }
        }

        Set<String> methodsOnClass = hookedMethods.get(targetClass);
        if (methodsOnClass != null) {
            methodsOnClass.remove(yarnMethodName);
            if (methodsOnClass.isEmpty()) {
                hookedMethods.remove(targetClass);
                nameToClassMap.remove(targetClass.getName());
            }

            try {
                instrumentation.retransformClasses(targetClass);
                if (methodsOnClass.isEmpty()) {
                    Main.LOGGER.info("Successfully unhooked and restored class '{}'.", targetClass.getSimpleName());
                } else {
                    Main.LOGGER.info("Successfully unregistered hook for '{}' on class '{}'. Class was re-transformed.",
                            yarnMethodName, targetClass.getSimpleName());
                }
            } catch (Exception e) {
                Main.LOGGER.error("Failed to re-transform class '{}' after unhooking.", targetClass.getName(), e);
            }
        }
    }

    public void unhookAll(RunningScript owner) {
        Set<HookIdentifier> owned = scriptOwnedHooks.get(owner);
        if (owned == null || owned.isEmpty()) {
            return;
        }

        Main.LOGGER.info("Unhooking all {} hooks for script '{}'.", owned.size(), owner.getName());
        Set<HookIdentifier> hooksToProcess = new HashSet<>(owned);

        for (HookIdentifier id : hooksToProcess) {
            unhook(owner, id.targetClass(), id.yarnMethodName());
        }
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
            if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                return new String[]{yarnMethodName};
            } else {
                Main.LOGGER.error("Could not find production mapping for method '{}' on class '{}'. The hook will not be applied.", yarnMethodName, targetClass.getName());
                return new String[0];
            }
        }
    }

    private String generateHookId(Class<?> targetClass, String methodName) {
        return targetClass.getName() + "::" + methodName;
    }
}
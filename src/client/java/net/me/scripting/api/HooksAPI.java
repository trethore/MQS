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

package net.me.scripting.api;

import net.me.hooking.HookExecutionMode;
import net.me.hooking.HookManager;
import net.me.hooking.HookOptions;
import net.me.scripting.ScriptManager;
import net.me.scripting.api.internal.ClassResolverHelper;
import net.me.scripting.api.internal.HandleTracker;
import net.me.scripting.api.internal.ScriptContextHelper;
import net.me.scripting.engine.ScriptingClassResolver;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Locale;
import java.util.Set;

public class HooksAPI implements ProxyObject {
    private static final String API_NAME = "Hooks API";
    private static final String HOOK = "hook";
    private static final String UNHOOK = "unhook";
    private static final String UNHOOK_ALL = "unhookAll";
    private static final String HOOK_BEFORE = "before";
    private static final String HOOK_AFTER = "after";
    private static final String HOOK_INSTEAD = "instead";
    private static final String HOOK_USAGE_PREFIX = "Usage: MQS.hooks.";
    private static final String MODE_HOOK_USAGE_SUFFIX = "(target, methodOrHandler, handler?, options?)";
    private static final String HOOK_METHOD_USAGE = "(Class, 'methodName', handler, options?)";
    private static final String HOOK_HANDLER_USAGE = "Usage: MQS.hooks.hook(TargetClass, 'methodName', callbackFunction, [options])";
    private static final String UNHOOK_METHOD_USAGE = "Usage: MQS.hooks.unhook(TargetClass, 'methodName', [options])";
    private static final String UNHOOK_ALL_USAGE = "Usage: MQS.hooks.unhookAll()";
    private static final String HANDLER_EXECUTABLE_MESSAGE = "Handler must be executable.";
    private static final String DESCRIPTOR_EMPTY_MESSAGE = "Descriptor cannot be empty.";
    private static final String UNSUPPORTED_PUT_MEMBER_MESSAGE = "Cannot modify MQS.hooks.";
    private static final Set<String> MEMBER_KEYS = Set.of(
            HOOK_BEFORE,
            HOOK_AFTER,
            HOOK_INSTEAD,
            HOOK,
            UNHOOK,
            UNHOOK_ALL
    );

    private final HookManager hookManager;
    private final ScriptContextHelper contextHelper;
    private final ClassResolverHelper classHelper;
    private final HandleTracker<HookHandle> hookTracker;

    public HooksAPI(HookManager hookManager, ScriptManager scriptManager, ScriptingClassResolver classResolver) {
        this.hookManager = hookManager;
        this.contextHelper = new ScriptContextHelper(scriptManager);
        this.classHelper = new ClassResolverHelper(classResolver);
        this.hookTracker = new HandleTracker<>();
    }

    private static HookOptions resolveHookOptions(Value optionsValue, HookExecutionMode mode, boolean enforceMode) {
        if (enforceMode) {
            return HookOptions.withEnforcedMode(optionsValue, mode);
        }
        return HookOptions.fromScript(optionsValue, mode);
    }

    private static String buildModeUsage(HookExecutionMode mode, String signature) {
        return HOOK_USAGE_PREFIX + mode.name().toLowerCase(Locale.ROOT) + signature;
    }

    private static String requireDescriptor(String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) {
            throw new IllegalArgumentException(DESCRIPTOR_EMPTY_MESSAGE);
        }
        return descriptor;
    }

    private static Value requireExecutable(Value value, String message) {
        if (value == null || !value.canExecute()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case HOOK_BEFORE -> createHookExecutable(HookExecutionMode.BEFORE);
            case HOOK_AFTER -> createHookExecutable(HookExecutionMode.AFTER);
            case HOOK_INSTEAD -> createHookExecutable(HookExecutionMode.INSTEAD);
            case HOOK -> (ProxyExecutable) args -> hook(contextHelper.require(API_NAME), args);
            case UNHOOK -> (ProxyExecutable) args -> unhook(contextHelper.require(API_NAME), args);
            case UNHOOK_ALL -> (ProxyExecutable) args -> unhookAll(contextHelper.require(API_NAME), args);
            default -> null;
        };
    }

    @Override
    public Object getMemberKeys() {
        return MEMBER_KEYS.toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        return MEMBER_KEYS.contains(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException(UNSUPPORTED_PUT_MEMBER_MESSAGE);
    }

    private ProxyExecutable createHookExecutable(HookExecutionMode mode) {
        return args -> {
            ApiArgumentChecks.requireArgCountAtLeast(args, 2, buildModeUsage(mode, MODE_HOOK_USAGE_SUFFIX));
            RunningScript owner = contextHelper.require(API_NAME);
            HookCall call = parseModeHookCall(args, mode);
            hookManager.hook(owner, call.targetClass(), call.methodName(), call.callback(), call.options());
            HookHandle handle = new HookHandle(call.targetClass(), call.methodName(), call.options().argCount(), mode);
            hookTracker.track(owner, handle);
            return contextHelper.createIdempotentDisposer(owner, () -> {
                hookManager.unhookSingle(owner, call.targetClass(), call.methodName(), call.options().argCount(), mode);
                hookTracker.remove(owner, handle);
            });
        };
    }

    private HookCall parseModeHookCall(Value[] args, HookExecutionMode mode) {
        if (args[0].isString()) {
            Value optionsValue = args.length > 2 ? args[2] : null;
            return parseDescriptorHookCall(args[0], args[1], optionsValue, mode, true);
        }

        String usage = buildModeUsage(mode, HOOK_METHOD_USAGE);
        ApiArgumentChecks.requireArgCountAtLeast(args, 3, usage);
        Value optionsValue = args.length > 3 ? args[3] : null;
        return parseClassHookCall(args[0], args[1], args[2], optionsValue, mode, true, usage, HANDLER_EXECUTABLE_MESSAGE);
    }

    private Value hook(RunningScript owner, Value[] args) {
        HookCall call = parseHookInvocation(args);
        hookManager.hook(owner, call.targetClass(), call.methodName(), call.callback(), call.options());
        HookHandle handle = new HookHandle(call.targetClass(), call.methodName(), call.options().argCount(), call.options().mode());
        hookTracker.track(owner, handle);
        return contextHelper.createIdempotentDisposer(owner, () -> {
            hookManager.unhookSingle(owner, call.targetClass(), call.methodName(), call.options().argCount(), call.options().mode());
            hookTracker.remove(owner, handle);
        });
    }

    private Void unhook(RunningScript owner, Value[] args) {
        UnhookInvocation invocation = parseUnhookInvocation(args);
        if (invocation.argCount() != null) {
            HookExecutionMode mode = invocation.mode();
            if (mode != null) {
                hookManager.unhookSingle(owner, invocation.targetClass(), invocation.methodName(), invocation.argCount(), mode);
                hookTracker.dispose(owner, handle -> handle.matches(invocation.targetClass(), invocation.methodName(), invocation.argCount(), mode), ignored -> {
                });
            } else {
                for (HookExecutionMode candidate : HookExecutionMode.values()) {
                    hookManager.unhookSingle(owner, invocation.targetClass(), invocation.methodName(), invocation.argCount(), candidate);
                }
                hookTracker.dispose(owner, handle -> handle.matches(invocation.targetClass(), invocation.methodName(), invocation.argCount(), null), ignored -> {
                });
            }
        } else {
            hookManager.unhookAllForMethod(owner, invocation.targetClass(), invocation.methodName());
            hookTracker.dispose(owner, handle -> handle.matches(invocation.targetClass(), invocation.methodName(), null, null), ignored -> {
            });
        }
        return null;
    }

    private Void unhookAll(RunningScript owner, Value[] args) {
        ApiArgumentChecks.requireArgCount(args, 0, UNHOOK_ALL_USAGE);
        hookManager.unhookAllForScript(owner);
        hookTracker.disposeAll(owner, ignored -> {
        });
        return null;
    }

    private HookCall parseHookInvocation(Value[] args) {
        if ((args.length == 2 || args.length == 3) && args[0].isString() && args[1].canExecute()) {
            Value optionsValue = args.length == 3 ? args[2] : null;
            return parseDescriptorHookCall(args[0], args[1], optionsValue, HookExecutionMode.BEFORE, false);
        }

        ApiArgumentChecks.requireArgCountRange(args, 3, 4, HOOK_HANDLER_USAGE);
        Value optionsValue = args.length == 4 ? args[3] : null;
        return parseClassHookCall(args[0], args[1], args[2], optionsValue, HookExecutionMode.BEFORE, false, HOOK_HANDLER_USAGE, HOOK_HANDLER_USAGE);
    }

    private HookCall parseDescriptorHookCall(Value descriptorValue,
                                             Value callbackValue,
                                             Value optionsValue,
                                             HookExecutionMode mode,
                                             boolean enforceMode) {
        String descriptor = requireDescriptor(descriptorValue.asString());
        Value callback = requireExecutable(callbackValue, HANDLER_EXECUTABLE_MESSAGE);
        ClassResolverHelper.HookTarget target = classHelper.parseDescriptor(descriptor);
        HookOptions options = resolveHookOptions(optionsValue, mode, enforceMode);
        return new HookCall(target.targetClass(), target.methodName(), callback, options);
    }

    private HookCall parseClassHookCall(Value targetValue,
                                        Value methodNameValue,
                                        Value callbackValue,
                                        Value optionsValue,
                                        HookExecutionMode mode,
                                        boolean enforceMode,
                                        String methodUsage,
                                        String callbackMessage) {
        if (methodNameValue == null || !methodNameValue.isString()) {
            throw new IllegalArgumentException(methodUsage);
        }
        Class<?> targetClass = classHelper.resolveFromValue(targetValue);
        String methodName = methodNameValue.asString();
        Value callback = requireExecutable(callbackValue, callbackMessage);
        HookOptions options = resolveHookOptions(optionsValue, mode, enforceMode);
        return new HookCall(targetClass, methodName, callback, options);
    }

    private UnhookInvocation parseUnhookInvocation(Value[] args) {
        Value options = args.length > 1 ? args[1] : null;
        boolean descriptorForm = args.length >= 1 && args[0] != null && args[0].isString();
        boolean descriptorArityValid = switch (args.length) {
            case 1 -> true;
            case 2, 3 -> options != null;
            default -> false;
        };
        if (descriptorForm && descriptorArityValid) {
            String descriptor = requireDescriptor(args[0].asString());
            ClassResolverHelper.HookTarget target = classHelper.parseDescriptor(descriptor);
            Integer argCount = HookOptions.extractArgCount(options);
            HookExecutionMode mode = HookOptions.extractMode(options, null);
            return new UnhookInvocation(target.targetClass(), target.methodName(), argCount, mode);
        }

        ApiArgumentChecks.requireArgCountRange(args, 2, 3, UNHOOK_METHOD_USAGE);
        String methodName = ApiArgumentChecks.requireString(args, 1, UNHOOK_METHOD_USAGE);

        Class<?> targetClass = classHelper.resolveFromValue(args[0]);
        Value optionsValue = args.length == 3 ? args[2] : null;
        Integer argCount = HookOptions.extractArgCount(optionsValue);
        HookExecutionMode mode = HookOptions.extractMode(optionsValue, null);
        return new UnhookInvocation(targetClass, methodName, argCount, mode);
    }

    private record HookHandle(Class<?> targetClass, String methodName, Integer argCount, HookExecutionMode mode) {
        private boolean matches(Class<?> target, String method, Integer argCount, HookExecutionMode mode) {
            if (!targetClass.equals(target) || !methodName.equals(method)) {
                return false;
            }
            if (argCount != null && !argCount.equals(this.argCount)) {
                return false;
            }
            return mode == null || mode == this.mode;
        }
    }

    private record HookCall(Class<?> targetClass, String methodName, Value callback, HookOptions options) {
    }

    private record UnhookInvocation(Class<?> targetClass, String methodName, Integer argCount, HookExecutionMode mode) {
    }
}

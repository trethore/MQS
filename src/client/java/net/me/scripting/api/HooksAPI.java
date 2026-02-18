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
    private static final String HOOK_METHOD_USAGE = "(Class, 'methodName', handler, options?)";
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
        throw new UnsupportedOperationException("Cannot modify MQS.hooks.");
    }

    private ProxyExecutable createHookExecutable(HookExecutionMode mode) {
        return args -> {
            ApiArgumentChecks.requireArgCountAtLeast(args, 2, HOOK_USAGE_PREFIX + mode.name().toLowerCase() + "(target, methodOrHandler, handler?, options?)");
            RunningScript owner = contextHelper.require(API_NAME);
            HookCall call = parseArgs(args, mode);
            hookManager.hook(owner, call.targetClass(), call.methodName(), call.callback(), call.options());
            HookHandle handle = new HookHandle(call.targetClass(), call.methodName(), call.options().argCount(), mode);
            hookTracker.track(owner, handle);
            return contextHelper.createIdempotentDisposer(owner, () -> {
                hookManager.unhookSingle(owner, call.targetClass(), call.methodName(), call.options().argCount(), mode);
                hookTracker.remove(owner, handle);
            });
        };
    }

    private HookCall parseArgs(Value[] args, HookExecutionMode mode) {
        if (args[0].isString()) {
            String descriptor = args[0].asString();
            if (descriptor == null || descriptor.isEmpty()) {
                throw new IllegalArgumentException("Descriptor cannot be empty.");
            }
            Value handler = ApiArgumentChecks.requireExecutable(args, 1, "Handler must be executable.");
            Value optionsValue = args.length > 2 ? args[2] : null;
            HookOptions options = HookOptions.withEnforcedMode(optionsValue, mode);
            ClassResolverHelper.HookTarget parsed = classHelper.parseDescriptor(descriptor);
            return new HookCall(parsed.targetClass(), parsed.methodName(), handler, options);
        }

        ApiArgumentChecks.requireArgCountAtLeast(args, 3, HOOK_USAGE_PREFIX + mode.name().toLowerCase() + HOOK_METHOD_USAGE);
        ApiArgumentChecks.requireString(args, 1, HOOK_USAGE_PREFIX + mode.name().toLowerCase() + HOOK_METHOD_USAGE);

        Class<?> targetClass = classHelper.resolveFromValue(args[0]);
        String methodName = args[1].asString();
        Value handler = ApiArgumentChecks.requireExecutable(args, 2, "Handler must be executable.");
        Value optionsValue = args.length > 3 ? args[3] : null;
        HookOptions options = HookOptions.withEnforcedMode(optionsValue, mode);
        return new HookCall(targetClass, methodName, handler, options);
    }

    private Value hook(RunningScript owner, Value[] args) {
        HookInvocation invocation = parseHookInvocation(args);
        hookManager.hook(owner, invocation.targetClass(), invocation.methodName(), invocation.callback(), invocation.options());
        HookHandle handle = new HookHandle(invocation.targetClass(), invocation.methodName(), invocation.options().argCount(), invocation.options().mode());
        hookTracker.track(owner, handle);
        return contextHelper.createIdempotentDisposer(owner, () -> {
            hookManager.unhookSingle(owner, invocation.targetClass(), invocation.methodName(), invocation.options().argCount(), invocation.options().mode());
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
        ApiArgumentChecks.requireArgCount(args, 0, "Usage: MQS.hooks.unhookAll()");
        hookManager.unhookAllForScript(owner);
        hookTracker.disposeAll(owner, ignored -> {
        });
        return null;
    }

    private HookInvocation parseHookInvocation(Value[] args) {
        if ((args.length == 2 || args.length == 3) && args[0].isString() && args[1].canExecute()) {
            String descriptor = args[0].asString();
            if (descriptor == null || descriptor.isEmpty()) {
                throw new IllegalArgumentException("Descriptor cannot be empty.");
            }
            ClassResolverHelper.HookTarget target = classHelper.parseDescriptor(descriptor);
            Value callback = args[1];
            Value optionsValue = args.length == 3 ? args[2] : null;
            HookOptions options = HookOptions.fromScript(optionsValue, HookExecutionMode.BEFORE);
            return new HookInvocation(target.targetClass(), target.methodName(), callback, options);
        }

        ApiArgumentChecks.requireArgCountRange(args, 3, 4, "MQS.hooks.hook requires 3 or 4 arguments.");
        ApiArgumentChecks.requireString(args, 1, "Usage: MQS.hooks.hook(TargetClass, 'methodName', callbackFunction, [options])");
        ApiArgumentChecks.requireExecutable(args, 2, "Usage: MQS.hooks.hook(TargetClass, 'methodName', callbackFunction, [options])");

        Class<?> targetClass = classHelper.resolveFromValue(args[0]);
        String methodName = args[1].asString();
        Value callback = args[2];
        Value optionsValue = args.length == 4 ? args[3] : null;
        HookOptions options = HookOptions.fromScript(optionsValue, HookExecutionMode.BEFORE);
        return new HookInvocation(targetClass, methodName, callback, options);
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
            String descriptor = args[0].asString();
            if (descriptor == null || descriptor.isEmpty()) {
                throw new IllegalArgumentException("Descriptor cannot be empty.");
            }
            ClassResolverHelper.HookTarget target = classHelper.parseDescriptor(descriptor);
            Integer argCount = HookOptions.extractArgCount(options);
            HookExecutionMode mode = HookOptions.extractMode(options, null);
            return new UnhookInvocation(target.targetClass(), target.methodName(), argCount, mode);
        }

        ApiArgumentChecks.requireArgCountRange(args, 2, 3, "Usage: MQS.hooks.unhook(TargetClass, 'methodName', [options])");
        String methodName = ApiArgumentChecks.requireString(args, 1, "Usage: MQS.hooks.unhook(TargetClass, 'methodName', [options])");

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

    private record HookInvocation(Class<?> targetClass, String methodName, Value callback, HookOptions options) {
    }

    private record UnhookInvocation(Class<?> targetClass, String methodName, Integer argCount, HookExecutionMode mode) {
    }
}

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

package net.me.scripting.api;

import net.me.hooking.HookExecutionMode;
import net.me.hooking.HookManager;
import net.me.hooking.HookOptions;
import net.me.scripting.ScriptManager;
import net.me.scripting.engine.ScriptingClassResolver;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.ScriptUtils;
import net.me.scripting.wrappers.JsClassWrapper;
import net.me.scripting.wrappers.LazyJsClassHolder;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.me.scripting.api.ApiConstants.*;

public class HooksAPI implements ProxyObject {
    private static final String HOOK_USAGE_PREFIX = "Usage: MQS.hooks.";
    private static final String HOOK_METHOD_USAGE = "(Class, 'methodName', handler, options?)";
    private static final Set<String> MEMBER_KEYS = Set.of(
            HOOK_BEFORE,
            HOOK_AFTER,
            HOOK_INSTEAD
    );

    private final HookManager hookManager;
    private final ScriptManager scriptManager;
    private final ScriptingClassResolver classResolver;

    public HooksAPI(HookManager hookManager, ScriptManager scriptManager, ScriptingClassResolver classResolver) {
        this.hookManager = hookManager;
        this.scriptManager = scriptManager;
        this.classResolver = classResolver;
    }

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case HOOK_BEFORE -> createHookExecutable(HookExecutionMode.BEFORE);
            case HOOK_AFTER -> createHookExecutable(HookExecutionMode.AFTER);
            case HOOK_INSTEAD -> createHookExecutable(HookExecutionMode.INSTEAD);
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
            RunningScript owner = getCurrentScript();
            HookCall call = parseArgs(args, mode);
            hookManager.hook(owner, call.targetClass(), call.methodName(), call.callback(), call.options());
            AtomicBoolean disposed = new AtomicBoolean(false);
            ProxyExecutable disposer = _ -> {
                if (disposed.compareAndSet(false, true)) {
                    hookManager.unhookSingle(owner, call.targetClass(), call.methodName(), call.options().argCount(), mode);
                }
                return null;
            };
            return owner.getContext().asValue(disposer);
        };
    }

    private RunningScript getCurrentScript() {
        RunningScript script = scriptManager.getCurrentScript();
        if (script == null) {
            throw new IllegalStateException("Hooks helper can only be used from an active script.");
        }
        return script;
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
            HookDescriptor parsed = parseDescriptor(descriptor);
            Class<?> targetClass = resolveDescriptorClass(parsed.className());
            return new HookCall(targetClass, parsed.methodName(), handler, options);
        }

        ApiArgumentChecks.requireArgCountAtLeast(args, 3, HOOK_USAGE_PREFIX + mode.name().toLowerCase() + HOOK_METHOD_USAGE);
        ApiArgumentChecks.requireString(args, 1, HOOK_USAGE_PREFIX + mode.name().toLowerCase() + HOOK_METHOD_USAGE);

        Object unwrapped = ScriptUtils.unwrapReceiver(args[0]);
        Class<?> targetClass = resolveClass(unwrapped);
        String methodName = args[1].asString();
        Value handler = ApiArgumentChecks.requireExecutable(args, 2, "Handler must be executable.");
        Value optionsValue = args.length > 3 ? args[3] : null;
        HookOptions options = HookOptions.withEnforcedMode(optionsValue, mode);
        return new HookCall(targetClass, methodName, handler, options);
    }

    private HookDescriptor parseDescriptor(String descriptor) {
        int idx = descriptor.indexOf('#');
        if (idx <= 0 || idx == descriptor.length() - 1) {
            throw new IllegalArgumentException("Descriptor must follow the 'fully.qualified.Class#methodName' format.");
        }
        return new HookDescriptor(descriptor.substring(0, idx), descriptor.substring(idx + 1));
    }

    private Class<?> resolveClass(Object unwrapped) {
        if (unwrapped instanceof JsClassWrapper wrapper) {
            return wrapper.getTargetClass();
        }
        if (unwrapped instanceof LazyJsClassHolder holder) {
            return holder.getWrapper().getTargetClass();
        }
        if (unwrapped instanceof Class<?> cls) {
            return cls;
        }
        throw new IllegalArgumentException("Target must be a class imported via importClass().");
    }

    private Class<?> resolveDescriptorClass(String className) {
        try {
            String runtimeName = classResolver.getRuntimeName(className);
            String lookup = runtimeName != null ? runtimeName : className;
            return Class.forName(lookup, false, HooksAPI.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not resolve class '" + className + "'.", e);
        }
    }

    private record HookDescriptor(String className, String methodName) {
    }

    private record HookCall(Class<?> targetClass, String methodName, Value callback, HookOptions options) {
    }
}

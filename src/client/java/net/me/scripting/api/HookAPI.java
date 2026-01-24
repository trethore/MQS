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

import static net.me.scripting.api.ApiConstants.*;

public class HookAPI implements ProxyObject {

    private static final Set<String> MEMBER_KEYS = Set.of(HOOK, UNHOOK, UNHOOK_ALL);
    private final HookManager hookManager;
    private final ScriptManager scriptManager;
    private final ScriptingClassResolver classResolver;

    public HookAPI(HookManager hookManager, ScriptManager scriptManager, ScriptingClassResolver classResolver) {
        this.hookManager = hookManager;
        this.scriptManager = scriptManager;
        this.classResolver = classResolver;
    }

    private RunningScript getCurrentScript() {
        RunningScript script = scriptManager.getCurrentScript();
        if (script == null) {
            throw new IllegalStateException("HookManager can only be used inside a running script context.");
        }
        return script;
    }

    @Override
    public Object getMember(String key) {
        return (ProxyExecutable) args -> {
            RunningScript owner = getCurrentScript();

            switch (key) {
                case HOOK: {
                    HookInvocation invocation = parseHookInvocation(args);
                    hookManager.hook(owner, invocation.targetClass(), invocation.methodName(), invocation.callback(), invocation.options());
                    return null;
                }

                case UNHOOK: {
                    UnhookInvocation invocation = parseUnhookInvocation(args);
                    if (invocation.argCount() != null) {
                        HookExecutionMode mode = invocation.mode();
                        if (mode != null) {
                            hookManager.unhookSingle(owner, invocation.targetClass(), invocation.methodName(), invocation.argCount(), mode);
                        } else {
                            for (HookExecutionMode candidate : HookExecutionMode.values()) {
                                hookManager.unhookSingle(owner, invocation.targetClass(), invocation.methodName(), invocation.argCount(), candidate);
                            }
                        }
                    } else {
                        hookManager.unhookAllForMethod(owner, invocation.targetClass(), invocation.methodName());
                    }
                    return null;
                }

                case UNHOOK_ALL: {
                    if (args.length != 0) {
                        throw new IllegalArgumentException("Usage: HookManager.unhookAll()");
                    }
                    hookManager.unhookAllForScript(owner);
                    return null;
                }


                default:
                    throw new UnsupportedOperationException("Unsupported HookManager operation: " + key);
            }
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
        throw new UnsupportedOperationException("Cannot modify the HookManager object.");
    }

    private HookInvocation parseHookInvocation(Value[] args) {
        if ((args.length == 2 || args.length == 3) && args[0].isString() && args[1].canExecute()) {
            String descriptor = args[0].asString();
            if (descriptor == null || descriptor.isEmpty()) {
                throw new IllegalArgumentException("Descriptor cannot be empty.");
            }
            HookDescriptor descriptorParts = parseDescriptor(descriptor);
            Class<?> targetClass = resolveDescriptorClass(descriptorParts.className());
            Value callback = args[1];
            Value optionsValue = args.length == 3 ? args[2] : null;
            HookOptions options = HookOptions.fromScript(optionsValue, HookExecutionMode.BEFORE);
            return new HookInvocation(targetClass, descriptorParts.methodName(), callback, options);
        }


        if (args.length != 3 && args.length != 4) {
            throw new IllegalArgumentException("HookManager.hook requires 3 or 4 arguments.");
        }
        if (!args[1].isString() || !args[2].canExecute()) {
            throw new IllegalArgumentException("Usage: HookManager.hook(TargetClass, 'methodName', callbackFunction, [options])");
        }

        Object unwrappedArg = ScriptUtils.unwrapReceiver(args[0]);
        Class<?> targetClass = resolveClass(unwrappedArg);
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
            HookDescriptor descriptorParts = parseDescriptor(descriptor);
            Class<?> targetClass = resolveDescriptorClass(descriptorParts.className());
            Integer argCount = HookOptions.extractArgCount(options);
            HookExecutionMode mode = HookOptions.extractMode(options, null);
            return new UnhookInvocation(targetClass, descriptorParts.methodName(), argCount, mode);
        }

        Value methodArgument = args.length > 1 ? args[1] : null;
        if (args.length < 2 || args.length > 3 || methodArgument == null || !methodArgument.isString()) {
            throw new IllegalArgumentException("Usage: HookManager.unhook(TargetClass, 'methodName', [options])");
        }

        Object unwrappedArg = ScriptUtils.unwrapReceiver(args[0]);
        Class<?> targetClass = resolveClass(unwrappedArg);
        String methodName = methodArgument.asString();

        Value optionsValue = args.length == 3 ? args[2] : null;
        Integer argCount = HookOptions.extractArgCount(optionsValue);
        HookExecutionMode mode = HookOptions.extractMode(optionsValue, null);
        return new UnhookInvocation(targetClass, methodName, argCount, mode);
    }

    private HookDescriptor parseDescriptor(String descriptor) {
        int idx = descriptor.indexOf('#');
        if (idx <= 0 || idx == descriptor.length() - 1) {
            throw new IllegalArgumentException("Descriptor must follow the 'fully.qualified.Class#methodName' format.");
        }
        return new HookDescriptor(descriptor.substring(0, idx), descriptor.substring(idx + 1));
    }

    private Class<?> resolveDescriptorClass(String className) {
        try {
            String runtimeName = classResolver.getRuntimeName(className);
            String lookup = runtimeName != null ? runtimeName : className;
            return Class.forName(lookup, false, HookAPI.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not resolve class '" + className + "'.", e);
        }
    }

    private Class<?> resolveClass(Object unwrappedArg) {
        return switch (unwrappedArg) {
            case JsClassWrapper wrapper -> wrapper.getTargetClass();
            case LazyJsClassHolder holder -> holder.getWrapper().getTargetClass();
            case Class<?> cls -> cls;
            case null, default ->
                    throw new IllegalArgumentException("First argument must be a class (e.g. from importClass).");
        };
    }

    private record HookDescriptor(String className, String methodName) {
    }

    private record HookInvocation(Class<?> targetClass, String methodName, Value callback, HookOptions options) {
    }

    private record UnhookInvocation(Class<?> targetClass, String methodName, Integer argCount, HookExecutionMode mode) {
    }
}

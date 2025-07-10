package net.me.scripting.api;

import net.me.hooking.HookManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.ScriptUtils;
import net.me.scripting.wrappers.JsClassWrapper;
import net.me.scripting.wrappers.LazyJsClassHolder;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

public class HookAPI implements ProxyObject {

    private final HookManager hookManager;
    private final ScriptManager scriptManager;

    public HookAPI(HookManager hookManager, ScriptManager scriptManager) {
        this.hookManager = hookManager;
        this.scriptManager = scriptManager;
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
                case "hook": {
                    Value targetClassValue;
                    String yarnMethodName;
                    Value callback;
                    Value optionsValue = null;

                    if (args.length == 3) {
                        if (!args[1].isString() || !args[2].canExecute()) {
                            throw new IllegalArgumentException("Usage: HookManager.hook(TargetClass, 'methodName', callbackFunction, [options])");
                        }
                        targetClassValue = args[0];
                        yarnMethodName = args[1].asString();
                        callback = args[2];
                    } else if (args.length == 4) {
                        if (!args[1].isString() || !args[2].canExecute()) {
                            throw new IllegalArgumentException("Usage: HookManager.hook(TargetClass, 'methodName', callbackFunction, [options])");
                        }
                        targetClassValue = args[0];
                        yarnMethodName = args[1].asString();
                        callback = args[2];
                        optionsValue = args[3];
                    } else {
                        throw new IllegalArgumentException("HookManager.hook requires 3 or 4 arguments.");
                    }

                    Object unwrappedArg = ScriptUtils.unwrapReceiver(targetClassValue);
                    Class<?> targetClass = switch (unwrappedArg) {
                        case JsClassWrapper wrapper -> wrapper.getTargetClass();
                        case LazyJsClassHolder holder -> holder.getWrapper().getTargetClass();
                        case Class<?> cls -> cls;
                        case null, default ->
                                throw new IllegalArgumentException("First argument must be a class (e.g. from importClass).");
                    };

                    hookManager.hook(owner, targetClass, yarnMethodName, callback, optionsValue);
                    return null;
                }

                case "unhook": {
                    if (args.length < 2 || args.length > 3 || !args[1].isString()) {
                        throw new IllegalArgumentException("Usage: HookManager.unhook(TargetClass, 'methodName', [options])");
                    }

                    Object unwrappedArg = ScriptUtils.unwrapReceiver(args[0]);
                    Class<?> targetClass = switch (unwrappedArg) {
                        case JsClassWrapper wrapper -> wrapper.getTargetClass();
                        case LazyJsClassHolder holder -> holder.getWrapper().getTargetClass();
                        case Class<?> cls -> cls;
                        case null, default ->
                                throw new IllegalArgumentException("First argument must be a class (e.g. from importClass).");
                    };
                    String yarnMethodName = args[1].asString();

                    if (args.length == 3 && args[2] != null && args[2].hasMembers()) {
                        Value optionsValue = args[2];
                        Integer argCount = null;
                        if (optionsValue.hasMember("args") && optionsValue.getMember("args").isNumber()) {
                            argCount = optionsValue.getMember("args").asInt();
                        }
                        hookManager.unhookSingle(owner, targetClass, yarnMethodName, argCount);
                    } else {
                        hookManager.unhookAllForMethod(owner, targetClass, yarnMethodName);
                    }
                    return null;
                }

                case "unhookAll": {
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
        return new String[]{"hook", "unhook", "unhookAll"};
    }

    @Override
    public boolean hasMember(String key) {
        return "hook".equals(key) || "unhook".equals(key) || "unhookAll".equals(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify the HookManager object.");
    }
}
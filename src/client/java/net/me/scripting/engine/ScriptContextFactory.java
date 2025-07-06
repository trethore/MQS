package net.me.scripting.engine;

import net.me.Main;
import net.me.event.Event;
import net.me.event.EventManager;
import net.me.hooking.HookManager;
import net.me.keybinds.KeybindManager;
import net.me.scripting.ConfigManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.commands.CommandAPIService;
import net.me.scripting.commands.CommandsAPI;
import net.me.scripting.keybinds.KeybindAPI;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.ScriptUtils;
import net.me.scripting.wrappers.JsClassWrapper;
import net.me.scripting.wrappers.LazyJsClassHolder;
import net.me.scripting.wrappers.LazyPackageProxy;
import net.me.utils.*;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.*;

public class ScriptContextFactory {

    private final ScriptingClassResolver classResolver;
    private final Engine sharedEngine;
    private final EventManager eventManager;
    private final ScriptManager scriptManager;
    private final HookManager hookManager;
    private final ConfigManager configManager;
    private final CommandAPIService commandApiService;
    private final Set<String> standardApiMembers = new HashSet<>();
    private final KeybindManager keybindManager;

    public ScriptContextFactory(ScriptingClassResolver classResolver, Engine sharedEngine, ScriptManager scriptManager, EventManager eventManager, ConfigManager configManager, CommandAPIService commandApiService, HookManager hookManager, KeybindManager keybindManager) {
        this.classResolver = classResolver;
        this.sharedEngine = sharedEngine;
        this.scriptManager = scriptManager;
        this.configManager = configManager;
        this.commandApiService = commandApiService;
        this.eventManager = eventManager;
        this.hookManager = hookManager;
        this.keybindManager = keybindManager;

    }

    public Context createContext(ThreadLocal<Map<String, Value>> perFileExports) {
        Main.LOGGER.info("Creating new script context (ECMAScript 2024)...");
        long startTime = System.currentTimeMillis();

        Context newContext = Context.newBuilder("js")
                .engine(sharedEngine)
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(classResolver::isClassAllowed)
                .option("js.ecmascript-version", "2024")
                .option("js.esm-eval-returns-exports", "true")
                .allowIO(IOAccess.ALL)
                .build();

        configureContext(newContext, perFileExports);

        long endTime = System.currentTimeMillis();
        Main.LOGGER.info("New script context created in {}ms.", (endTime - startTime));
        return newContext;
    }

    private void configureContext(Context context, ThreadLocal<Map<String, Value>> perFileExports) {
        if (!standardApiMembers.isEmpty()) {
            standardApiMembers.clear();
        }
        registerPackages(context);

        Value bindings = context.getBindings("js");

        addApiMember(bindings, "importClass", ScriptingApi.createImportClassProxy(classResolver, context));
        addApiMember(bindings, "extendMapped", ScriptingApi.createExtendMappedProxy(classResolver, context));
        addApiMember(bindings, "wrap", ScriptingApi.createWrapProxy(classResolver));
        addApiMember(bindings, "exportModule", ScriptingApi.createExportModuleProxy(perFileExports));
        addApiMember(bindings, "EventManager", createEventManagerProxy());
        addApiMember(bindings, "ConfigManager", createConfigProxy());

        addApiMember(bindings, "KeybindManager", new KeybindAPI(this.keybindManager, this.scriptManager));
        addApiMember(bindings, "CommandManager", new CommandsAPI(this.scriptManager, this.commandApiService));
        addApiMember(bindings, "HookManager", createHookManagerProxy());
        addApiMember(bindings, "MQSUtils", createMqsUtilsProxy());

        addApiMember(bindings, "println", (ProxyExecutable) args -> {
            for (Value arg : args) System.out.println(arg);
            return null;
        });
        addApiMember(bindings, "print", (ProxyExecutable) args -> {
            for (Value arg : args) System.out.print(arg);
            return null;
        });
    }

    private ProxyObject createMqsUtilsProxy() {
        final Map<String, Class<?>> utilsMap = new HashMap<>();
        utilsMap.put("Render2D", Render2DUtils.class);
        utilsMap.put("Render3D", Render3DUtils.class);
        utilsMap.put("TextRender", TextRenderUtils.class);
        utilsMap.put("TextRenderer", TextRendererUtils.class);
        utilsMap.put("Chat", ChatUtils.class);
        utilsMap.put("Color", ColorUtils.class);
        utilsMap.put("Camera", CameraUtils.class);
        utilsMap.put("Mc", McUtils.class);

        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                Class<?> utilClass = utilsMap.get(key);
                if (utilClass != null) {
                    return classResolver.getOrCreateWrapper(utilClass.getName());
                }
                return null;
            }

            @Override
            public Object getMemberKeys() {
                return utilsMap.keySet().toArray(new String[0]);
            }

            @Override
            public boolean hasMember(String key) {
                return utilsMap.containsKey(key);
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("Cannot modify the MQSUtils object.");
            }
        };
    }

    private ProxyObject createHookManagerProxy() {
        return new ProxyObject() {
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

                    if ("hook".equals(key)) {
                        Value targetClassValue;
                        String yarnMethodName;
                        Integer argCount = null;
                        Value callback;

                        if (args.length == 3) {
                            if (!args[1].isString() || !args[2].canExecute()) {
                                throw new IllegalArgumentException("Usage: HookManager.hook(TargetClass, 'methodName', callbackFunction)");
                            }
                            targetClassValue = args[0];
                            yarnMethodName = args[1].asString();
                            callback = args[2];
                        } else if (args.length == 4) {
                            if (!args[1].isString() || !args[2].isNumber() || !args[3].canExecute()) {
                                throw new IllegalArgumentException("Usage: HookManager.hook(TargetClass, 'methodName', argCount, callbackFunction)");
                            }
                            targetClassValue = args[0];
                            yarnMethodName = args[1].asString();
                            argCount = args[2].asInt();
                            callback = args[3];
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

                        hookManager.hook(owner, targetClass, yarnMethodName, callback, argCount);
                        return null;
                    }

                    if ("unhook".equals(key)) {
                        if (args.length != 2 || !args[1].isString()) {
                            throw new IllegalArgumentException("Usage: HookManager.unhook(TargetClass, 'methodName')");
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
                        hookManager.unhook(owner, targetClass, yarnMethodName);
                        return null;
                    }

                    throw new UnsupportedOperationException("Unsupported HookManager operation: " + key);
                };
            }

            @Override
            public Object getMemberKeys() {
                return new String[]{"hook", "unhook"};
            }

            @Override
            public boolean hasMember(String key) {
                return "hook".equals(key) || "unhook".equals(key);
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("Cannot modify the HookManager object.");
            }
        };
    }

    private void addApiMember(Value bindings, String name, Object member) {
        bindings.putMember(name, member);
        standardApiMembers.add(name);
    }

    public void resetContext(Context context) {
        Value bindings = context.getBindings("js");
        Set<String> memberKeys = new HashSet<>(bindings.getMemberKeys());

        for (String key : memberKeys) {
            if (!standardApiMembers.contains(key)) {
                bindings.removeMember(key);
            }
        }
    }

    private void registerPackages(Context context) {
        Set<String> topLevelPackages = new HashSet<>();
        if (classResolver.getKnownPackagePrefixes() != null) {
            for (String prefix : classResolver.getKnownPackagePrefixes()) {
                if (classResolver.isClassInMc(prefix)) {
                    topLevelPackages.add(prefix.split("\\.")[0]);
                }
            }
        }

        var bindings = context.getBindings("js");
        for (String pkg : topLevelPackages) {
            if (!bindings.hasMember(pkg)) {
                addApiMember(bindings, pkg, new LazyPackageProxy(pkg, this.classResolver));
            }
        }
    }

    private ProxyObject createEventManagerProxy() {
        return new ProxyObject() {
            private final ProxyObject eventsEnumProxy = createEventsEnumProxy();

            @Override
            public Object getMember(String key) {
                if ("Events".equals(key)) {
                    return eventsEnumProxy;
                }

                return (ProxyExecutable) args -> {
                    RunningScript owner = scriptManager.getCurrentScript();
                    if (owner == null) {
                        throw new IllegalStateException("EventManager can only be used inside onEnable/onDisable or a registered event callback.");
                    }

                    if ("register".equals(key)) {
                        if (args.length != 2 || !args[1].canExecute()) {
                            throw new IllegalArgumentException("Usage: EventManager.register(EventType, callbackFunction)");
                        }
                        Object eventTarget = resolveEventTarget(args[0]);
                        Value callback = args[1];

                        switch (eventTarget) {
                            case EventManager.Events eventEnum -> eventManager.register(owner, eventEnum, callback);
                            case Class<?> cls when Event.class.isAssignableFrom(cls) ->
                                //noinspection unchecked
                                    eventManager.register(owner, (Class<? extends Event>) cls, callback);
                            case net.fabricmc.fabric.api.event.Event<?> fabricEvent ->
                                    eventManager.registerFabric(owner, fabricEvent, callback);
                            case null, default ->
                                    throw new IllegalArgumentException("First argument to EventManager.register must be a MQS event class, a Fabric Event object, or an MQS Event from EventManager.Events.");
                        }
                        return null;
                    }

                    if ("unregister".equals(key)) {
                        if (args.length == 0) {
                            eventManager.unregister(owner);
                        } else if (args.length == 1) {
                            Object eventTarget = resolveEventTarget(args[0]);
                            switch (eventTarget) {
                                case EventManager.Events eventEnum -> eventManager.unregister(owner, eventEnum);
                                case Class<?> cls when Event.class.isAssignableFrom(cls) ->
                                    //noinspection unchecked
                                        eventManager.unregister(owner, (Class<? extends Event>) cls);
                                case net.fabricmc.fabric.api.event.Event<?> fabricEvent ->
                                        eventManager.unregister(owner, fabricEvent);
                                case null, default ->
                                        throw new IllegalArgumentException("Argument to EventManager.unregister must be a MQS event class, a Fabric Event object, or an MQS Event from EventManager.Events.");
                            }
                        } else if (args.length == 2 && args[1].canExecute()) {
                            Object eventTarget = resolveEventTarget(args[0]);
                            Value callback = args[1];
                            switch (eventTarget) {
                                case EventManager.Events eventEnum ->
                                        eventManager.unregister(owner, eventEnum, callback);
                                case Class<?> cls when Event.class.isAssignableFrom(cls) ->
                                    //noinspection unchecked
                                        eventManager.unregister(owner, (Class<? extends Event>) cls, callback);
                                case net.fabricmc.fabric.api.event.Event<?> fabricEvent ->
                                        eventManager.unregister(owner, fabricEvent, callback);
                                case null, default ->
                                        throw new IllegalArgumentException("First argument to EventManager.unregister must be a MQS event class, a Fabric Event object, or an MQS Event from EventManager.Events.");
                            }
                        } else {
                            throw new IllegalArgumentException("Invalid arguments for EventManager.unregister");
                        }
                        return null;
                    }

                    throw new UnsupportedOperationException("Unsupported EventManager operation: " + key);
                };
            }

            private Object resolveEventTarget(Value eventTypeArg) {
                if (eventTypeArg == null) {
                    throw new IllegalArgumentException("Event type cannot be null.");
                }

                if (eventTypeArg.isProxyObject()) {
                    Object proxy = eventTypeArg.asProxyObject();
                    if (proxy instanceof JsClassWrapper wrapper) return wrapper.getTargetClass();
                    if (proxy instanceof LazyJsClassHolder holder) return holder.getWrapper().getTargetClass();
                }

                if (eventTypeArg.isHostObject()) {
                    Object hostObject = eventTypeArg.asHostObject();
                    if (hostObject instanceof EventManager.Events) {
                        return hostObject;
                    }
                    if (hostObject instanceof Class) return hostObject;
                    if (hostObject instanceof net.fabricmc.fabric.api.event.Event) return hostObject;
                }

                throw new IllegalArgumentException("Event target must be a class imported via importClass(), a direct Fabric Event object, or an MQS Event from EventManager.Events.");
            }

            @Override
            public Object getMemberKeys() {
                return new String[]{"register", "unregister", "Events"};
            }

            @Override
            public boolean hasMember(String key) {
                return "register".equals(key) || "unregister".equals(key) || "Events".equals(key);
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("Cannot modify the EventManager object.");
            }
        };
    }

    private ProxyObject createEventsEnumProxy() {
        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                try {
                    return EventManager.Events.valueOf(key);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }

            @Override
            public Object getMemberKeys() {
                return Arrays.stream(EventManager.Events.values()).map(Enum::name).toArray(String[]::new);
            }

            @Override
            public boolean hasMember(String key) {
                return Arrays.stream(EventManager.Events.values()).anyMatch(e -> e.name().equals(key));
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("Cannot modify the Events enum object.");
            }
        };
    }

    private static Object toSerializableObject(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isNumber()) {
            return value.as(Number.class);
        }
        if (value.hasArrayElements()) {
            List<Object> javaList = new ArrayList<>();
            for (int i = 0; i < value.getArraySize(); i++) {
                javaList.add(toSerializableObject(value.getArrayElement(i)));
            }
            return javaList;
        }
        if (value.isHostObject()) {
            return value.asHostObject();
        }
        if (value.hasMembers() || value.isProxyObject()) {
            Map<String, Object> javaMap = new LinkedHashMap<>();
            for (String k : value.getMemberKeys()) {
                javaMap.put(k, toSerializableObject(value.getMember(k)));
            }
            return javaMap;
        }
        return value;
    }

    private ProxyObject createConfigProxy() {
        final ConfigManager cm = configManager;

        return new ProxyObject() {
            private RunningScript getCurrentScript() {
                RunningScript script = scriptManager.getCurrentScript();
                if (script == null) {
                    throw new IllegalStateException("Config API can only be used within a running script context (e.g., onEnable, onDisable, or an event).");
                }
                return script;
            }

            @Override
            public Object getMember(String key) {
                return (ProxyExecutable) args -> {
                    RunningScript script = getCurrentScript();
                    switch (key) {
                        case "get": {
                            if (args.length == 0)
                                throw new IllegalArgumentException("Config.get requires at least one argument (key).");
                            String configKey = args[0].asString();
                            Object result = cm.get(script.getId(), configKey);
                            if (result == null) {
                                return args.length > 1 ? args[1] : script.getContext().eval("js", "null");
                            }
                            return script.getContext().asValue(result);
                        }
                        case "set": {
                            if (args.length != 2)
                                throw new IllegalArgumentException("Config.set requires two arguments (key, value).");
                            String configKey = args[0].asString();
                            Object value = toSerializableObject(args[1]);
                            cm.set(script.getId(), configKey, value);
                            return null;
                        }
                        case "has": {
                            if (args.length != 1)
                                throw new IllegalArgumentException("Config.has requires one argument (key).");
                            String configKey = args[0].asString();
                            return cm.get(script.getId(), configKey) != null;
                        }
                        case "save": {
                            if (args.length != 0)
                                throw new IllegalArgumentException("Config.save takes no arguments.");
                            cm.saveConfig(script);
                            return null;
                        }
                        case "load": {
                            if (args.length != 0)
                                throw new IllegalArgumentException("Config.load takes no arguments.");
                            cm.unloadConfig(script);
                            cm.getConfigForScript(script);
                            return null;
                        }
                        case "getAll": {
                            if (args.length != 0)
                                throw new IllegalArgumentException("Config.getAll takes no arguments.");
                            return cm.getConfigForScript(script);
                        }
                        default:
                            throw new UnsupportedOperationException("Unsupported Config operation: " + key);
                    }
                };
            }

            @Override
            public Object getMemberKeys() {
                return new String[]{"get", "set", "has", "save", "load", "getAll"};
            }

            @Override
            public boolean hasMember(String key) {
                return "get".equals(key) || "set".equals(key) || "has".equals(key) || "save".equals(key) || "load".equals(key) || "getAll".equals(key);
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("Cannot modify the Config object itself.");
            }
        };
    }
}
package net.me.scripting.engine;

import net.me.Main;
import net.me.event.Event;
import net.me.event.EventManager;
import net.me.scripting.ConfigManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.commands.CommandsAPI;
import net.me.scripting.module.RunningScript;
import net.me.scripting.wrappers.JsClassWrapper;
import net.me.scripting.wrappers.LazyPackageProxy;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ScriptContextFactory {

    private final ScriptingClassResolver classResolver;

    public ScriptContextFactory(ScriptingClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    public Context createContext(ThreadLocal<Map<String, Value>> perFileExports) {
        Main.LOGGER.info("Creating new script context (ECMAScript 2024)...");
        long startTime = System.currentTimeMillis();

        Context newContext = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(classResolver::isClassAllowed)
                .option("js.ecmascript-version", "2024")
                .option("js.esm-eval-returns-exports", "true")
                .build();

        configureContext(newContext, perFileExports);

        long endTime = System.currentTimeMillis();
        Main.LOGGER.info("New script context created in {}ms.", (endTime - startTime));
        return newContext;
    }

    private void configureContext(Context context, ThreadLocal<Map<String, Value>> perFileExports) {
        registerPackages(context);

        var bindings = context.getBindings("js");

        bindings.putMember("importClass", ScriptingApi.createImportClassProxy(classResolver, context));
        bindings.putMember("extendMapped", ScriptingApi.createExtendMappedProxy(classResolver, context));
        bindings.putMember("wrap", ScriptingApi.createWrapProxy(classResolver));
        bindings.putMember("exportModule", ScriptingApi.createExportModuleProxy(perFileExports));
        bindings.putMember("EventManager", createEventManagerProxy());
        bindings.putMember("ConfigManager", createConfigProxy());

        bindings.putMember("CommandManager", new CommandsAPI());

        bindings.putMember("println", (ProxyExecutable) args -> {
            for (Value arg : args) System.out.println(arg);
            return null;
        });
        bindings.putMember("print", (ProxyExecutable) args -> {
            for (Value arg : args) System.out.print(arg);
            return null;
        });
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
                bindings.putMember(pkg, new LazyPackageProxy(pkg, this.classResolver));
            }
        }
    }

    private ProxyObject createEventManagerProxy() {
        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                return (ProxyExecutable) args -> {
                    RunningScript owner = ScriptManager.getInstance().getCurrentScript();
                    if (owner == null) {
                        throw new IllegalStateException("EventManager can only be used inside onEnable/onDisable or a registered event callback.");
                    }

                    if ("register".equals(key)) {
                        if (args.length != 2 || !args[1].canExecute()) {
                            throw new IllegalArgumentException("Usage: EventManager.register(EventType, callbackFunction)");
                        }
                        Class<? extends Event> eventType = getEventTypeFromValue(args[0]);
                        EventManager.getInstance().register(owner, eventType, args[1]);
                        return null;
                    }

                    if ("unregister".equals(key)) {
                        if (args.length == 0) {
                            EventManager.getInstance().unregister(owner);
                        } else if (args.length == 1) {
                            Class<? extends Event> eventType = getEventTypeFromValue(args[0]);
                            EventManager.getInstance().unregister(owner, eventType);
                        } else if (args.length == 2 && args[1].canExecute()) {
                            Class<? extends Event> eventType = getEventTypeFromValue(args[0]);
                            EventManager.getInstance().unregister(owner, eventType, args[1]);
                        } else {
                            throw new IllegalArgumentException("Invalid arguments for EventManager.unregister");
                        }
                        return null;
                    }

                    throw new UnsupportedOperationException("Unsupported EventManager operation: " + key);
                };
            }

            private Class<? extends Event> getEventTypeFromValue(Value eventTypeArg) {
                if (eventTypeArg == null) {
                    throw new IllegalArgumentException("Event type cannot be null.");
                }

                Class<?> potentialClass = null;

                if (eventTypeArg.isProxyObject() && eventTypeArg.asProxyObject() instanceof JsClassWrapper wrapper) {
                    potentialClass = wrapper.getTargetClass();
                } else if (eventTypeArg.isHostObject() && eventTypeArg.asHostObject() instanceof Class) {
                    potentialClass = eventTypeArg.asHostObject();
                }

                if (potentialClass != null) {
                    if (Event.class.isAssignableFrom(potentialClass)) {
                        //noinspection unchecked
                        return (Class<? extends Event>) potentialClass;
                    } else {
                        throw new IllegalArgumentException("Class " + potentialClass.getName() + " does not extend the base Event class.");
                    }
                }

                throw new IllegalArgumentException("Event type must be a class imported via importClass().");
            }

            @Override
            public Object getMemberKeys() {
                return new String[]{"register", "unregister"};
            }

            @Override
            public boolean hasMember(String key) {
                return "register".equals(key) || "unregister".equals(key);
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("Cannot modify the EventManager object.");
            }
        };
    }

    private ProxyObject createConfigProxy() {
        final ConfigManager cm = ConfigManager.getInstance();

        return new ProxyObject() {
            private RunningScript getCurrentScript() {
                RunningScript script = ScriptManager.getInstance().getCurrentScript();
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
                            Value config = cm.getConfigForScript(script);
                            String configKey = args[0].asString();
                            Value result = config.getMember(configKey);
                            if (result == null || result.isNull()) {
                                return args.length > 1 ? args[1] : script.getJsInstance().getContext().eval("js", "null");
                            }
                            return result;
                        }
                        case "set": {
                            Main.LOGGER.info("Config.set called");
                            if (args.length != 2)
                                throw new IllegalArgumentException("Config.set requires two arguments (key, value).");
                            Value config = cm.getConfigForScript(script);
                            config.putMember(args[0].asString(), args[1]);
                            return null;
                        }
                        case "has": {
                            if (args.length != 1)
                                throw new IllegalArgumentException("Config.has requires one argument (key).");
                            Value config = cm.getConfigForScript(script);
                            return config.hasMember(args[0].asString());
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
                            cm.getConfigForScript(script); // This will force a reload
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
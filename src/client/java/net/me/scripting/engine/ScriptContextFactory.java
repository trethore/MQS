package net.me.scripting.engine;

import net.me.Main;
import net.me.event.EventManager;
import net.me.hooking.HookManager;
import net.me.keybinds.KeybindManager;
import net.me.scripting.ConfigManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.api.*;
import net.me.scripting.commands.CommandAPIService;
import net.me.scripting.extenders.proxies.ExtendedInstanceProxy;
import net.me.scripting.extenders.proxies.MappedInstanceProxy;
import net.me.scripting.wrappers.JsObjectWrapper;
import net.me.scripting.wrappers.LazyPackageProxy;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ScriptContextFactory {

    private final ScriptingClassResolver classResolver;
    private final Engine sharedEngine;
    private final ScriptManager scriptManager;
    private final HookManager hookManager;
    private final ConfigManager configManager;
    private final CommandAPIService commandApiService;
    private final Set<String> standardApiMembers = new HashSet<>();
    private final KeybindManager keybindManager;
    private final EventManager eventManager;

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
        HostAccess hostAccess = HostAccess.newBuilder(HostAccess.ALL)
                .targetTypeMapping(
                        JsObjectWrapper.class,
                        Object.class,
                        (v) -> v.getJavaInstance() != null,
                        JsObjectWrapper::getJavaInstance
                )
                .targetTypeMapping(
                        ExtendedInstanceProxy.class,
                        Object.class,
                        (v) -> v.getBaseInstance() != null,
                        ExtendedInstanceProxy::getBaseInstance
                )
                .targetTypeMapping(
                        MappedInstanceProxy.class,
                        Object.class,
                        (v) -> v.getInstance() != null,
                        MappedInstanceProxy::getInstance
                )
                .build();
        Context newContext = Context.newBuilder("js")
                .engine(sharedEngine)
                .allowHostAccess(hostAccess)
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

        addApiMember(bindings, "EventManager", new EventAPI(this.eventManager, this.scriptManager));
        addApiMember(bindings, "ConfigManager", new ConfigAPI(this.configManager, this.scriptManager));
        addApiMember(bindings, "KeybindManager", new KeybindAPI(this.keybindManager, this.scriptManager));
        addApiMember(bindings, "CommandManager", new CommandsAPI(this.scriptManager, this.commandApiService));
        addApiMember(bindings, "HookManager", new HookAPI(this.hookManager, this.scriptManager));
        addApiMember(bindings, "MQSUtils", new MqsUtilsAPI(this.classResolver));

        addApiMember(bindings, "println", (ProxyExecutable) args -> {
            for (Value arg : args) System.out.println(arg);
            return null;
        });
        addApiMember(bindings, "print", (ProxyExecutable) args -> {
            for (Value arg : args) System.out.print(arg);
            return null;
        });
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
}
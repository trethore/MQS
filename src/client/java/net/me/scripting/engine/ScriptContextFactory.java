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
import net.me.utils.ScriptScheduler;
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
    private final ScriptManager scriptManager;
    private final HookManager hookManager;
    private final ConfigManager configManager;
    private final CommandAPIService commandApiService;
    private final Set<String> standardApiMembers = new HashSet<>();
    private final KeybindManager keybindManager;
    private final EventManager eventManager;
    private final HostAccess hostAccess;
    private final ScriptScheduler scheduler;

    public ScriptContextFactory(ScriptingClassResolver classResolver, Engine sharedEngine, ScriptManager scriptManager, EventManager eventManager, ConfigManager configManager, CommandAPIService commandApiService, HookManager hookManager, KeybindManager keybindManager, ScriptScheduler scheduler) {
        this.classResolver = classResolver;
        this.sharedEngine = sharedEngine;
        this.scriptManager = scriptManager;
        this.configManager = configManager;
        this.commandApiService = commandApiService;
        this.eventManager = eventManager;
        this.hookManager = hookManager;
        this.keybindManager = keybindManager;
        this.scheduler = scheduler;

        this.hostAccess = HostAccess.newBuilder(HostAccess.ALL)
                .targetTypeMapping(
                        JsObjectWrapper.class,
                        Object.class,
                        Objects::nonNull,
                        JsObjectWrapper::getJavaInstance
                )
                .targetTypeMapping(
                        ExtendedInstanceProxy.class,
                        Object.class,
                        Objects::nonNull,
                        ExtendedInstanceProxy::getBaseInstance
                )
                .targetTypeMapping(
                        MappedInstanceProxy.class,
                        Object.class,
                        Objects::nonNull,
                        MappedInstanceProxy::getInstance
                )
                .build();
    }

    public Context createContext(ThreadLocal<Map<String, Value>> perFileExports) {
        Main.LOGGER.info("Creating new script context (ECMAScript 2024)...");
        long startTime = System.currentTimeMillis();

        Context newContext = Context.newBuilder(ScriptConstants.JS)
                .engine(this.sharedEngine)
                .allowHostAccess(this.hostAccess)
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

        Value bindings = context.getBindings(ScriptConstants.JS);

        addApiMember(bindings, "importClass", ScriptingApi.createImportClassProxy(classResolver, context));
        addApiMember(bindings, "extendMapped", ScriptingApi.createExtendMappedProxy(classResolver, context));
        addApiMember(bindings, "isInstanceOf", ScriptingApi.createIsInstanceOfProxy());
        addApiMember(bindings, "wrap", ScriptingApi.createWrapProxy(classResolver));
        addApiMember(bindings, "exportModule", ScriptingApi.createExportModuleProxy(perFileExports));

        Map<String, Object> mqsMembers = new HashMap<>();
        EventsAPI eventsApi = new EventsAPI(this.eventManager, this.scriptManager);
        ConfigsAPI configsApi = new ConfigsAPI(this.configManager, this.scriptManager);
        KeybindsAPI keybindsApi = new KeybindsAPI(this.keybindManager, this.scriptManager);
        CommandsAPI commandsApi = new CommandsAPI(this.scriptManager, this.commandApiService);
        HooksAPI hooksApi = new HooksAPI(this.hookManager, this.scriptManager, this.classResolver);

        mqsMembers.put("events", eventsApi);
        mqsMembers.put("config", configsApi);
        mqsMembers.put("keybinds", keybindsApi);
        mqsMembers.put("cmd", commandsApi);
        mqsMembers.put("hooks", hooksApi);
        mqsMembers.put("utils", new MqsUtilsAPI(this.classResolver, this.scriptManager, this.scheduler));

        addApiMember(bindings, "MQS", ProxyObject.fromMap(mqsMembers));

        addApiMember(bindings, "println", (ProxyExecutable) args -> {
            logScriptOutput(args);
            return null;
        });
        addApiMember(bindings, "print", (ProxyExecutable) args -> {
            logScriptOutput(args);
            return null;
        });
    }

    private void logScriptOutput(Value[] args) {
        if (args == null) {
            return;
        }
        for (Value arg : args) {
            Main.LOGGER.info("{}", arg);
        }
    }

    private void addApiMember(Value bindings, String name, Object member) {
        bindings.putMember(name, member);
        standardApiMembers.add(name);
    }

    public void resetContext(Context context) {
        Value bindings = context.getBindings(ScriptConstants.JS);
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

        Value bindings = context.getBindings(ScriptConstants.JS);
        for (String pkg : topLevelPackages) {
            if (!bindings.hasMember(pkg)) {
                addApiMember(bindings, pkg, new LazyPackageProxy(pkg, this.classResolver));
            }
        }
    }
}

package net.me.scripting.api;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.commands.CommandAPIService;
import net.me.scripting.commands.CommandBuilder;
import net.me.scripting.commands.ScriptArgumentType;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Arrays;

import static net.me.scripting.api.ApiConstants.*;

public class CommandsAPI implements ProxyObject {

    private static final ProxyObject ARG_TYPE_PROXY = createArgTypeProxy();
    private final CommandAPIService service;
    private final ScriptManager scriptManager;

    public CommandsAPI(ScriptManager scriptManager, CommandAPIService service) {
        this.scriptManager = scriptManager;
        this.service = service;
    }

    private static ProxyObject createArgTypeProxy() {
        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                return Arrays.stream(ScriptArgumentType.values())
                        .filter(type -> type.name().equals(key))
                        .findFirst()
                        .map(ScriptArgumentType::toString)
                        .orElse(null);
            }

            @Override
            public Object getMemberKeys() {
                return Arrays.stream(ScriptArgumentType.values()).map(Enum::name).toArray(String[]::new);
            }

            @Override
            public boolean hasMember(String key) {
                return Arrays.stream(ScriptArgumentType.values()).anyMatch(type -> type.name().equals(key));
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("Cannot modify the ArgType object.");
            }
        };
    }

    private RunningScript getCurrentScript() {
        RunningScript script = scriptManager.getCurrentScript();
        if (script == null) {
            throw new IllegalStateException("Commands API can only be used within a running script context (e.g., onEnable, onDisable, or an event).");
        }
        return script;
    }

    @Override
    public Object getMember(String key) {
        if (ARG_TYPE.equals(key)) {
            return ARG_TYPE_PROXY;
        }

        return (ProxyExecutable) args -> {
            RunningScript owner = getCurrentScript();
            switch (key) {
                case BUILDER: {
                    if (args.length != 1 || !args[0].isString())
                        throw new IllegalArgumentException("Commands.builder(name) requires one string argument.");
                    return new CommandBuilder(args[0].asString(), owner, this.scriptManager);
                }
                case LITERAL: {
                    if (args.length != 1 || !args[0].isString()) {
                        throw new IllegalArgumentException("Commands.literal(name) requires one string argument.");
                    }
                    return new CommandBuilder(ClientCommandManager.literal(args[0].asString()), owner, this.scriptManager);
                }
                case ARGUMENT: {
                    if (args.length != 2 || !args[0].isString() || !args[1].isString()) {
                        throw new IllegalArgumentException("Commands.argument(name, type) requires two string arguments.");
                    }
                    String name = args[0].asString();
                    String typeStr = args[1].asString();
                    var type = ScriptArgumentType.fromString(typeStr);
                    return new CommandBuilder(ClientCommandManager.argument(name, type.get()), owner, this.scriptManager);
                }
                case REGISTER: {
                    if (args.length != 1)
                        throw new IllegalArgumentException("Commands.register(builder) requires one argument.");
                    if (!args[0].isHostObject() || !(args[0].asHostObject() instanceof CommandBuilder)) {
                        throw new IllegalArgumentException("Argument must be a CommandBuilder instance.");
                    }
                    CommandBuilder builder = args[0].asHostObject();
                    service.register(owner, builder);
                    return null;
                }
                case UNREGISTER: {
                    if (args.length != 1 || !args[0].isString())
                        throw new IllegalArgumentException("Commands.unregister(commandName) requires one string argument.");
                    String commandName = args[0].asString();
                    service.unregister(owner, commandName);
                    return null;
                }
                case UNREGISTER_ALL: {
                    if (args.length != 0)
                        throw new IllegalArgumentException("Commands.unregisterAll() takes no arguments.");
                    service.unregisterAllFor(owner);
                    return null;
                }
                default:
                    throw new UnsupportedOperationException("Unsupported Commands operation: " + key);
            }
        };
    }

    @Override
    public Object getMemberKeys() {
        return new String[]{BUILDER, LITERAL, ARGUMENT, REGISTER, UNREGISTER, UNREGISTER_ALL, ARG_TYPE};
    }

    @Override
    public boolean hasMember(String key) {
        return BUILDER.equals(key) || LITERAL.equals(key) || ARGUMENT.equals(key) || REGISTER.equals(key) || UNREGISTER.equals(key) || UNREGISTER_ALL.equals(key) || ARG_TYPE.equals(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify the Commands API object.");
    }
}
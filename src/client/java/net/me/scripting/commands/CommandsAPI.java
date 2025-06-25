package net.me.scripting.commands;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Arrays;

public class CommandsAPI implements ProxyObject {

    private final CommandAPIService service = CommandAPIService.getInstance();
    private static final ProxyObject ARG_TYPE_PROXY = createArgTypeProxy();

    private RunningScript getCurrentScript() {
        RunningScript script = ScriptManager.getInstance().getCurrentScript();
        if (script == null) {
            throw new IllegalStateException("Commands API can only be used within a running script context (e.g., onEnable, onDisable, or an event).");
        }
        return script;
    }

    @Override
    public Object getMember(String key) {
        if ("ArgType".equals(key)) {
            return ARG_TYPE_PROXY;
        }

        return (ProxyExecutable) args -> {
            RunningScript owner = getCurrentScript();
            switch (key) {
                case "builder": {
                    if (args.length != 1 || !args[0].isString())
                        throw new IllegalArgumentException("Commands.builder(name) requires one string argument.");
                    return new CommandBuilder(args[0].asString(), owner);
                }
                case "literal": {
                    if (args.length != 1 || !args[0].isString()) {
                        throw new IllegalArgumentException("Commands.literal(name) requires one string argument.");
                    }
                    return new CommandBuilder(ClientCommandManager.literal(args[0].asString()), owner);
                }
                case "argument": {
                    if (args.length != 2 || !args[0].isString() || !args[1].isString()) {
                        throw new IllegalArgumentException("Commands.argument(name, type) requires two string arguments.");
                    }
                    String name = args[0].asString();
                    String typeStr = args[1].asString();
                    var type = ScriptArgumentType.fromString(typeStr);
                    return new CommandBuilder(ClientCommandManager.argument(name, type.get()), owner);
                }
                case "register": {
                    if (args.length != 1)
                        throw new IllegalArgumentException("Commands.register(builder) requires one argument.");
                    if (!args[0].isHostObject() || !(args[0].asHostObject() instanceof CommandBuilder)) {
                        throw new IllegalArgumentException("Argument must be a CommandBuilder instance.");
                    }
                    CommandBuilder builder = args[0].asHostObject();
                    service.register(owner, builder);
                    return null;
                }
                case "unregister": {
                    if (args.length != 1 || !args[0].isString())
                        throw new IllegalArgumentException("Commands.unregister(commandName) requires one string argument.");
                    String commandName = args[0].asString();
                    service.unregister(owner, commandName);
                    return null;
                }
                case "unregisterAll": {
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

    @Override
    public Object getMemberKeys() {
        return new String[]{"builder", "literal", "argument", "register", "unregister", "unregisterAll", "ArgType"};
    }

    @Override
    public boolean hasMember(String key) {
        return "builder".equals(key) || "literal".equals(key) || "argument".equals(key) || "register".equals(key) || "unregister".equals(key) || "unregisterAll".equals(key) || "ArgType".equals(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify the Commands API object.");
    }
}
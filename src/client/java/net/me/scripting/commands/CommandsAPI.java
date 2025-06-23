package net.me.scripting.commands;

import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

public class CommandsAPI implements ProxyObject {

    private final CommandAPIService service = CommandAPIService.getInstance();

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
            Value jsObject = Value.asValue(new Object());
            for(ScriptArgumentType type : ScriptArgumentType.values()) {
                jsObject.putMember(type.name(), type.toString());
            }
            return jsObject;
        }

        return (ProxyExecutable) args -> {
            RunningScript owner = getCurrentScript();
            switch (key) {
                case "builder": {
                    if (args.length != 1 || !args[0].isString()) throw new IllegalArgumentException("Commands.builder(name) requires one string argument.");
                    String name = args[0].asString();
                    return new CommandBuilder(name, owner);
                }
                case "register": {
                    if (args.length != 1) throw new IllegalArgumentException("Commands.register(builder) requires one argument.");
                    if (!args[0].isHostObject() || !(args[0].asHostObject() instanceof CommandBuilder)) {
                        throw new IllegalArgumentException("Argument must be a CommandBuilder instance.");
                    }
                    CommandBuilder builder = args[0].asHostObject();
                    service.register(owner, builder);
                    return null;
                }
                case "unregister": {
                    if (args.length != 1 || !args[0].isString()) throw new IllegalArgumentException("Commands.unregister(commandName) requires one string argument.");
                    String commandName = args[0].asString();
                    service.unregister(owner, commandName);
                    return null;
                }
                case "unregisterAll": {
                    if (args.length != 0) throw new IllegalArgumentException("Commands.unregisterAll() takes no arguments.");
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
        return new String[]{"builder", "register", "unregister", "unregisterAll", "ArgType"};
    }

    @Override
    public boolean hasMember(String key) {
        return "builder".equals(key) || "register".equals(key) || "unregister".equals(key) || "unregisterAll".equals(key) || "ArgType".equals(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify the Commands API object.");
    }
}
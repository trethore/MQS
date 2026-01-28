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

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.api.internal.HandleTracker;
import net.me.scripting.api.internal.ScriptContextHelper;
import net.me.scripting.commands.CommandAPIService;
import net.me.scripting.commands.CommandBuilder;
import net.me.scripting.commands.ScriptArgumentType;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Arrays;
import java.util.Set;

import static net.me.scripting.api.ApiConstants.*;

public class CommandsAPI implements ProxyObject {

    private static final Set<String> MEMBER_KEYS = Set.of(
            BUILDER,
            LITERAL,
            ARGUMENT,
            REGISTER,
            UNREGISTER,
            UNREGISTER_ALL,
            ARG_TYPE,
            REGISTER_LITERAL
    );
    private static final ProxyObject ARG_TYPE_PROXY = createArgTypeProxy();
    private final CommandAPIService service;
    private final ScriptManager scriptManager;
    private final ScriptContextHelper contextHelper;
    private final HandleTracker<String> commandTracker;

    public CommandsAPI(ScriptManager scriptManager, CommandAPIService service) {
        this.scriptManager = scriptManager;
        this.service = service;
        this.contextHelper = new ScriptContextHelper(scriptManager);
        this.commandTracker = new HandleTracker<>();
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
    public Object getMember(String key) {
        if (ARG_TYPE.equals(key)) {
            return ARG_TYPE_PROXY;
        }

        return (ProxyExecutable) args -> executeCommand(key, args, contextHelper.require("Commands API"));
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
        throw new UnsupportedOperationException("Cannot modify the Commands API object.");
    }

    private Object executeCommand(String key, Value[] args, RunningScript owner) {
        return switch (key) {
            case BUILDER -> buildNamed(args, owner);
            case LITERAL -> buildLiteral(args, owner);
            case ARGUMENT -> buildArgument(args, owner);
            case REGISTER -> register(args, owner);
            case UNREGISTER -> unregister(args, owner);
            case UNREGISTER_ALL -> unregisterAll(args, owner);
            case REGISTER_LITERAL -> registerLiteral(args, owner);
            default -> throw new UnsupportedOperationException("Unsupported Commands operation: " + key);
        };
    }

    private CommandBuilder buildNamed(Value[] args, RunningScript owner) {
        ApiArgumentChecks.requireArgCount(args, 1, "Commands.builder(name) requires one string argument.");
        String name = ApiArgumentChecks.requireString(args, 0, "Commands.builder(name) requires one string argument.");
        return new CommandBuilder(name, owner, this.scriptManager);
    }

    private CommandBuilder buildLiteral(Value[] args, RunningScript owner) {
        ApiArgumentChecks.requireArgCountAtLeast(args, 1, "Commands.literal(name) requires one string argument.");
        String name = ApiArgumentChecks.requireString(args, 0, "Commands.literal(name) requires one string argument.");
        CommandBuilder builder = new CommandBuilder(ClientCommandManager.literal(name), owner, this.scriptManager);
        if (args.length > 1) {
            Value configure = args[1];
            if (configure != null && configure.canExecute()) {
                contextHelper.executeWithScript(owner, () -> {
                    Value jsInstance = owner.getJsInstance();
                    configure.invokeMember("call", jsInstance, builder);
                });
            }
        }
        return builder;
    }

    private CommandBuilder buildArgument(Value[] args, RunningScript owner) {
        ApiArgumentChecks.requireArgCount(args, 2, COMMAND_ARGUMENT_USAGE);
        String name = ApiArgumentChecks.requireString(args, 0, COMMAND_ARGUMENT_USAGE);
        String typeStr = ApiArgumentChecks.requireString(args, 1, COMMAND_ARGUMENT_USAGE);
        ScriptArgumentType type = ScriptArgumentType.fromString(typeStr);
        return new CommandBuilder(ClientCommandManager.argument(name, type.get()), owner, this.scriptManager);
    }

    private Value register(Value[] args, RunningScript owner) {
        ApiArgumentChecks.requireArgCount(args, 1, "Commands.register(builder) requires one argument.");
        CommandBuilder builder = ApiArgumentChecks.requireHostObject(args, 0, CommandBuilder.class, "Argument must be a CommandBuilder instance.");
        service.register(owner, builder);
        String commandName = builder.getRootBuilder().getLiteral();
        commandTracker.track(owner, commandName);
        return contextHelper.createIdempotentDisposer(owner, () -> {
            service.unregister(owner, commandName);
            commandTracker.remove(owner, commandName);
        });
    }

    private Void unregister(Value[] args, RunningScript owner) {
        ApiArgumentChecks.requireArgCount(args, 1, "Commands.unregister(commandName) requires one string argument.");
        String commandName = ApiArgumentChecks.requireString(args, 0, "Commands.unregister(commandName) requires one string argument.");
        service.unregister(owner, commandName);
        commandTracker.remove(owner, commandName);
        return null;
    }

    private Void unregisterAll(Value[] args, RunningScript owner) {
        ApiArgumentChecks.requireArgCount(args, 0, "Commands.unregisterAll() takes no arguments.");
        service.unregisterAllFor(owner);
        commandTracker.disposeAll(owner, _ -> {
        });
        return null;
    }

    private Value registerLiteral(Value[] args, RunningScript owner) {
        ApiArgumentChecks.requireArgCountAtLeast(args, 2, "Commands.registerLiteral(name, handler) requires a name and handler.");
        String name = ApiArgumentChecks.requireString(args, 0, "Commands.registerLiteral(name, handler) requires a name and handler.");
        Value handler = ApiArgumentChecks.requireExecutable(args, 1, "Handler must be executable.");
        CommandBuilder builder = new CommandBuilder(name, owner, scriptManager);
        builder.executes(handler);
        service.register(owner, builder);
        commandTracker.track(owner, name);
        return contextHelper.createIdempotentDisposer(owner, () -> {
            service.unregister(owner, name);
            commandTracker.remove(owner, name);
        });
    }
}

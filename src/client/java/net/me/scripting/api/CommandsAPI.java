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

package net.me.scripting.api;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.api.internal.HandleTracker;
import net.me.scripting.api.internal.ScriptContextHelper;
import net.me.scripting.commands.CommandAPIService;
import net.me.scripting.commands.CommandBuilder;
import net.me.scripting.commands.ScriptArgumentType;
import net.me.scripting.module.RunningScript;
import net.me.scripting.typings.MqsApiFragment;
import net.me.scripting.typings.TypingsConstants;
import net.me.scripting.typings.schema.TsMember;
import net.me.scripting.typings.schema.TsObject;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static net.me.scripting.typings.schema.TsDescriptors.*;

public class CommandsAPI implements ProxyObject {

    private static final String LIT = "lit";
    private static final String ARG = "arg";
    private static final String REG = "reg";
    private static final String UNREG = "unreg";
    private static final String CLEAR = "clear";
    private static final String TYPES = "types";
    private static final String QUICK = "quick";
    private static final String ARG_USAGE = "cmd.arg(name, type) requires two string arguments.";
    private static final String COMMAND_BUILDER_TYPE = "MQSCommandBuilder";

    private static final Set<String> MEMBER_KEYS = Set.of(
            LIT,
            ARG,
            REG,
            UNREG,
            CLEAR,
            TYPES,
            QUICK
    );
    private static final ProxyObject TYPES_PROXY = createTypesProxy();
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

    public static MqsApiFragment describeTypeScript() {
        return new MqsApiFragment(
                List.of(
                        alias("MQSCommandSuggestions", TypingsConstants.STRING + " | " + TypingsConstants.STRING + "[] | ArrayLike<" + TypingsConstants.UNKNOWN + "> | Promise<" + TypingsConstants.STRING + " | " + TypingsConstants.STRING + "[] | ArrayLike<" + TypingsConstants.UNKNOWN + "> | null> | null"),
                        alias("MQSCommandSuggestionProvider", "((context: MQSCommandContext) => MQSCommandSuggestions) | MQSCommandSuggestions")
                ),
                List.of(),
                List.of(),
                List.of(
                        describeCommandContext(),
                        describeCommandBuilder(),
                        describeCommandTypes(),
                        describeCommandApi()
                )
        );
    }

    private static TsObject describeCommandContext() {
        return new TsObject(
                "MQSCommandContext",
                List.of(
                        method("source", fn("any")),
                        method("arg", fn(TypingsConstants.UNKNOWN, p("name", TypingsConstants.STRING))),
                        method("str", fn(TypingsConstants.STRING, p("name", TypingsConstants.STRING)))
                )
        );
    }

    private static TsObject describeCommandBuilder() {
        return new TsObject(
                COMMAND_BUILDER_TYPE,
                List.of(
                        method("then", fn(COMMAND_BUILDER_TYPE, p("child", COMMAND_BUILDER_TYPE))),
                        method(LIT, fn(COMMAND_BUILDER_TYPE, p("name", TypingsConstants.STRING))),
                        method(ARG, fn(COMMAND_BUILDER_TYPE, p("name", TypingsConstants.STRING), p("typeName", TypingsConstants.STRING))),
                        method("run", fn(COMMAND_BUILDER_TYPE, p("handler", "(context: MQSCommandContext) => " + TypingsConstants.UNKNOWN))),
                        method("suggest", fn(COMMAND_BUILDER_TYPE, p("suggestions", "MQSCommandSuggestionProvider")))
                )
        );
    }

    private static TsObject describeCommandTypes() {
        List<TsMember> members = new ArrayList<>();
        for (ScriptArgumentType argumentType : ScriptArgumentType.values()) {
            String typeName = argumentType.toString();
            members.add(ro(typeName, "\"" + typeName + "\""));
        }
        return new TsObject("MQSCommandTypes", List.copyOf(members));
    }

    private static TsObject describeCommandApi() {
        return new TsObject(
                "MQSCommandApi",
                List.of(
                        ro(TYPES, "MQSCommandTypes"),
                        method(LIT, fn(COMMAND_BUILDER_TYPE, p("name", TypingsConstants.STRING), opt("configure", "(builder: " + COMMAND_BUILDER_TYPE + ") => " + TypingsConstants.UNKNOWN))),
                        method(ARG, fn(COMMAND_BUILDER_TYPE, p("name", TypingsConstants.STRING), p("type", TypingsConstants.STRING))),
                        method(REG, fn(TypingsConstants.MQS_DISPOSER, p("builder", COMMAND_BUILDER_TYPE))),
                        method(UNREG, fn(TypingsConstants.VOID, p("commandName", TypingsConstants.STRING))),
                        method(CLEAR, fn(TypingsConstants.VOID)),
                        method(QUICK, fn(TypingsConstants.MQS_DISPOSER, p("name", TypingsConstants.STRING), p("handler", "(context: MQSCommandContext) => " + TypingsConstants.UNKNOWN)))
                )
        );
    }

    private static ProxyObject createTypesProxy() {
        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                ScriptArgumentType type = ScriptArgumentType.resolve(key);
                return type != null ? type.toString() : null;
            }

            @Override
            public Object getMemberKeys() {
                return Arrays.stream(ScriptArgumentType.values())
                        .map(ScriptArgumentType::toString)
                        .toArray(String[]::new);
            }

            @Override
            public boolean hasMember(String key) {
                return ScriptArgumentType.resolve(key) != null;
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("Cannot modify cmd.types.");
            }
        };
    }

    @Override
    public Object getMember(String key) {
        if (TYPES.equals(key)) {
            return TYPES_PROXY;
        }
        if (!MEMBER_KEYS.contains(key)) {
            return null;
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
            case LIT -> buildLiteral(args, owner);
            case ARG -> buildArgument(args, owner);
            case REG -> register(args, owner);
            case UNREG -> unregister(args, owner);
            case CLEAR -> clear(args, owner);
            case QUICK -> quick(args, owner);
            default -> throw new UnsupportedOperationException("Unsupported Commands operation: " + key);
        };
    }

    private CommandBuilder buildLiteral(Value[] args, RunningScript owner) {
        ApiArgumentChecks.requireArgCountAtLeast(args, 1, "cmd.lit(name) requires one string argument.");
        String name = ApiArgumentChecks.requireString(args, 0, "cmd.lit(name) requires one string argument.");
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
        ApiArgumentChecks.requireArgCount(args, 2, ARG_USAGE);
        String name = ApiArgumentChecks.requireString(args, 0, ARG_USAGE);
        String typeStr = ApiArgumentChecks.requireString(args, 1, ARG_USAGE);
        ScriptArgumentType type = ScriptArgumentType.fromString(typeStr);
        return new CommandBuilder(ClientCommandManager.argument(name, type.get()), owner, this.scriptManager);
    }

    private Value register(Value[] args, RunningScript owner) {
        ApiArgumentChecks.requireArgCount(args, 1, "cmd.reg(builder) requires one argument.");
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
        ApiArgumentChecks.requireArgCount(args, 1, "cmd.unreg(commandName) requires one string argument.");
        String commandName = ApiArgumentChecks.requireString(args, 0, "cmd.unreg(commandName) requires one string argument.");
        service.unregister(owner, commandName);
        commandTracker.remove(owner, commandName);
        return null;
    }

    private Void clear(Value[] args, RunningScript owner) {
        ApiArgumentChecks.requireArgCount(args, 0, "cmd.clear() takes no arguments.");
        service.unregisterAllFor(owner);
        commandTracker.disposeAll(owner, ignored -> {
        });
        return null;
    }

    private Value quick(Value[] args, RunningScript owner) {
        ApiArgumentChecks.requireArgCountAtLeast(args, 2, "cmd.quick(name, handler) requires a name and handler.");
        String name = ApiArgumentChecks.requireString(args, 0, "cmd.quick(name, handler) requires a name and handler.");
        Value handler = ApiArgumentChecks.requireExecutable(args, 1, "Handler must be executable.");
        CommandBuilder builder = new CommandBuilder(name, owner, scriptManager);
        builder.run(handler);
        service.register(owner, builder);
        commandTracker.track(owner, name);
        return contextHelper.createIdempotentDisposer(owner, () -> {
            service.unregister(owner, name);
            commandTracker.remove(owner, name);
        });
    }
}

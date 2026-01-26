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
import net.me.scripting.commands.CommandAPIService;
import net.me.scripting.commands.CommandBuilder;
import net.me.scripting.commands.ScriptArgumentType;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.me.scripting.api.ApiConstants.*;

public class CommandsHelperAPI implements ProxyObject {
    private static final Set<String> MEMBER_KEYS = Set.of(
            LITERAL,
            ARGUMENT,
            REGISTER,
            REGISTER_LITERAL
    );

    private final ScriptManager scriptManager;
    private final CommandAPIService commandApiService;

    public CommandsHelperAPI(ScriptManager scriptManager, CommandAPIService commandApiService) {
        this.scriptManager = scriptManager;
        this.commandApiService = commandApiService;
    }

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case LITERAL -> createLiteralExecutable();
            case ARGUMENT -> createArgumentExecutable();
            case REGISTER -> createRegisterExecutable();
            case REGISTER_LITERAL -> createRegisterLiteralExecutable();
            default -> null;
        };
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
        throw new UnsupportedOperationException("Cannot modify MQS.commands.");
    }

    private ProxyExecutable createLiteralExecutable() {
        return args -> {
            String name = ApiArgumentChecks.requireString(args, 0, "commands.literal requires a command name.");
            RunningScript owner = getCurrentScript();
            CommandBuilder builder = new CommandBuilder(name, owner, scriptManager);
            if (args.length > 1) {
                Value configure = args[1];
                if (configure != null && configure.canExecute()) {
                    invokeWithScript(owner, configure, builder);
                }
            }
            return builder;
        };
    }

    private ProxyExecutable createArgumentExecutable() {
        return args -> {
            ApiArgumentChecks.requireArgCount(args, 2, COMMAND_ARGUMENT_USAGE);
            String name = ApiArgumentChecks.requireString(args, 0, COMMAND_ARGUMENT_USAGE);
            String typeName = ApiArgumentChecks.requireString(args, 1, COMMAND_ARGUMENT_USAGE);
            RunningScript owner = getCurrentScript();
            ScriptArgumentType type = ScriptArgumentType.fromString(typeName);
            return new CommandBuilder(ClientCommandManager.argument(name, type.get()), owner, scriptManager);
        };
    }

    private ProxyExecutable createRegisterExecutable() {
        return args -> {
            ApiArgumentChecks.requireArgCount(args, 1, "commands.register expects a CommandBuilder instance.");
            CommandBuilder builder = ApiArgumentChecks.requireHostObject(args, 0, CommandBuilder.class, "commands.register expects a CommandBuilder instance.");
            RunningScript owner = getCurrentScript();
            commandApiService.register(owner, builder);
            return createDisposer(owner, builder.getRootBuilder().getLiteral());
        };
    }

    private ProxyExecutable createRegisterLiteralExecutable() {
        return args -> {
            ApiArgumentChecks.requireArgCountAtLeast(args, 2, "commands.registerLiteral(name, handler) requires a name and handler.");
            String name = ApiArgumentChecks.requireString(args, 0, "commands.registerLiteral(name, handler) requires a name and handler.");
            Value handler = ApiArgumentChecks.requireExecutable(args, 1, "Handler must be executable.");
            RunningScript owner = getCurrentScript();
            CommandBuilder builder = new CommandBuilder(name, owner, scriptManager);
            builder.executes(handler);
            commandApiService.register(owner, builder);
            return createDisposer(owner, builder.getRootBuilder().getLiteral());
        };
    }

    private Value createDisposer(RunningScript owner, String commandName) {
        AtomicBoolean disposed = new AtomicBoolean(false);
        ProxyExecutable exec = _ -> {
            if (disposed.compareAndSet(false, true)) {
                commandApiService.unregister(owner, commandName);
            }
            return null;
        };
        return owner.getContext().asValue(exec);
    }

    private void invokeWithScript(RunningScript owner, Value configure, CommandBuilder builder) {
        RunningScript previous = scriptManager.getCurrentScript();
        scriptManager.setCurrentScript(owner);
        try {
            Value jsInstance = owner.getJsInstance();
            configure.invokeMember("call", jsInstance, builder);
        } finally {
            if (previous != null) {
                scriptManager.setCurrentScript(previous);
            } else {
                scriptManager.clearCurrentScript();
            }
        }
    }

    private RunningScript getCurrentScript() {
        RunningScript script = scriptManager.getCurrentScript();
        if (script == null) {
            throw new IllegalStateException("Commands helper can only be used from an active script.");
        }
        return script;
    }
}

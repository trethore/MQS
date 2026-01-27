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

package net.me.scripting.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.Main;
import net.me.command.CommandManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CommandBuilder {
    private final ArgumentBuilder<FabricClientCommandSource, ?> builder;
    private final RunningScript owner;
    private final ScriptManager scriptManager;

    public CommandBuilder(String name, RunningScript owner, ScriptManager scriptManager) {
        this(ClientCommandManager.literal(name), owner, scriptManager);
    }

    public CommandBuilder(ArgumentBuilder<FabricClientCommandSource, ?> builder, RunningScript owner, ScriptManager scriptManager) {
        this.builder = builder;
        this.owner = owner;
        this.scriptManager = scriptManager;
    }

    @HostAccess.Export
    public CommandBuilder then(CommandBuilder childCommandBuilder) {
        this.builder.then(childCommandBuilder.builder);
        return this;
    }

    @HostAccess.Export
    public CommandBuilder executes(Value callback) {
        this.builder.executes(context -> {
            RunningScript previous = scriptManager.getCurrentScript();
            scriptManager.setCurrentScript(owner);
            try {
                Value jsInstance = owner.getJsInstance();
                callback.invokeMember("call", jsInstance, new JSCommandContext(context));
            } catch (Exception e) {
                Main.LOGGER.error("Error executing command in script '{}': {}", owner.getName(), e.getMessage(), e);
            } finally {
                if (previous != null) {
                    scriptManager.setCurrentScript(previous);
                } else {
                    scriptManager.clearCurrentScript();
                }
            }
            return CommandManager.COMMAND_SUCCESS;
        });
        return this;
    }

    @HostAccess.Export
    public CommandBuilder exec(Value callback) {
        return executes(callback);
    }


    @HostAccess.Export
    public CommandBuilder suggests(Value callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Suggestion source cannot be null.");
        }

        RequiredArgumentBuilder<FabricClientCommandSource, ?> argBuilder = requireArgumentBuilder();
        argBuilder.suggests((context, suggestionsBuilder) -> {
            RunningScript previous = scriptManager.getCurrentScript();
            scriptManager.setCurrentScript(this.owner);
            try {
                Value jsInstance = this.owner.getJsInstance();
                Value result = callback.canExecute()
                        ? callback.invokeMember("call", jsInstance, new JSCommandContext(context))
                        : callback;

                Value resolved = awaitIfPromise(result);
                appendSuggestions(resolved, suggestionsBuilder);
            } catch (Throwable t) {
                Main.LOGGER.error("Error executing suggestion provider for script '{}'", this.owner.getName(), t);
            } finally {
                if (previous != null) {
                    scriptManager.setCurrentScript(previous);
                } else {
                    scriptManager.clearCurrentScript();
                }
            }
            return suggestionsBuilder.buildFuture();
        });
        return this;
    }

    @HostAccess.Export
    public CommandBuilder suggest(Value callback) {
        return suggests(callback);
    }

    private Value awaitIfPromise(Value value) {
        if (value == null || !value.hasMember("then")) {
            return value;
        }

        try {
            CompletableFuture<Value> future = new CompletableFuture<>();
            value.invokeMember("then", (ProxyExecutable) args -> {
                Value resolved = args != null && args.length > 0 ? args[0] : null;
                future.complete(resolved);
                return null;
            });
            if (value.hasMember("catch")) {
                value.invokeMember("catch", (ProxyExecutable) args -> {
                    if (!future.isDone()) {
                        future.complete(null);
                    }
                    return null;
                });
            }
            return future.join();
        } catch (Exception e) {
            Main.LOGGER.error("Failed to await promise suggestions for script '{}'", this.owner.getName(), e);
            return null;
        }
    }

    private void appendSuggestions(Value suggestions, SuggestionsBuilder builder) {
        if (suggestions == null) {
            return;
        }
        if (appendArraySuggestions(suggestions, builder)) {
            return;
        }
        if (appendStringSuggestion(suggestions, builder)) {
            return;
        }
        appendIterableSuggestions(suggestions, builder);
    }

    private boolean appendArraySuggestions(Value suggestions, SuggestionsBuilder builder) {
        if (!suggestions.hasArrayElements()) {
            return false;
        }
        for (long i = 0; i < suggestions.getArraySize(); i++) {
            Value element = suggestions.getArrayElement(i);
            if (element != null && element.isString()) {
                builder.suggest(element.asString());
            }
        }
        return true;
    }

    private boolean appendStringSuggestion(Value suggestions, SuggestionsBuilder builder) {
        if (!suggestions.isString()) {
            return false;
        }
        builder.suggest(suggestions.asString());
        return true;
    }

    private void appendIterableSuggestions(Value suggestions, SuggestionsBuilder builder) {
        if (!(suggestions.isHostObject() && suggestions.asHostObject() instanceof Iterable<?> iterable)) {
            return;
        }
        for (Object entry : iterable) {
            if (entry != null) {
                builder.suggest(entry.toString());
            }
        }
    }

    private RequiredArgumentBuilder<FabricClientCommandSource, ?> requireArgumentBuilder() {
        if (!(this.builder instanceof RequiredArgumentBuilder<?, ?> rawArgBuilder)) {
            throw new IllegalStateException("'.suggests()' can only be called on an argument node created with '.arg()'");
        }
        @SuppressWarnings("unchecked")
        RequiredArgumentBuilder<FabricClientCommandSource, ?> argBuilder =
                (RequiredArgumentBuilder<FabricClientCommandSource, ?>) rawArgBuilder;
        return argBuilder;
    }

    public LiteralArgumentBuilder<FabricClientCommandSource> getRootBuilder() {
        if (builder instanceof LiteralArgumentBuilder<?> rawLiteral) {
            @SuppressWarnings("unchecked")
            LiteralArgumentBuilder<FabricClientCommandSource> literalBuilder = (LiteralArgumentBuilder<FabricClientCommandSource>) rawLiteral;
            return literalBuilder;
        }
        throw new IllegalStateException("The root of a command must be a literal. Current builder is: " + builder.getClass().getSimpleName());
    }
}

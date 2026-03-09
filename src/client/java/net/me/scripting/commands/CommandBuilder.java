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

package net.me.scripting.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
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

@SuppressWarnings({"unused", "UnusedReturnValue"})
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
    public CommandBuilder lit(String name) {
        CommandBuilder child = new CommandBuilder(ClientCommandManager.literal(name), owner, scriptManager);
        this.builder.then(child.builder);
        return child;
    }

    @HostAccess.Export
    public CommandBuilder arg(String name, String typeName) {
        ScriptArgumentType type = ScriptArgumentType.fromString(typeName);
        CommandBuilder child = new CommandBuilder(ClientCommandManager.argument(name, type.get()), owner, scriptManager);
        this.builder.then(child.builder);
        return child;
    }

    @HostAccess.Export
    public CommandBuilder run(Value callback) {
        if (callback == null || !callback.canExecute()) {
            throw new IllegalArgumentException("Command handler must be executable.");
        }
        this.builder.executes(context -> {
            withScriptContext(() -> invokeCallback(callback, context));
            return CommandManager.COMMAND_SUCCESS;
        });
        return this;
    }

    @HostAccess.Export
    public CommandBuilder suggest(Value callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Suggestion source cannot be null.");
        }

        RequiredArgumentBuilder<FabricClientCommandSource, ?> argBuilder = requireArgumentBuilder();
        argBuilder.suggests((context, suggestionsBuilder) -> {
            CompletableFuture<Suggestions> suggestionsFuture =
                    withScriptContext(() -> resolveSuggestions(callback, context, suggestionsBuilder));
            return suggestionsFuture != null
                    ? suggestionsFuture
                    : CompletableFuture.completedFuture(suggestionsBuilder.build());
        });
        return this;
    }

    private void withScriptContext(Runnable action) {
        withScriptContext(() -> {
            action.run();
            return null;
        });
    }

    private <T> T withScriptContext(ScriptAction<T> action) {
        RunningScript previous = scriptManager.getCurrentScript();
        scriptManager.setCurrentScript(owner);
        try {
            return action.run();
        } catch (Exception e) {
            Main.LOGGER.error("Error in script '{}': {}", owner.getName(), e.getMessage(), e);
            return null;
        } finally {
            restoreScriptContext(previous);
        }
    }

    private void restoreScriptContext(RunningScript previous) {
        if (previous != null) {
            scriptManager.setCurrentScript(previous);
        } else {
            scriptManager.clearCurrentScript();
        }
    }

    private void invokeCallback(Value callback, CommandContext<FabricClientCommandSource> context) {
        Value jsInstance = owner.getJsInstance();
        callback.invokeMember("call", jsInstance, new JSCommandContext(context));
    }

    private Value invokeOrReturn(Value callback, CommandContext<FabricClientCommandSource> context) {
        if (!callback.canExecute()) {
            return callback;
        }
        Value jsInstance = owner.getJsInstance();
        return callback.invokeMember("call", jsInstance, new JSCommandContext(context));
    }

    private CompletableFuture<Suggestions> resolveSuggestions(Value callback,
                                                              CommandContext<FabricClientCommandSource> context,
                                                              SuggestionsBuilder suggestionsBuilder) {
        try {
            Value result = invokeOrReturn(callback, context);
            if (!isPromise(result)) {
                appendSuggestions(result, suggestionsBuilder);
                return CompletableFuture.completedFuture(suggestionsBuilder.build());
            }
            return attachPromiseHandlers(result, suggestionsBuilder);
        } catch (Exception e) {
            Main.LOGGER.error("Failed to resolve suggestions for script '{}'", owner.getName(), e);
            return CompletableFuture.completedFuture(suggestionsBuilder.build());
        }
    }

    private boolean isPromise(Value value) {
        return value != null && value.hasMember("then");
    }

    private CompletableFuture<Suggestions> attachPromiseHandlers(Value promise,
                                                                 SuggestionsBuilder suggestionsBuilder) {
        CompletableFuture<Suggestions> future = new CompletableFuture<>();
        promise.invokeMember("then", (ProxyExecutable) args -> {
            withScriptContext(() -> {
                try {
                    Value resolved = args != null && args.length > 0 ? args[0] : null;
                    appendSuggestions(resolved, suggestionsBuilder);
                } catch (Exception e) {
                    Main.LOGGER.error("Failed to append async suggestions for script '{}'", owner.getName(), e);
                } finally {
                    completeSuggestions(future, suggestionsBuilder);
                }
            });
            return null;
        });

        if (promise.hasMember("catch")) {
            promise.invokeMember("catch", (ProxyExecutable) args -> {
                withScriptContext(() -> Main.LOGGER.error("Failed to resolve async suggestions for script '{}'", owner.getName()));
                completeSuggestions(future, suggestionsBuilder);
                return null;
            });
        }
        return future;
    }

    private void completeSuggestions(CompletableFuture<Suggestions> future, SuggestionsBuilder suggestionsBuilder) {
        if (!future.isDone()) {
            future.complete(suggestionsBuilder.build());
        }
    }

    @FunctionalInterface
    private interface ScriptAction<T> {
        T run();
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
            throw new IllegalStateException("'.suggest()' can only be called on an argument node created with '.arg()'");
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

package net.me.scripting.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.command.CommandManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CommandBuilder {
    private final ArgumentBuilder<FabricClientCommandSource, ?> builder;
    private final RunningScript owner;

    public CommandBuilder(String name, RunningScript owner) {
        this.builder = ClientCommandManager.literal(name);
        this.owner = owner;
    }

    private CommandBuilder(ArgumentBuilder<FabricClientCommandSource, ?> builder, RunningScript owner) {
        this.builder = builder;
        this.owner = owner;
    }

    @HostAccess.Export
    public CommandBuilder then(String literal) {
        var childBuilder = ClientCommandManager.literal(literal);
        this.builder.then(childBuilder);
        return new CommandBuilder(childBuilder, owner);
    }

    @HostAccess.Export
    public CommandBuilder arg(String name, String typeStr) {
        var type = ScriptArgumentType.fromString(typeStr);
        var childBuilder = ClientCommandManager.argument(name, type.get());
        this.builder.then(childBuilder);
        return new CommandBuilder(childBuilder, owner);
    }

    @HostAccess.Export
    public CommandBuilder executes(Value callback) {
        this.builder.executes(context -> {
            ScriptManager sm = ScriptManager.getInstance();
            sm.setCurrentScript(owner);
            try {
                callback.execute(new JSCommandContext(context));
            } finally {
                sm.clearCurrentScript();
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
        if (this.builder instanceof RequiredArgumentBuilder<?,?> rawArgBuilder) {
            @SuppressWarnings("unchecked")
            RequiredArgumentBuilder<FabricClientCommandSource, ?>
                    argBuilder = (RequiredArgumentBuilder<FabricClientCommandSource, ?>) rawArgBuilder;

            argBuilder.suggests((context, suggestionsBuilder) -> {
                ScriptManager sm = ScriptManager.getInstance();
                sm.setCurrentScript(owner);
                try {
                    Value result = callback.execute(new JSCommandContext(context), suggestionsBuilder);
                    if (result != null && result.isHostObject()) {
                        Object host = result.asHostObject();
                        if (host instanceof CompletableFuture<?>) {
                            @SuppressWarnings("unchecked")
                            CompletableFuture<Suggestions> future = (CompletableFuture<Suggestions>) host;
                            return future;
                        }
                    }
                } finally {
                    sm.clearCurrentScript();
                }
                return suggestionsBuilder.buildFuture();
            });
            return this;
        } else {
            throw new IllegalStateException("'.suggests()' can only be called on an argument node created with '.arg()'");
        }
    }

    @HostAccess.Export
    public CommandBuilder suggest(Value callback) {
        return suggests(callback);
    }

    public LiteralArgumentBuilder<FabricClientCommandSource> getRootBuilder() {
        if (builder instanceof LiteralArgumentBuilder<?> rawLiteral) {
            @SuppressWarnings("unchecked")
            LiteralArgumentBuilder<FabricClientCommandSource>
                    literalBuilder = (LiteralArgumentBuilder<FabricClientCommandSource>) rawLiteral;
            return literalBuilder;
        }
        throw new IllegalStateException("The root of a command must be a literal. Current builder is: "
                + builder.getClass().getSimpleName());
    }
}

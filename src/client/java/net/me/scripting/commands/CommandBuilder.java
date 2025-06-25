package net.me.scripting.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.Main;
import net.me.command.CommandManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

@SuppressWarnings("unused")
public class CommandBuilder {
    private final ArgumentBuilder<FabricClientCommandSource, ?> builder;
    private final RunningScript owner;

    public CommandBuilder(String name, RunningScript owner) {
        this(ClientCommandManager.literal(name), owner);
    }

    CommandBuilder(ArgumentBuilder<FabricClientCommandSource, ?> builder, RunningScript owner) {
        this.builder = builder;
        this.owner = owner;
    }

    @HostAccess.Export
    public CommandBuilder then(CommandBuilder childCommandBuilder) {
        this.builder.then(childCommandBuilder.builder);
        return this;
    }

    @HostAccess.Export
    public CommandBuilder executes(Value callback) {
        this.builder.executes(context -> {
            ScriptManager sm = ScriptManager.getInstance();
            sm.setCurrentScript(owner);
            try {
                Value jsInstance = owner.getJsInstance();
                callback.invokeMember("call", jsInstance, new JSCommandContext(context));
            } catch (Exception e) {
                Main.LOGGER.error("Error executing command in script '{}': {}", owner.getName(), e.getMessage(), e);
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
        if (this.builder instanceof RequiredArgumentBuilder<?, ?> rawArgBuilder) {
            @SuppressWarnings("unchecked")
            RequiredArgumentBuilder<FabricClientCommandSource, ?> argBuilder =
                    (RequiredArgumentBuilder<FabricClientCommandSource, ?>) rawArgBuilder;

            argBuilder.suggests((context, builder) -> {
                ScriptManager sm = ScriptManager.getInstance();
                sm.setCurrentScript(this.owner);
                try {
                    Value jsInstance = this.owner.getJsInstance();
                    Value result = callback.invokeMember("call", jsInstance);

                    if (result != null && result.hasArrayElements()) {
                        String input = builder.getRemainingLowerCase();
                        for (long i = 0; i < result.getArraySize(); i++) {
                            Value element = result.getArrayElement(i);
                            if (element.isString()) {
                                String suggestion = element.asString();
                                if (suggestion.toLowerCase().startsWith(input)) {
                                    builder.suggest(suggestion);
                                }
                            }
                        }
                    }
                } catch (Throwable t) { // Catch Throwable for maximum safety
                    Main.LOGGER.error("Error executing suggestion provider for script '{}'", this.owner.getName(), t);
                } finally {
                    sm.clearCurrentScript();
                }
                return builder.buildFuture();
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
            LiteralArgumentBuilder<FabricClientCommandSource> literalBuilder = (LiteralArgumentBuilder<FabricClientCommandSource>) rawLiteral;
            return literalBuilder;
        }
        throw new IllegalStateException("The root of a command must be a literal. Current builder is: " + builder.getClass().getSimpleName());
    }
}
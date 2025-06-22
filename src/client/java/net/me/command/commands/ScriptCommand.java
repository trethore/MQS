package net.me.command.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.command.CommandManager;
import net.me.command.MQSCommand;
import net.me.scripting.ScriptingService;
import net.me.scripting.module.RunningScript;
import net.me.scripting.module.ScriptDescriptor;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ScriptCommand extends MQSCommand {

    private final ScriptingService scriptingService = new ScriptingService();

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("script")
                .then(ClientCommandManager.literal("list")
                        .executes(this::listScripts))
                .then(ClientCommandManager.literal("enable")
                        .then(ClientCommandManager.argument("script_id", StringArgumentType.greedyString())
                                .suggests(this::suggestDisabledScripts)
                                .executes(this::enableScript)))
                .then(ClientCommandManager.literal("disable")
                        .then(ClientCommandManager.argument("script_id", StringArgumentType.greedyString())
                                .suggests(this::suggestEnabledScripts)
                                .executes(this::disableScript)))
                .then(ClientCommandManager.literal("reload")
                        .then(ClientCommandManager.argument("script_id", StringArgumentType.greedyString())
                                .suggests(this::suggestEnabledScripts)
                                .executes(this::reloadScript)));
    }

    private int listScripts(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§a--- Available Scripts ---"));
        for (ScriptDescriptor descriptor : scriptingService.listAvailable()) {
            boolean isRunning = scriptingService.isRunning(descriptor.getId());
            Text status = isRunning ? Text.literal("§a[ENABLED]") : Text.literal("§c[DISABLED]");
            context.getSource().sendFeedback(Text.literal(" - " + descriptor.moduleName() + " (" + descriptor.getId() + ") ").append(status));
        }
        return CommandManager.COMMAND_SUCCESS;
    }

    private int enableScript(CommandContext<FabricClientCommandSource> context) {
        String scriptId = StringArgumentType.getString(context, "script_id");
        scriptingService.enable(scriptId);
        context.getSource().sendFeedback(Text.literal("Attempting to enable script: " + scriptId));
        return CommandManager.COMMAND_SUCCESS;
    }

    private int disableScript(CommandContext<FabricClientCommandSource> context) {
        String scriptId = StringArgumentType.getString(context, "script_id");
        scriptingService.disable(scriptId);
        context.getSource().sendFeedback(Text.literal("Disabled script: " + scriptId));
        return CommandManager.COMMAND_SUCCESS;
    }

    private int reloadScript(CommandContext<FabricClientCommandSource> context) {
        String scriptId = StringArgumentType.getString(context, "script_id");
        scriptingService.disable(scriptId);
        scriptingService.enable(scriptId);
        context.getSource().sendFeedback(Text.literal("Reloaded script: " + scriptId));
        return CommandManager.COMMAND_SUCCESS;
    }

    private CompletableFuture<Suggestions> suggestEnabledScripts(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        scriptingService.listRunning().stream()
                .map(RunningScript::getId)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestDisabledScripts(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        var runningIds = scriptingService.listRunning().stream()
                .map(RunningScript::getId)
                .collect(Collectors.toSet());
        scriptingService.listAvailable().stream()
                .map(ScriptDescriptor::getId)
                .filter(id -> !runningIds.contains(id))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}
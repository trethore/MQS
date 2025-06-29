package net.me.command.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.command.Command;
import net.me.command.CommandManager;
import net.me.scripting.ScriptingService;
import net.me.scripting.module.RunningScript;
import net.me.scripting.module.ScriptDescriptor;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ScriptCommand extends Command {
    private final ScriptingService scriptingService;

    public ScriptCommand(ScriptingService scriptingService) {
        this.scriptingService = scriptingService;
    }

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
                                .executes(this::reloadScript)))
                .then(ClientCommandManager.literal("refresh")
                        .executes(this::refreshScripts))
                .then(ClientCommandManager.literal("refreshandreenable")
                        .executes(this::refreshAndReenableScripts))
                .then(ClientCommandManager.literal("save")
                        .then(ClientCommandManager.argument("script_id", StringArgumentType.greedyString())
                                .suggests(this::suggestEnabledScripts)
                                .executes(this::saveScriptConfig)))
                .then(ClientCommandManager.literal("saveall")
                        .executes(this::saveAllScriptConfigs));
    }

    private int listScripts(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§a--- Available Scripts ---"));
        String scriptList = scriptingService.getFormattedScriptList();
        for (String line : scriptList.split("\n")) {
            context.getSource().sendFeedback(Text.literal(line));
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

    private int refreshScripts(CommandContext<FabricClientCommandSource> context) {
        scriptingService.refresh();
        context.getSource().sendFeedback(Text.literal("Scripts refreshed. All running scripts have been disabled."));
        return CommandManager.COMMAND_SUCCESS;
    }

    private int refreshAndReenableScripts(CommandContext<FabricClientCommandSource> context) {
        scriptingService.refreshAndReenable();
        context.getSource().sendFeedback(Text.literal("Scripts refreshed and previously running scripts were re-enabled."));
        return CommandManager.COMMAND_SUCCESS;
    }

    private int saveScriptConfig(CommandContext<FabricClientCommandSource> context) {
        String scriptId = StringArgumentType.getString(context, "script_id");
        boolean success = scriptingService.save(scriptId);
        if (success) {
            context.getSource().sendFeedback(Text.literal("Saved config for script: " + scriptId));
        } else {
            context.getSource().sendError(Text.literal("Could not save config. Script not running or not found: " + scriptId));
        }
        return success ? CommandManager.COMMAND_SUCCESS : CommandManager.COMMAND_FAILURE;
    }

    private int saveAllScriptConfigs(CommandContext<FabricClientCommandSource> context) {
        int count = scriptingService.saveAll();
        if (count > 0) {
            context.getSource().sendFeedback(Text.literal("Saved configs for all " + count + " running scripts."));
        } else {
            context.getSource().sendFeedback(Text.literal("No running scripts to save configs for."));
        }
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
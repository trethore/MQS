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
import net.me.utils.ChatUtils;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ScriptCommand extends Command {
    private static final String SCRIPT_ID_ARG = "script_id";
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
                        .then(ClientCommandManager.argument(SCRIPT_ID_ARG, StringArgumentType.greedyString())
                                .suggests(this::suggestDisabledScripts)
                                .executes(this::enableScript)))
                .then(ClientCommandManager.literal("disable")
                        .then(ClientCommandManager.argument(SCRIPT_ID_ARG, StringArgumentType.greedyString())
                                .suggests(this::suggestEnabledScripts)
                                .executes(this::disableScript)))
                .then(ClientCommandManager.literal("reload")
                        .then(ClientCommandManager.argument(SCRIPT_ID_ARG, StringArgumentType.greedyString())
                                .suggests(this::suggestEnabledScripts)
                                .executes(this::reloadScript)))
                .then(ClientCommandManager.literal("refresh")
                        .executes(this::refreshScripts))
                .then(ClientCommandManager.literal("refreshandreenable")
                        .executes(this::refreshAndReenableScripts))
                .then(ClientCommandManager.literal("save")
                        .then(ClientCommandManager.argument(SCRIPT_ID_ARG, StringArgumentType.greedyString())
                                .suggests(this::suggestEnabledScripts)
                                .executes(this::saveScriptConfig)))
                .then(ClientCommandManager.literal("saveall")
                        .executes(this::saveAllScriptConfigs));
    }

    private int listScripts(CommandContext<FabricClientCommandSource> context) {
        ChatUtils.addSuccessChatMessage("--- Available Scripts ---", true);
        String scriptList = scriptingService.getFormattedScriptList();
        for (String line : scriptList.split("\n")) {
            ChatUtils.addRawMessage(line);
        }
        return CommandManager.COMMAND_SUCCESS;
    }

    private int enableScript(CommandContext<FabricClientCommandSource> context) {
        String scriptId = StringArgumentType.getString(context, SCRIPT_ID_ARG);
        scriptingService.enable(scriptId);
        ChatUtils.addInfoChatMessage("Attempting to enable script: " + scriptId, true);
        return CommandManager.COMMAND_SUCCESS;
    }

    private int disableScript(CommandContext<FabricClientCommandSource> context) {
        String scriptId = StringArgumentType.getString(context, SCRIPT_ID_ARG);
        scriptingService.disable(scriptId);
        ChatUtils.addSuccessChatMessage("Disabled script: " + scriptId, true);
        return CommandManager.COMMAND_SUCCESS;
    }

    private int reloadScript(CommandContext<FabricClientCommandSource> context) {
        String scriptId = StringArgumentType.getString(context, SCRIPT_ID_ARG);
        scriptingService.disable(scriptId);
        scriptingService.enable(scriptId);
        ChatUtils.addSuccessChatMessage("Reloaded script: " + scriptId, true);
        return CommandManager.COMMAND_SUCCESS;
    }

    private int refreshScripts(CommandContext<FabricClientCommandSource> context) {
        scriptingService.refresh();
        ChatUtils.addInfoChatMessage("Scripts refreshed. All running scripts have been disabled.", true);
        return CommandManager.COMMAND_SUCCESS;
    }

    private int refreshAndReenableScripts(CommandContext<FabricClientCommandSource> context) {
        scriptingService.refreshAndReenable();
        ChatUtils.addSuccessChatMessage("Scripts refreshed and previously running scripts were re-enabled.", true);
        return CommandManager.COMMAND_SUCCESS;
    }

    private int saveScriptConfig(CommandContext<FabricClientCommandSource> context) {
        String scriptId = StringArgumentType.getString(context, SCRIPT_ID_ARG);
        boolean success = scriptingService.save(scriptId);
        if (success) {
            ChatUtils.addSuccessChatMessage("Saved config for script: " + scriptId, true);
        } else {
            ChatUtils.addErrorChatMessage("Could not save config. Script not running or not found: " + scriptId, true);
        }
        return success ? CommandManager.COMMAND_SUCCESS : CommandManager.COMMAND_FAILURE;
    }

    private int saveAllScriptConfigs(CommandContext<FabricClientCommandSource> context) {
        int count = scriptingService.saveAll();
        if (count > 0) {
            ChatUtils.addSuccessChatMessage("Saved configs for all " + count + " running scripts.", true);
        } else {
            ChatUtils.addInfoChatMessage("No running scripts to save configs for.", true);
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
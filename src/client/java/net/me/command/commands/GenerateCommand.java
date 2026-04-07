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

package net.me.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.Main;
import net.me.command.Command;
import net.me.command.CommandManager;
import net.me.scripting.typings.TypeDefinitionGenerator;
import net.me.scripting.typings.TypeDefinitionGenerator.GenerationTarget;
import net.me.utils.ChatUtils;
import net.me.utils.PathUtils;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GenerateCommand extends Command {
    private final TypeDefinitionGenerator typeDefinitionGenerator;

    public GenerateCommand(TypeDefinitionGenerator typeDefinitionGenerator) {
        this.typeDefinitionGenerator = typeDefinitionGenerator;
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("generate")
                .executes(context -> executeGenerate(context, GenerationTarget.BOTH))
                .then(ClientCommandManager.literal("api")
                        .executes(context -> executeGenerate(context, GenerationTarget.API)))
                .then(ClientCommandManager.literal("mc")
                        .executes(context -> executeGenerate(context, GenerationTarget.MC)));
    }

    private int executeGenerate(CommandContext<FabricClientCommandSource> context, GenerationTarget target) {
        if (!typeDefinitionGenerator.isReady(target)) {
            ChatUtils.addErrorChatMessage("Mappings are still loading. Try /mqs generate again in a moment.", true);
            return CommandManager.COMMAND_FAILURE;
        }

        ChatUtils.addInfoChatMessage(buildGenerationMessage(target), true);
        Minecraft client = context.getSource().getClient();

        CompletableFuture.runAsync(() -> generateDefinitions(client, target))
                .exceptionally(exception -> {
                    Main.LOGGER.atError().setCause(exception).log(
                            "Unexpected failure while generating TypeScript definitions for target {}.",
                            target
                    );
                    client.execute(() -> ChatUtils.addErrorChatMessage("Failed to generate TypeScript definitions. Check logs for details.", true));
                    return null;
                });
        return CommandManager.COMMAND_SUCCESS;
    }

    private void generateDefinitions(Minecraft client, GenerationTarget target) {
        try {
            List<TypeDefinitionGenerator.GenerationResult> results = typeDefinitionGenerator.generate(target);
            client.execute(() -> {
                ChatUtils.addSuccessChatMessage(buildSuccessMessage(target), true);
                for (TypeDefinitionGenerator.GenerationResult result : results) {
                    ChatUtils.addInfoChatMessage("File: " + formatPath(result.outputPath()), false);
                    if (result.target() == GenerationTarget.MC) {
                        ChatUtils.addInfoChatMessage(
                                "Classes: " + result.classCount() + ", methods: " + result.methodCount() + ", fields: " + result.fieldCount(),
                                false
                        );
                    }
                }
                ChatUtils.addInfoChatMessage("File: " + formatPath(typeDefinitionGenerator.getTsConfigPath()), false);
            });
        } catch (IOException exception) {
            Main.LOGGER.atError().setCause(exception).log(
                    "Failed to generate TypeScript definitions for target {}.",
                    target
            );
            client.execute(() -> ChatUtils.addErrorChatMessage("Failed to generate TypeScript definitions. Check logs for details.", true));
        }
    }

    private String buildGenerationMessage(GenerationTarget target) {
        return switch (target) {
            case API -> "Generating TypeScript definitions for MQS API...";
            case MC -> "Generating TypeScript definitions for Minecraft mappings...";
            case BOTH -> "Generating TypeScript definitions for MQS API and Minecraft mappings...";
        };
    }

    private String buildSuccessMessage(GenerationTarget target) {
        return switch (target) {
            case API -> "Generated MQS API TypeScript definitions.";
            case MC -> "Generated Minecraft mappings TypeScript definitions.";
            case BOTH -> "Generated MQS API and Minecraft mappings TypeScript definitions.";
        };
    }

    private String formatPath(Path outputPath) {
        return PathUtils.formatPathRelativeToModDir(outputPath);
    }
}

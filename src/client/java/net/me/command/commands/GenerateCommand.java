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
import net.me.utils.ChatUtils;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class GenerateCommand extends Command {
    private final TypeDefinitionGenerator typeDefinitionGenerator;

    public GenerateCommand(TypeDefinitionGenerator typeDefinitionGenerator) {
        this.typeDefinitionGenerator = typeDefinitionGenerator;
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("generate")
                .executes(this::executeGenerate);
    }

    private int executeGenerate(CommandContext<FabricClientCommandSource> context) {
        if (!typeDefinitionGenerator.isReady()) {
            ChatUtils.addErrorChatMessage("Mappings are still loading. Try /mqs generate again in a moment.", true);
            return CommandManager.COMMAND_FAILURE;
        }

        ChatUtils.addInfoChatMessage("Generating TypeScript definitions for MQS and Minecraft mappings...", true);
        Minecraft client = context.getSource().getClient();

        CompletableFuture.runAsync(() -> generateDefinitions(client));
        return CommandManager.COMMAND_SUCCESS;
    }

    private void generateDefinitions(Minecraft client) {
        try {
            TypeDefinitionGenerator.GenerationResult result = typeDefinitionGenerator.generate();
            client.execute(() -> {
                ChatUtils.addSuccessChatMessage("Generated TypeScript definitions.", true);
                ChatUtils.addInfoChatMessage("File: " + formatPath(result.outputPath()), false);
                ChatUtils.addInfoChatMessage(
                        "Classes: " + result.classCount() + ", methods: " + result.methodCount() + ", fields: " + result.fieldCount(),
                        false
                );
            });
        } catch (Exception exception) {
            Main.LOGGER.error("Failed to generate TypeScript definitions.", exception);
            client.execute(() -> ChatUtils.addErrorChatMessage("Failed to generate TypeScript definitions. Check logs for details.", true));
        }
    }

    private String formatPath(Path outputPath) {
        Path normalizedOutput = outputPath.toAbsolutePath().normalize();
        Path normalizedModDir = Main.MOD_DIR.toAbsolutePath().normalize();
        if (normalizedOutput.startsWith(normalizedModDir)) {
            Path relativePath = normalizedModDir.relativize(normalizedOutput);
            return Main.MOD_ID + "/" + relativePath.toString().replace('\\', '/');
        }
        return normalizedOutput.toString();
    }
}

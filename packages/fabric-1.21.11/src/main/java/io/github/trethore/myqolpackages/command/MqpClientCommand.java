/*
 * My QOL Packages - Client-side Minecraft modding at runtime
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
package io.github.trethore.myqolpackages.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.trethore.myqolpackages.api.MqpRuntime;
import io.github.trethore.myqolpackages.command.commands.PackagesClientCommand;
import io.github.trethore.myqolpackages.command.commands.TrustClientCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class MqpClientCommand {
    private final PackagesClientCommand packagesClientCommand;
    private final TrustClientCommand trustClientCommand;

    public MqpClientCommand(MqpRuntime runtime) {
        packagesClientCommand = new PackagesClientCommand(runtime.getPackageManager());
        trustClientCommand = new TrustClientCommand(runtime.getPackageManager());
    }

    public void register() {
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> dispatcher.register(buildCommand()));
    }

    private LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("mqp")
                .executes(this::execute)
                .then(packagesClientCommand.buildCommand())
                .then(trustClientCommand.buildCommand());
    }

    private int execute(CommandContext<FabricClientCommandSource> context) {
        MqpCommandFeedback.sendInfo(context.getSource(), "Available actions: packages, trust.");
        return ClientCommandResult.SUCCESS;
    }
}

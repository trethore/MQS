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
package io.github.trethore.myqolpackages.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.command.ClientCommandResult;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public final class PackagesClientCommand {
  private final InfoPackageClientCommand infoPackageClientCommand;
  private final ListPackagesClientCommand listPackagesClientCommand;
  private final RefreshPackagesClientCommand refreshPackagesClientCommand;

  public PackagesClientCommand(PackageManager packageManager) {
    infoPackageClientCommand = new InfoPackageClientCommand(packageManager);
    listPackagesClientCommand = new ListPackagesClientCommand(packageManager);
    refreshPackagesClientCommand = new RefreshPackagesClientCommand(packageManager);
  }

  public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
    return ClientCommandManager.literal("packages")
        .executes(this::execute)
        .then(listPackagesClientCommand.buildCommand())
        .then(infoPackageClientCommand.buildCommand())
        .then(refreshPackagesClientCommand.buildCommand());
  }

  private int execute(CommandContext<FabricClientCommandSource> context) {
    context
        .getSource()
        .sendFeedback(Component.literal("Use list, info, or refresh to manage MQP packages."));
    return ClientCommandResult.SUCCESS;
  }
}

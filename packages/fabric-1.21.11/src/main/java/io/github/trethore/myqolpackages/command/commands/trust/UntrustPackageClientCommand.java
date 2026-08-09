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
package io.github.trethore.myqolpackages.command.commands.trust;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.api.packages.PackageOperationResult;
import io.github.trethore.myqolpackages.command.ClientCommandResult;
import io.github.trethore.myqolpackages.command.MqpCommandFeedback;
import io.github.trethore.myqolpackages.command.commands.PackageCommandSupport;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class UntrustPackageClientCommand {
  private final PackageManager packageManager;

  public UntrustPackageClientCommand(PackageManager packageManager) {
    this.packageManager = packageManager;
  }

  public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
    return ClientCommandManager.literal("untrust")
        .then(
            ClientCommandManager.argument("id", StringArgumentType.word())
                .suggests(
                    (context, builder) ->
                        PackageCommandSupport.suggestPackageIds(
                            builder, packageManager.getTrustedPackageIds(), Function.identity()))
                .executes(this::execute));
  }

  private int execute(CommandContext<FabricClientCommandSource> context) {
    String packageId = StringArgumentType.getString(context, "id");
    PackageOperationResult result = packageManager.untrustPackage(packageId);
    PackageCommandSupport.sendDiagnostics(context.getSource(), result.diagnostics());
    if (!result.successful()) {
      return ClientCommandResult.FAILURE;
    }
    MqpCommandFeedback.sendInfo(
        context.getSource(), packageId + " is now untrusted, disabled, and stopped");
    return ClientCommandResult.SUCCESS;
  }
}

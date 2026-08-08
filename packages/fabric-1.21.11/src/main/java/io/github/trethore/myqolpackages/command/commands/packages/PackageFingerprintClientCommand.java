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
package io.github.trethore.myqolpackages.command.commands.packages;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.trethore.myqolpackages.api.packages.PackageInfo;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.api.packages.PackageOperationResult;
import io.github.trethore.myqolpackages.command.ClientCommandResult;
import io.github.trethore.myqolpackages.command.MqpCommandFeedback;
import io.github.trethore.myqolpackages.command.commands.trust.TrustPackageClientCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class PackageFingerprintClientCommand {
  private final PackageManager packageManager;
  private final TrustPackageClientCommand trustPackageClientCommand;

  public PackageFingerprintClientCommand(
      PackageManager packageManager, TrustPackageClientCommand trustPackageClientCommand) {
    this.packageManager = packageManager;
    this.trustPackageClientCommand = trustPackageClientCommand;
  }

  public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
    return ClientCommandManager.literal("fingerprint")
        .then(
            ClientCommandManager.literal("accept")
                .then(
                    ClientCommandManager.argument("id", StringArgumentType.word())
                        .suggests(
                            (context, builder) ->
                                PackageCommandSupport.suggestPackageIds(
                                    builder, packageManager.getPackages(), PackageInfo::id))
                        .executes(this::executeAccept)));
  }

  public LiteralArgumentBuilder<FabricClientCommandSource> buildAcceptCallbackCommand() {
    return ClientCommandManager.literal("_accept-fingerprint")
        .then(
            ClientCommandManager.argument("token", StringArgumentType.word())
                .executes(
                    context ->
                        trustPackageClientCommand.acceptFingerprint(
                            context.getSource(), StringArgumentType.getString(context, "token"))));
  }

  private int executeAccept(CommandContext<FabricClientCommandSource> context) {
    String packageId = StringArgumentType.getString(context, "id");
    PackageOperationResult result = packageManager.acceptPackageFingerprint(packageId, null);
    PackageCommandSupport.sendDiagnostics(context.getSource(), result.diagnostics());
    if (!result.successful()) {
      return ClientCommandResult.FAILURE;
    }
    MqpCommandFeedback.sendInfo(context.getSource(), "Accepted fingerprint for " + packageId);
    return ClientCommandResult.SUCCESS;
  }
}

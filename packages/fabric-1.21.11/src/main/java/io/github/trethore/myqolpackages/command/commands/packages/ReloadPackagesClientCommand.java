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
import io.github.trethore.myqolpackages.api.packages.PackageDiscoveryResult;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.api.packages.PackageOperationResult;
import io.github.trethore.myqolpackages.command.ClientCommandResult;
import io.github.trethore.myqolpackages.command.MqpCommandFeedback;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class ReloadPackagesClientCommand {
  private final PackageManager packageManager;

  public ReloadPackagesClientCommand(PackageManager packageManager) {
    this.packageManager = packageManager;
  }

  public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
    return ClientCommandManager.literal("reload")
        .executes(this::executeAll)
        .then(
            ClientCommandManager.argument("id", StringArgumentType.word())
                .suggests(
                    (context, builder) ->
                        PackageCommandSupport.suggestPackageIds(
                            builder,
                            packageManager.getConfiguredEnabledPackageIds(),
                            Function.identity()))
                .executes(this::executePackage));
  }

  private int executeAll(CommandContext<FabricClientCommandSource> context) {
    FabricClientCommandSource source = context.getSource();
    PackageDiscoveryResult result = packageManager.reload();
    MqpCommandFeedback.sendInfo(
        source,
        "Reloaded: "
            + result.packages().size()
            + " package(s), "
            + result.diagnostics().size()
            + " diagnostic(s).");
    return PackageCommandSupport.sendDiagnostics(source, result.diagnostics());
  }

  private int executePackage(CommandContext<FabricClientCommandSource> context) {
    FabricClientCommandSource source = context.getSource();
    String packageId = StringArgumentType.getString(context, "id");
    PackageOperationResult result = packageManager.reloadPackage(packageId);
    if (result.successful()) {
      MqpCommandFeedback.sendInfo(source, "Reloaded " + packageId + ".");
      return ClientCommandResult.SUCCESS;
    }
    return PackageCommandSupport.sendDiagnostics(source, result.diagnostics());
  }
}

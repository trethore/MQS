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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.api.packages.PackageOperationResult;
import io.github.trethore.myqolpackages.command.ClientCommandResult;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class DisablePackageClientCommand {
  private final PackageManager packageManager;

  public DisablePackageClientCommand(PackageManager packageManager) {
    this.packageManager = packageManager;
  }

  public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
    return ClientCommandManager.literal("disable")
        .then(
            ClientCommandManager.argument("id", StringArgumentType.word())
                .suggests((context, builder) -> suggestPackageIds(builder))
                .executes(this::execute));
  }

  private int execute(CommandContext<FabricClientCommandSource> context) {
    FabricClientCommandSource source = context.getSource();
    String packageId = StringArgumentType.getString(context, "id");
    PackageOperationResult result = packageManager.disablePackage(packageId);
    if (result.successful()) {
      PackageCommandSupport.sendDisabled(source, packageId);
      return ClientCommandResult.SUCCESS;
    }
    return PackageCommandSupport.sendDiagnostics(source, result.diagnostics());
  }

  private CompletableFuture<Suggestions> suggestPackageIds(SuggestionsBuilder builder) {
    return PackageCommandSupport.suggestPackageIds(
        builder, packageManager.getConfiguredEnabledPackageIds(), Function.identity());
  }
}

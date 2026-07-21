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
import io.github.trethore.myqolpackages.api.packages.PackageInfo;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.command.ClientCommandResult;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public final class InfoPackageClientCommand {
  private final PackageManager packageManager;

  public InfoPackageClientCommand(PackageManager packageManager) {
    this.packageManager = packageManager;
  }

  public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
    return ClientCommandManager.literal("info")
        .then(
            ClientCommandManager.argument("id", StringArgumentType.word())
                .suggests((context, builder) -> suggestPackageIds(builder))
                .executes(this::execute));
  }

  private int execute(CommandContext<FabricClientCommandSource> context) {
    FabricClientCommandSource source = context.getSource();
    String packageId = StringArgumentType.getString(context, "id");
    Optional<PackageInfo> optionalPackage = packageManager.findPackage(packageId);
    if (optionalPackage.isEmpty()) {
      source.sendError(Component.literal("Unknown MQP package: " + packageId));
      return ClientCommandResult.FAILURE;
    }

    PackageInfo packageInfo = optionalPackage.get();
    source.sendFeedback(Component.literal(packageInfo.name() + " (" + packageInfo.id() + ")"));
    source.sendFeedback(Component.literal("Version: " + packageInfo.version()));
    source.sendFeedback(Component.literal("Description: " + packageInfo.description()));
    source.sendFeedback(Component.literal("Entrypoint: " + packageInfo.entrypoint()));
    return ClientCommandResult.SUCCESS;
  }

  private CompletableFuture<Suggestions> suggestPackageIds(SuggestionsBuilder builder) {
    String remaining = builder.getRemainingLowerCase();
    for (PackageInfo packageInfo : packageManager.getPackages()) {
      if (packageInfo.id().toLowerCase(Locale.ROOT).contains(remaining)) {
        builder.suggest(packageInfo.id());
      }
    }
    return builder.buildFuture();
  }
}

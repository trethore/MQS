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

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.trethore.myqolpackages.api.packages.PackageDiagnostic;
import io.github.trethore.myqolpackages.api.packages.PackageState;
import io.github.trethore.myqolpackages.command.ClientCommandResult;
import io.github.trethore.myqolpackages.command.MqpCommandFeedback;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

final class PackageCommandSupport {
  private PackageCommandSupport() {}

  static int sendDiagnostics(
      FabricClientCommandSource source, List<PackageDiagnostic> diagnostics) {
    for (PackageDiagnostic diagnostic : diagnostics) {
      MqpCommandFeedback.sendError(source, diagnostic.packageId() + ": " + diagnostic.message());
    }
    return diagnostics.isEmpty() ? ClientCommandResult.SUCCESS : ClientCommandResult.FAILURE;
  }

  static Component formatState(PackageState state) {
    return Component.literal(state.name().toLowerCase(Locale.ROOT)).withStyle(stateColor(state));
  }

  static void sendEnabled(FabricClientCommandSource source, String packageId) {
    MqpCommandFeedback.sendInfo(
        source, createStateChange("Enabled", ChatFormatting.GREEN, packageId));
  }

  static void sendDisabled(FabricClientCommandSource source, String packageId) {
    MqpCommandFeedback.sendInfo(
        source, createStateChange("Disabled", ChatFormatting.RED, packageId));
  }

  static <T> CompletableFuture<Suggestions> suggestPackageIds(
      SuggestionsBuilder builder, Iterable<T> packages, Function<T, String> idProvider) {
    String remaining = builder.getRemainingLowerCase();
    for (T packageValue : packages) {
      String packageId = idProvider.apply(packageValue);
      if (packageId.toLowerCase(Locale.ROOT).contains(remaining)) {
        builder.suggest(packageId);
      }
    }
    return builder.buildFuture();
  }

  private static Component createStateChange(
      String action, ChatFormatting color, String packageId) {
    return Component.empty()
        .append(Component.literal(action).withStyle(color))
        .append(Component.literal(": " + packageId));
  }

  private static ChatFormatting stateColor(PackageState state) {
    return switch (state) {
      case ENABLED -> ChatFormatting.GREEN;
      case DISABLED, ERROR -> ChatFormatting.RED;
    };
  }
}

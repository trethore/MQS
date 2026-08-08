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
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.trethore.myqolpackages.api.packages.PackageInfo;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.command.ClientCommandResult;
import io.github.trethore.myqolpackages.command.MqpCommandFeedback;
import io.github.trethore.myqolpackages.command.commands.PackageCommandSupport;
import java.nio.file.Path;
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
      MqpCommandFeedback.sendError(source, "Unknown package: " + packageId);
      return ClientCommandResult.FAILURE;
    }

    PackageInfo packageInfo = optionalPackage.get();
    MqpCommandFeedback.sendHeader(source);
    MqpCommandFeedback.sendLine(source, packageInfo.name() + " (" + packageInfo.id() + ")");
    MqpCommandFeedback.sendLine(source, "Version: " + packageInfo.version());
    MqpCommandFeedback.sendLine(source, "Description: " + packageInfo.description());
    MqpCommandFeedback.sendLine(source, "Entrypoint: " + packageInfo.entrypoint());
    MqpCommandFeedback.sendLine(
        source,
        Component.empty()
            .append(Component.literal("State: "))
            .append(PackageCommandSupport.formatState(packageInfo.state())));
    MqpCommandFeedback.sendLine(
        source,
        Component.empty()
            .append(Component.literal("Trust: "))
            .append(PackageCommandSupport.formatTrustState(packageInfo.trust().state())));
    if (packageInfo.trust().trustedVersions() != null) {
      MqpCommandFeedback.sendLine(
          source, "Trusted versions: " + packageInfo.trust().trustedVersions());
    }
    MqpCommandFeedback.sendLine(
        source,
        Component.empty()
            .append(Component.literal("Fingerprint: "))
            .append(PackageCommandSupport.formatFingerprint(packageInfo.trust())));
    MqpCommandFeedback.sendLine(
        source, "Directory: " + anonymizeDirectory(packageInfo.packageDirectory()));
    return ClientCommandResult.SUCCESS;
  }

  private static String anonymizeDirectory(Path directory) {
    String userHome = System.getProperty("user.home");
    if (userHome == null || userHome.isBlank()) {
      return directory.toString();
    }

    Path normalizedDirectory = directory.toAbsolutePath().normalize();
    Path normalizedUserHome = Path.of(userHome).toAbsolutePath().normalize();
    if (!normalizedDirectory.startsWith(normalizedUserHome)) {
      return directory.toString();
    }

    Path anonymizedUserHome = normalizedUserHome.resolveSibling("user");
    return anonymizedUserHome
        .resolve(normalizedUserHome.relativize(normalizedDirectory))
        .toString();
  }

  private CompletableFuture<Suggestions> suggestPackageIds(SuggestionsBuilder builder) {
    return PackageCommandSupport.suggestPackageIds(
        builder, packageManager.getPackages(), PackageInfo::id);
  }
}

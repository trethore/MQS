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
import io.github.trethore.myqolpackages.api.packages.FingerprintMismatchBehavior;
import io.github.trethore.myqolpackages.api.packages.PackageInfo;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.api.packages.TrustVersionScope;
import io.github.trethore.myqolpackages.command.trust.PackageTrustInteractionManager;
import io.github.trethore.myqolpackages.command.trust.PackageTrustInteractionManager.OriginalOperation;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class TrustPackageClientCommand {
  private static final String TOKEN_ARGUMENT = "token";

  private final PackageManager packageManager;
  private final PackageTrustInteractionManager interactionManager;

  public TrustPackageClientCommand(
      PackageManager packageManager, PackageTrustInteractionManager interactionManager) {
    this.packageManager = packageManager;
    this.interactionManager = interactionManager;
  }

  public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
    return ClientCommandManager.literal("trust")
        .then(
            ClientCommandManager.argument("id", StringArgumentType.word())
                .suggests(
                    (context, builder) ->
                        PackageCommandSupport.suggestPackageIds(
                            builder, packageManager.getPackages(), PackageInfo::id))
                .executes(this::execute));
  }

  public LiteralArgumentBuilder<FabricClientCommandSource> buildVersionCallbackCommand() {
    return ClientCommandManager.literal("_trust-version")
        .then(
            ClientCommandManager.argument(TOKEN_ARGUMENT, StringArgumentType.word())
                .then(scope("exact", TrustVersionScope.EXACT))
                .then(scope("patch", TrustVersionScope.PATCH_UPDATES))
                .then(scope("compatible", TrustVersionScope.COMPATIBLE_UPDATES))
                .then(scope("all", TrustVersionScope.ALL_VERSIONS)));
  }

  public LiteralArgumentBuilder<FabricClientCommandSource> buildFingerprintCallbackCommand() {
    return ClientCommandManager.literal("_trust-fingerprint")
        .then(
            ClientCommandManager.argument(TOKEN_ARGUMENT, StringArgumentType.word())
                .then(fingerprint("disabled", false, FingerprintMismatchBehavior.BLOCK))
                .then(fingerprint("log_only", true, FingerprintMismatchBehavior.LOG_ONLY))
                .then(fingerprint("chat_warning", true, FingerprintMismatchBehavior.CHAT_WARNING))
                .then(fingerprint("block", true, FingerprintMismatchBehavior.BLOCK)));
  }

  private int execute(CommandContext<FabricClientCommandSource> context) {
    return interactionManager.start(
        context.getSource(), StringArgumentType.getString(context, "id"), OriginalOperation.NONE);
  }

  private LiteralArgumentBuilder<FabricClientCommandSource> scope(
      String literal, TrustVersionScope scope) {
    return ClientCommandManager.literal(literal)
        .executes(
            context ->
                interactionManager.selectVersion(
                    context.getSource(),
                    StringArgumentType.getString(context, TOKEN_ARGUMENT),
                    scope));
  }

  private LiteralArgumentBuilder<FabricClientCommandSource> fingerprint(
      String literal, boolean fingerprintEnabled, FingerprintMismatchBehavior mismatchBehavior) {
    return ClientCommandManager.literal(literal)
        .executes(
            context ->
                interactionManager.selectFingerprint(
                    context.getSource(),
                    StringArgumentType.getString(context, TOKEN_ARGUMENT),
                    fingerprintEnabled,
                    mismatchBehavior));
  }
}

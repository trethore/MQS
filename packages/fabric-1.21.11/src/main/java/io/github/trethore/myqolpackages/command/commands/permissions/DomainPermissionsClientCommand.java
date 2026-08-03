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
package io.github.trethore.myqolpackages.command.commands.permissions;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.trethore.myqolpackages.api.MqpRuntime;
import io.github.trethore.myqolpackages.api.config.InternetAccessPermission;
import io.github.trethore.myqolpackages.api.config.InternetPermissions;
import io.github.trethore.myqolpackages.api.config.PackagePermissionOverrides;
import io.github.trethore.myqolpackages.command.ClientCommandResult;
import io.github.trethore.myqolpackages.command.MqpCommandFeedback;
import java.io.IOException;
import java.util.Locale;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class DomainPermissionsClientCommand {
  private static final String DOMAIN_ARGUMENT = "domain";

  private final MqpRuntime runtime;

  public DomainPermissionsClientCommand(MqpRuntime runtime) {
    this.runtime = runtime;
  }

  public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
    return ClientCommandManager.literal(DOMAIN_ARGUMENT)
        .then(
            ClientCommandManager.literal("add")
                .then(
                    ClientCommandManager.argument(DOMAIN_ARGUMENT, StringArgumentType.word())
                        .executes(this::add)))
        .then(ClientCommandManager.literal("list").executes(this::list))
        .then(
            ClientCommandManager.literal("remove")
                .then(
                    ClientCommandManager.argument(DOMAIN_ARGUMENT, StringArgumentType.word())
                        .executes(this::remove)))
        .then(ClientCommandManager.literal("clear").executes(this::clear));
  }

  private int add(CommandContext<FabricClientCommandSource> context) {
    InternetPermissions current = requireDomainsMode(context);
    if (current == null) {
      return ClientCommandResult.FAILURE;
    }
    String input = StringArgumentType.getString(context, DOMAIN_ARGUMENT);
    try {
      InternetPermissions updated = current.addDomain(input);
      if (updated.equals(current)) {
        MqpCommandFeedback.sendError(context.getSource(), "Domain is already allowed.");
        return ClientCommandResult.FAILURE;
      }
      return save(context, updated, "Added internet domain " + updated.domains().getLast() + ".");
    } catch (IllegalArgumentException exception) {
      MqpCommandFeedback.sendError(context.getSource(), exception.getMessage());
      return ClientCommandResult.FAILURE;
    }
  }

  private int list(CommandContext<FabricClientCommandSource> context) {
    InternetPermissions internet = currentInternet();
    MqpCommandFeedback.sendHeader(context.getSource());
    MqpCommandFeedback.sendLine(
        context.getSource(),
        "Internet access mode: " + internet.access().name().toLowerCase(Locale.ROOT));
    if (internet.domains().isEmpty()) {
      MqpCommandFeedback.sendLine(context.getSource(), "No internet domains configured.");
    } else {
      for (String domain : internet.domains()) {
        MqpCommandFeedback.sendLine(context.getSource(), "- " + domain);
      }
    }
    return ClientCommandResult.SUCCESS;
  }

  private int remove(CommandContext<FabricClientCommandSource> context) {
    InternetPermissions current = requireDomainsMode(context);
    if (current == null) {
      return ClientCommandResult.FAILURE;
    }
    String input = StringArgumentType.getString(context, DOMAIN_ARGUMENT);
    try {
      InternetPermissions updated = current.removeDomain(input);
      if (updated.equals(current)) {
        MqpCommandFeedback.sendError(context.getSource(), "Domain is not configured.");
        return ClientCommandResult.FAILURE;
      }
      return save(context, updated, "Removed internet domain.");
    } catch (IllegalArgumentException exception) {
      MqpCommandFeedback.sendError(context.getSource(), exception.getMessage());
      return ClientCommandResult.FAILURE;
    }
  }

  private int clear(CommandContext<FabricClientCommandSource> context) {
    InternetPermissions current = requireDomainsMode(context);
    if (current == null) {
      return ClientCommandResult.FAILURE;
    }
    return save(context, current.clearDomains(), "Cleared internet domains.");
  }

  private InternetPermissions requireDomainsMode(
      CommandContext<FabricClientCommandSource> context) {
    InternetPermissions internet = currentInternet();
    if (internet.access() != InternetAccessPermission.DOMAINS) {
      MqpCommandFeedback.sendError(
          context.getSource(), "Set internet permission to domains before editing domains.");
      return null;
    }
    return internet;
  }

  private InternetPermissions currentInternet() {
    InternetPermissions internet = runtime.getGlobalPermissions().internet();
    return internet == null ? InternetPermissions.none() : internet;
  }

  private int save(
      CommandContext<FabricClientCommandSource> context,
      InternetPermissions internet,
      String message) {
    PackagePermissionOverrides current = runtime.getGlobalPermissions();
    try {
      runtime.setGlobalPermissions(
          new PackagePermissionOverrides(
              current.hostAccess(), current.hostClassLookup(), current.filesystem(), internet));
    } catch (IOException exception) {
      MqpCommandFeedback.sendError(
          context.getSource(), "Could not save global permissions: " + exception.getMessage());
      return ClientCommandResult.FAILURE;
    }
    MqpCommandFeedback.sendInfo(context.getSource(), message);
    MqpCommandFeedback.sendInfo(context.getSource(), "Reload packages to apply the change.");
    return ClientCommandResult.SUCCESS;
  }
}

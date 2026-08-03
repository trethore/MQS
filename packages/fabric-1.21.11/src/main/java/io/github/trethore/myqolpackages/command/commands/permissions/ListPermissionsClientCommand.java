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

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.trethore.myqolpackages.api.MqpRuntime;
import io.github.trethore.myqolpackages.api.config.FileSystemPermissionOverrides;
import io.github.trethore.myqolpackages.api.config.FileSystemReadPermission;
import io.github.trethore.myqolpackages.api.config.FileSystemWritePermission;
import io.github.trethore.myqolpackages.api.config.HostAccessPermission;
import io.github.trethore.myqolpackages.api.config.HostClassLookupPermission;
import io.github.trethore.myqolpackages.api.config.InternetPermissions;
import io.github.trethore.myqolpackages.api.config.PackagePermissionOverrides;
import io.github.trethore.myqolpackages.command.ClientCommandResult;
import io.github.trethore.myqolpackages.command.MqpCommandFeedback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class ListPermissionsClientCommand {
  private final MqpRuntime runtime;

  public ListPermissionsClientCommand(MqpRuntime runtime) {
    this.runtime = runtime;
  }

  public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
    return ClientCommandManager.literal("list").executes(this::execute);
  }

  int execute(CommandContext<FabricClientCommandSource> context) {
    PackagePermissionOverrides permissions = runtime.getGlobalPermissions();
    FileSystemPermissionOverrides filesystem = permissions.filesystem();
    InternetPermissions internet =
        permissions.internet() == null ? InternetPermissions.none() : permissions.internet();
    MqpCommandFeedback.sendHeader(context.getSource());
    MqpCommandFeedback.sendLine(context.getSource(), "Global permissions:");
    sendPermission(
        context.getSource(),
        "host-access",
        permissions.hostAccess() == null ? HostAccessPermission.NONE : permissions.hostAccess());
    sendPermission(
        context.getSource(),
        "host-class-lookup",
        permissions.hostClassLookup() == null
            ? HostClassLookupPermission.NONE
            : permissions.hostClassLookup());
    sendPermission(
        context.getSource(),
        "filesystem-read",
        filesystem == null || filesystem.read() == null
            ? FileSystemReadPermission.NONE
            : filesystem.read());
    sendPermission(
        context.getSource(),
        "filesystem-write",
        filesystem == null || filesystem.write() == null
            ? FileSystemWritePermission.NONE
            : filesystem.write());
    sendPermission(context.getSource(), "internet", internet.access());
    for (String domain : internet.domains()) {
      MqpCommandFeedback.sendLine(context.getSource(), "  domain: " + domain);
    }
    return ClientCommandResult.SUCCESS;
  }

  private static void sendPermission(FabricClientCommandSource source, String name, Enum<?> value) {
    MqpCommandFeedback.sendLine(
        source,
        Component.literal(name + ": ")
            .withStyle(ChatFormatting.WHITE)
            .append(PermissionCommandSupport.formatPermission(value)));
  }
}

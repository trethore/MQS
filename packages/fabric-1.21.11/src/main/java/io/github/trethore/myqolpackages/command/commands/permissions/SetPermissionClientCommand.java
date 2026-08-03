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
import io.github.trethore.myqolpackages.api.config.FileSystemPermissionOverrides;
import io.github.trethore.myqolpackages.api.config.FileSystemReadPermission;
import io.github.trethore.myqolpackages.api.config.FileSystemWritePermission;
import io.github.trethore.myqolpackages.api.config.HostAccessPermission;
import io.github.trethore.myqolpackages.api.config.HostClassLookupPermission;
import io.github.trethore.myqolpackages.api.config.InternetAccessPermission;
import io.github.trethore.myqolpackages.api.config.InternetPermissions;
import io.github.trethore.myqolpackages.api.config.PackagePermissionOverrides;
import io.github.trethore.myqolpackages.command.ClientCommandResult;
import io.github.trethore.myqolpackages.command.MqpCommandFeedback;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public final class SetPermissionClientCommand {
  private static final String VALUE_ARGUMENT = "value";

  private final MqpRuntime runtime;

  public SetPermissionClientCommand(MqpRuntime runtime) {
    this.runtime = runtime;
  }

  public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
    return ClientCommandManager.literal("set")
        .then(
            ClientCommandManager.literal("host-access")
                .then(
                    ClientCommandManager.argument(VALUE_ARGUMENT, StringArgumentType.word())
                        .suggests(
                            (context, builder) ->
                                PermissionCommandSupport.suggestValues(
                                    builder, HostAccessPermission.values()))
                        .executes(this::setHostAccess)))
        .then(
            ClientCommandManager.literal("host-class-lookup")
                .then(
                    ClientCommandManager.argument(VALUE_ARGUMENT, StringArgumentType.word())
                        .suggests(
                            (context, builder) ->
                                PermissionCommandSupport.suggestValues(
                                    builder, HostClassLookupPermission.values()))
                        .executes(this::setHostClassLookup)))
        .then(
            ClientCommandManager.literal("filesystem-read")
                .then(
                    ClientCommandManager.argument(VALUE_ARGUMENT, StringArgumentType.word())
                        .suggests(
                            (context, builder) ->
                                PermissionCommandSupport.suggestValues(
                                    builder, FileSystemReadPermission.values()))
                        .executes(this::setFilesystemRead)))
        .then(
            ClientCommandManager.literal("filesystem-write")
                .then(
                    ClientCommandManager.argument(VALUE_ARGUMENT, StringArgumentType.word())
                        .suggests(
                            (context, builder) ->
                                PermissionCommandSupport.suggestValues(
                                    builder, FileSystemWritePermission.values()))
                        .executes(this::setFilesystemWrite)))
        .then(
            ClientCommandManager.literal("internet")
                .then(
                    ClientCommandManager.argument(VALUE_ARGUMENT, StringArgumentType.word())
                        .suggests(
                            (context, builder) ->
                                PermissionCommandSupport.suggestValues(
                                    builder, InternetAccessPermission.values()))
                        .executes(this::setInternet)));
  }

  private int setHostAccess(CommandContext<FabricClientCommandSource> context) {
    HostAccessPermission value = parseValue(context, HostAccessPermission.class);
    if (value == null) {
      return ClientCommandResult.FAILURE;
    }
    PackagePermissionOverrides current = getPermissions();
    return save(
        context,
        "host-access",
        value,
        new PackagePermissionOverrides(
            value, current.hostClassLookup(), current.filesystem(), current.internet()));
  }

  private int setHostClassLookup(CommandContext<FabricClientCommandSource> context) {
    HostClassLookupPermission value = parseValue(context, HostClassLookupPermission.class);
    if (value == null) {
      return ClientCommandResult.FAILURE;
    }
    PackagePermissionOverrides current = getPermissions();
    return save(
        context,
        "host-class-lookup",
        value,
        new PackagePermissionOverrides(
            current.hostAccess(), value, current.filesystem(), current.internet()));
  }

  private int setFilesystemRead(CommandContext<FabricClientCommandSource> context) {
    FileSystemReadPermission value = parseValue(context, FileSystemReadPermission.class);
    if (value == null) {
      return ClientCommandResult.FAILURE;
    }
    PackagePermissionOverrides current = getPermissions();
    FileSystemPermissionOverrides filesystem = current.filesystem();
    FileSystemWritePermission write = filesystem == null ? null : filesystem.write();
    return save(
        context,
        "filesystem-read",
        value,
        new PackagePermissionOverrides(
            current.hostAccess(),
            current.hostClassLookup(),
            new FileSystemPermissionOverrides(value, write),
            current.internet()));
  }

  private int setFilesystemWrite(CommandContext<FabricClientCommandSource> context) {
    FileSystemWritePermission value = parseValue(context, FileSystemWritePermission.class);
    if (value == null) {
      return ClientCommandResult.FAILURE;
    }
    PackagePermissionOverrides current = getPermissions();
    FileSystemPermissionOverrides filesystem = current.filesystem();
    FileSystemReadPermission read = filesystem == null ? null : filesystem.read();
    return save(
        context,
        "filesystem-write",
        value,
        new PackagePermissionOverrides(
            current.hostAccess(),
            current.hostClassLookup(),
            new FileSystemPermissionOverrides(read, value),
            current.internet()));
  }

  private int setInternet(CommandContext<FabricClientCommandSource> context) {
    InternetAccessPermission value = parseValue(context, InternetAccessPermission.class);
    if (value == null) {
      return ClientCommandResult.FAILURE;
    }
    InternetPermissions internet =
        switch (value) {
          case NONE -> InternetPermissions.none();
          case DOMAINS -> InternetPermissions.domains(List.of());
          case FULL -> InternetPermissions.full();
        };
    PackagePermissionOverrides current = getPermissions();
    return save(
        context,
        "internet",
        value,
        new PackagePermissionOverrides(
            current.hostAccess(), current.hostClassLookup(), current.filesystem(), internet));
  }

  private int save(
      CommandContext<FabricClientCommandSource> context,
      String permissionName,
      Enum<?> value,
      PackagePermissionOverrides permissions) {
    try {
      runtime.setGlobalPermissions(permissions);
    } catch (IOException exception) {
      MqpCommandFeedback.sendError(
          context.getSource(), "Could not save global permissions: " + exception.getMessage());
      return ClientCommandResult.FAILURE;
    }
    MqpCommandFeedback.sendInfo(
        context.getSource(),
        Component.literal(permissionName + " permission is now ")
            .append(PermissionCommandSupport.formatPermission(value)));
    MqpCommandFeedback.sendInfo(context.getSource(), "Reload packages to apply the change.");
    return ClientCommandResult.SUCCESS;
  }

  private PackagePermissionOverrides getPermissions() {
    return runtime.getGlobalPermissions();
  }

  private static <T extends Enum<T>> T parseValue(
      CommandContext<FabricClientCommandSource> context, Class<T> enumType) {
    String input = StringArgumentType.getString(context, VALUE_ARGUMENT);
    try {
      return Enum.valueOf(enumType, input.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      String validValues = PermissionCommandSupport.formatValues(enumType.getEnumConstants());
      MqpCommandFeedback.sendError(
          context.getSource(), "Unknown permission value. Expected one of: " + validValues + ".");
      return null;
    }
  }
}

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

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.command.ClientCommandResult;
import io.github.trethore.myqolpackages.command.MqpCommandFeedback;
import io.github.trethore.myqolpackages.command.commands.packages.*;
import io.github.trethore.myqolpackages.command.commands.trust.TrustPackageClientCommand;
import io.github.trethore.myqolpackages.command.commands.trust.UntrustPackageClientCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class PackagesClientCommand {
    private final DisablePackageClientCommand disablePackageClientCommand;
    private final EnablePackageClientCommand enablePackageClientCommand;
    private final InfoPackageClientCommand infoPackageClientCommand;
    private final ListPackagesClientCommand listPackagesClientCommand;
    private final RefreshPackagesClientCommand refreshPackagesClientCommand;
    private final ReloadPackagesClientCommand reloadPackagesClientCommand;
    private final TrustPackageClientCommand trustPackageClientCommand;
    private final UntrustPackageClientCommand untrustPackageClientCommand;
    private final PackageFingerprintClientCommand packageFingerprintClientCommand;

    public PackagesClientCommand(PackageManager packageManager) {
        trustPackageClientCommand = new TrustPackageClientCommand(packageManager);
        disablePackageClientCommand = new DisablePackageClientCommand(packageManager);
        enablePackageClientCommand = new EnablePackageClientCommand(packageManager, trustPackageClientCommand);
        infoPackageClientCommand = new InfoPackageClientCommand(packageManager);
        listPackagesClientCommand = new ListPackagesClientCommand(packageManager);
        refreshPackagesClientCommand = new RefreshPackagesClientCommand(packageManager);
        reloadPackagesClientCommand = new ReloadPackagesClientCommand(packageManager, trustPackageClientCommand);
        untrustPackageClientCommand = new UntrustPackageClientCommand(packageManager);
        packageFingerprintClientCommand =
                new PackageFingerprintClientCommand(packageManager, trustPackageClientCommand);
    }

    public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("packages")
                .executes(this::execute)
                .then(enablePackageClientCommand.buildCommand())
                .then(disablePackageClientCommand.buildCommand())
                .then(listPackagesClientCommand.buildCommand())
                .then(infoPackageClientCommand.buildCommand())
                .then(refreshPackagesClientCommand.buildCommand())
                .then(reloadPackagesClientCommand.buildCommand())
                .then(trustPackageClientCommand.buildCommand())
                .then(untrustPackageClientCommand.buildCommand())
                .then(packageFingerprintClientCommand.buildCommand())
                .then(trustPackageClientCommand.buildVersionCallbackCommand())
                .then(trustPackageClientCommand.buildFingerprintCallbackCommand())
                .then(packageFingerprintClientCommand.buildAcceptCallbackCommand());
    }

    private int execute(CommandContext<FabricClientCommandSource> context) {
        MqpCommandFeedback.sendInfo(
                context.getSource(),
                "Available actions: enable, disable, trust, untrust, fingerprint, list, info, refresh, reload.");
        return ClientCommandResult.SUCCESS;
    }
}

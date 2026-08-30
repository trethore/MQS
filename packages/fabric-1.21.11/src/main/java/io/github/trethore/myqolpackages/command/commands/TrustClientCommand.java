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
import io.github.trethore.myqolpackages.command.commands.trust.DisablePackageTrustClientCommand;
import io.github.trethore.myqolpackages.command.commands.trust.EnablePackageTrustClientCommand;
import io.github.trethore.myqolpackages.command.commands.trust.ListTrustedPackagesClientCommand;
import io.github.trethore.myqolpackages.command.commands.trust.PackageFingerprintClientCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class TrustClientCommand {
    private final DisablePackageTrustClientCommand disablePackageTrustClientCommand;
    private final EnablePackageTrustClientCommand enablePackageTrustClientCommand;
    private final ListTrustedPackagesClientCommand listTrustedPackagesClientCommand;
    private final PackageFingerprintClientCommand packageFingerprintClientCommand;

    public TrustClientCommand(PackageManager packageManager) {
        disablePackageTrustClientCommand = new DisablePackageTrustClientCommand(packageManager);
        enablePackageTrustClientCommand = new EnablePackageTrustClientCommand(packageManager);
        listTrustedPackagesClientCommand = new ListTrustedPackagesClientCommand(packageManager);
        packageFingerprintClientCommand = new PackageFingerprintClientCommand(packageManager);
    }

    public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("trust")
                .executes(this::execute)
                .then(enablePackageTrustClientCommand.buildCommand())
                .then(disablePackageTrustClientCommand.buildCommand())
                .then(listTrustedPackagesClientCommand.buildCommand())
                .then(packageFingerprintClientCommand.buildCommand());
    }

    private int execute(CommandContext<FabricClientCommandSource> context) {
        MqpCommandFeedback.sendInfo(context.getSource(), "Available actions: enable, disable, fingerprint, list.");
        return ClientCommandResult.SUCCESS;
    }
}

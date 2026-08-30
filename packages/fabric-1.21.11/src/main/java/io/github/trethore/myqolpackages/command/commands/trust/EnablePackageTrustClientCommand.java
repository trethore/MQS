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
package io.github.trethore.myqolpackages.command.commands.trust;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.trethore.myqolpackages.api.packages.FingerprintMismatchBehavior;
import io.github.trethore.myqolpackages.api.packages.PackageInfo;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.api.packages.PackageOperationResult;
import io.github.trethore.myqolpackages.api.packages.PackageTrustRequest;
import io.github.trethore.myqolpackages.api.packages.PackageTrustSnapshot;
import io.github.trethore.myqolpackages.api.packages.TrustVersionScope;
import io.github.trethore.myqolpackages.command.ClientCommandResult;
import io.github.trethore.myqolpackages.command.MqpCommandFeedback;
import io.github.trethore.myqolpackages.command.commands.PackageCommandSupport;
import java.util.Optional;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class EnablePackageTrustClientCommand {
    private final PackageManager packageManager;

    public EnablePackageTrustClientCommand(PackageManager packageManager) {
        this.packageManager = packageManager;
    }

    public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("enable")
                .then(ClientCommandManager.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> PackageCommandSupport.suggestPackageIds(
                                builder, packageManager.getPackages(), PackageInfo::id))
                        .executes(this::execute));
    }

    private int execute(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        String packageId = StringArgumentType.getString(context, "id");
        Optional<PackageTrustSnapshot> optionalSnapshot = packageManager.captureTrustSnapshot(packageId);
        if (optionalSnapshot.isEmpty()) {
            MqpCommandFeedback.sendError(source, "Unknown package: " + packageId);
            return ClientCommandResult.FAILURE;
        }

        PackageOperationResult result = packageManager.trustPackage(new PackageTrustRequest(
                optionalSnapshot.get(), TrustVersionScope.ALL_VERSIONS, true, FingerprintMismatchBehavior.BLOCK));
        PackageCommandSupport.sendDiagnostics(source, result.diagnostics());
        if (!result.successful()) {
            return ClientCommandResult.FAILURE;
        }
        MqpCommandFeedback.sendInfo(source, "Trust is now enabled for " + packageId);
        return ClientCommandResult.SUCCESS;
    }
}

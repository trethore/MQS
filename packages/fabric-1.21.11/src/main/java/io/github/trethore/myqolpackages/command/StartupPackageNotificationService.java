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
package io.github.trethore.myqolpackages.command;

import io.github.trethore.myqolpackages.api.packages.PackageDiagnostic;
import io.github.trethore.myqolpackages.api.packages.PackageDiagnosticCode;
import io.github.trethore.myqolpackages.api.packages.PackageDiscoveryResult;
import io.github.trethore.myqolpackages.command.commands.PackageCommandSupport;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;

public final class StartupPackageNotificationService {
    private final List<PackageDiagnostic> diagnostics;
    private boolean delivered;

    public StartupPackageNotificationService(PackageDiscoveryResult startupResult) {
        diagnostics = startupResult.diagnostics().stream()
                .filter(PackageDiagnostic::chatVisible)
                .filter(diagnostic -> diagnostic.code() == PackageDiagnosticCode.TRUST_REQUIRED
                        || diagnostic.code() == PackageDiagnosticCode.FINGERPRINT_BLOCKED
                        || diagnostic.code() == PackageDiagnosticCode.FINGERPRINT_WARNING)
                .toList();
    }

    public void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (delivered || diagnostics.isEmpty()) {
                return;
            }
            delivered = true;
            client.gui.getChat().addMessage(Component.literal("[MQP] Package trust review:"));
            for (PackageDiagnostic diagnostic : diagnostics) {
                client.gui.getChat().addMessage(PackageCommandSupport.createDiagnosticMessage(diagnostic));
            }
        });
    }
}

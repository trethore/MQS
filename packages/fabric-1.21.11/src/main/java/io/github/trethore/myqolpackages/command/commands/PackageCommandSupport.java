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
import io.github.trethore.myqolpackages.api.packages.PackageDiagnosticCode;
import io.github.trethore.myqolpackages.api.packages.PackageDiscoveryResult;
import io.github.trethore.myqolpackages.api.packages.PackageState;
import io.github.trethore.myqolpackages.api.packages.PackageTrustInfo;
import io.github.trethore.myqolpackages.api.packages.PackageTrustState;
import io.github.trethore.myqolpackages.command.ClientCommandResult;
import io.github.trethore.myqolpackages.command.MqpCommandFeedback;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class PackageCommandSupport {
    private PackageCommandSupport() {}

    public static int sendDiagnostics(FabricClientCommandSource source, List<PackageDiagnostic> diagnostics) {
        boolean failed = false;
        for (PackageDiagnostic diagnostic : diagnostics) {
            if (!diagnostic.chatVisible()) {
                continue;
            }
            Component messageComponent = createDiagnosticMessage(diagnostic);
            if (diagnostic.error()) {
                failed = true;
                MqpCommandFeedback.sendError(source, messageComponent);
            } else {
                MqpCommandFeedback.sendWarning(source, messageComponent);
            }
        }
        return failed ? ClientCommandResult.FAILURE : ClientCommandResult.SUCCESS;
    }

    public static Component createDiagnosticMessage(PackageDiagnostic diagnostic) {
        String message = diagnostic.packageId() + ": " + diagnostic.message();
        Component action = null;
        if (diagnostic.code() == PackageDiagnosticCode.TRUST_REQUIRED
                || diagnostic.code() == PackageDiagnosticCode.FINGERPRINT_BLOCKED) {
            action = MqpCommandFeedback.action(
                    "REVIEW", "mqp packages enable " + diagnostic.packageId(), "Review package trust");
        } else if (diagnostic.code() == PackageDiagnosticCode.FINGERPRINT_WARNING) {
            action = MqpCommandFeedback.action(
                    "ACCEPT FINGERPRINT",
                    "mqp packages fingerprint accept " + diagnostic.packageId(),
                    "Accept the current package fingerprint");
        }
        if (action == null) {
            return Component.literal(message);
        }
        return Component.empty().append(message).append(" ").append(action);
    }

    public static Component formatState(PackageState state) {
        return Component.literal(state.name().toLowerCase(Locale.ROOT)).withStyle(stateColor(state));
    }

    public static Component formatTrustState(PackageTrustState state) {
        boolean trusted = state != PackageTrustState.UNTRUSTED && state != PackageTrustState.VERSION_NOT_TRUSTED;
        return Component.literal(trusted ? "trusted" : "untrusted")
                .withStyle(trusted ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    public static Component formatFingerprint(PackageTrustInfo trustInfo) {
        if (trustInfo.state() == PackageTrustState.UNTRUSTED
                || trustInfo.state() == PackageTrustState.VERSION_NOT_TRUSTED) {
            return Component.literal("not evaluated").withStyle(ChatFormatting.GRAY);
        }
        if (!trustInfo.fingerprintEnabled() || trustInfo.state() == PackageTrustState.FINGERPRINT_DISABLED) {
            return Component.literal("disabled").withStyle(ChatFormatting.RED);
        }
        return Component.empty()
                .append(Component.literal("enabled").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" - " + formatFingerprintStatus(trustInfo)));
    }

    public static int sendDiscoveryResult(
            FabricClientCommandSource source, String action, PackageDiscoveryResult result) {
        MqpCommandFeedback.sendInfo(
                source,
                action
                        + ": "
                        + result.packages().size()
                        + " package(s), "
                        + result.diagnostics().size()
                        + " diagnostic(s).");
        return sendDiagnostics(source, result.diagnostics());
    }

    public static void sendEnabled(FabricClientCommandSource source, String packageId) {
        MqpCommandFeedback.sendInfo(source, createStateChange(packageId, "enabled", ChatFormatting.GREEN));
    }

    public static void sendDisabled(FabricClientCommandSource source, String packageId) {
        MqpCommandFeedback.sendInfo(source, createStateChange(packageId, "disabled", ChatFormatting.RED));
    }

    public static <T> CompletableFuture<Suggestions> suggestPackageIds(
            SuggestionsBuilder builder, Iterable<T> packages, Function<T, String> idProvider) {
        return suggestPackageIds(builder, packages, idProvider, packageValue -> true);
    }

    public static <T> CompletableFuture<Suggestions> suggestPackageIds(
            SuggestionsBuilder builder,
            Iterable<T> packages,
            Function<T, String> idProvider,
            Predicate<T> packageFilter) {
        String remaining = builder.getRemainingLowerCase();
        for (T packageValue : packages) {
            if (!packageFilter.test(packageValue)) {
                continue;
            }
            String packageId = idProvider.apply(packageValue);
            if (packageId.toLowerCase(Locale.ROOT).contains(remaining)) {
                builder.suggest(packageId);
            }
        }
        return builder.buildFuture();
    }

    private static Component createStateChange(String packageId, String state, ChatFormatting color) {
        return Component.empty()
                .append(Component.literal(packageId + " is now "))
                .append(Component.literal(state).withStyle(color));
    }

    private static String formatBehavior(PackageTrustInfo trustInfo) {
        return trustInfo.mismatchBehavior().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static String formatFingerprintStatus(PackageTrustInfo trustInfo) {
        return switch (trustInfo.state()) {
            case FINGERPRINT_MATCH -> "matched (" + formatBehavior(trustInfo) + ")";
            case FINGERPRINT_MISSING -> "missing (" + formatBehavior(trustInfo) + ")";
            case FINGERPRINT_MISMATCH_ALLOWED, FINGERPRINT_MISMATCH_BLOCKED ->
                "changed (" + formatBehavior(trustInfo) + ")";
            case FINGERPRINT_ERROR -> "error (" + formatBehavior(trustInfo) + ")";
            case UNTRUSTED, VERSION_NOT_TRUSTED, FINGERPRINT_DISABLED ->
                throw new IllegalArgumentException("Fingerprint is not enabled");
        };
    }

    private static ChatFormatting stateColor(PackageState state) {
        return switch (state) {
            case ENABLED -> ChatFormatting.GREEN;
            case DISABLED, ERROR -> ChatFormatting.RED;
        };
    }
}

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
package io.github.trethore.myqolpackages.command.trust;

import io.github.trethore.myqolpackages.api.packages.FingerprintMismatchBehavior;
import io.github.trethore.myqolpackages.api.packages.PackageDiagnostic;
import io.github.trethore.myqolpackages.api.packages.PackageManager;
import io.github.trethore.myqolpackages.api.packages.PackageOperationResult;
import io.github.trethore.myqolpackages.api.packages.PackageTrustRequest;
import io.github.trethore.myqolpackages.api.packages.PackageTrustSnapshot;
import io.github.trethore.myqolpackages.api.packages.TrustVersionScope;
import io.github.trethore.myqolpackages.command.ClientCommandResult;
import io.github.trethore.myqolpackages.command.MqpCommandFeedback;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public final class PackageTrustInteractionManager {
  private static final Duration SESSION_DURATION = Duration.ofMinutes(5);

  private final PackageManager packageManager;
  private final Map<String, FingerprintSession> fingerprintSessions = new HashMap<>();
  private final Map<String, TrustSession> trustSessions = new HashMap<>();

  public PackageTrustInteractionManager(PackageManager packageManager) {
    this.packageManager = packageManager;
  }

  public int start(
      FabricClientCommandSource source, String packageId, OriginalOperation originalOperation) {
    Optional<PackageTrustSnapshot> optionalSnapshot =
        packageManager.captureTrustSnapshot(packageId);
    if (optionalSnapshot.isEmpty()) {
      MqpCommandFeedback.sendError(source, "Unknown package: " + packageId);
      return ClientCommandResult.FAILURE;
    }
    PackageTrustSnapshot snapshot = optionalSnapshot.get();
    String token = createToken();
    trustSessions.put(
        token,
        new TrustSession(snapshot, originalOperation, null, Instant.now().plus(SESSION_DURATION)));
    sendStepOne(source, token, snapshot);
    return ClientCommandResult.SUCCESS;
  }

  public int selectVersion(
      FabricClientCommandSource source, String token, TrustVersionScope versionScope) {
    TrustSession session = getTrustSession(source, token);
    if (session == null) {
      return ClientCommandResult.FAILURE;
    }
    trustSessions.put(
        token,
        new TrustSession(
            session.snapshot(), session.originalOperation(), versionScope, session.expiresAt()));
    sendStepTwo(source, token, session.snapshot());
    return ClientCommandResult.SUCCESS;
  }

  public int selectFingerprint(
      FabricClientCommandSource source,
      String token,
      boolean fingerprintEnabled,
      FingerprintMismatchBehavior mismatchBehavior) {
    TrustSession session = getTrustSession(source, token);
    if (session == null || session.versionScope() == null) {
      MqpCommandFeedback.sendError(source, "Trust package Step 1/2 has not been completed");
      return ClientCommandResult.FAILURE;
    }
    trustSessions.remove(token);
    PackageOperationResult trustResult =
        packageManager.trustPackage(
            new PackageTrustRequest(
                session.snapshot(), session.versionScope(), fingerprintEnabled, mismatchBehavior));
    sendDiagnostics(source, trustResult);
    if (!trustResult.successful()) {
      return ClientCommandResult.FAILURE;
    }

    String range = formatRange(session.versionScope(), session.snapshot().version());
    MqpCommandFeedback.sendHeader(source);
    MqpCommandFeedback.sendLine(source, "Trusted " + formatPackage(session.snapshot()) + ".");
    MqpCommandFeedback.sendLine(
        source, "Versions: " + formatScope(session.versionScope()) + " (" + range + ")");
    MqpCommandFeedback.sendLine(
        source, "Fingerprint: " + (fingerprintEnabled ? "enabled" : "disabled"));
    if (fingerprintEnabled) {
      MqpCommandFeedback.sendLine(
          source, "Fingerprint changes: " + formatBehavior(mismatchBehavior));
      MqpCommandFeedback.sendLine(
          source, "Fingerprint: " + abbreviate(session.snapshot().fingerprint()));
    }
    return rerun(source, session.snapshot().id(), session.originalOperation());
  }

  public void sendFingerprintReview(
      FabricClientCommandSource source, String packageId, OriginalOperation originalOperation) {
    Optional<PackageTrustSnapshot> optionalSnapshot =
        packageManager.captureTrustSnapshot(packageId);
    if (optionalSnapshot.isEmpty() || optionalSnapshot.get().fingerprint() == null) {
      MqpCommandFeedback.sendError(source, "Could not capture the current package fingerprint");
      return;
    }
    PackageTrustSnapshot snapshot = optionalSnapshot.get();
    String token = createToken();
    fingerprintSessions.put(
        token,
        new FingerprintSession(snapshot, originalOperation, Instant.now().plus(SESSION_DURATION)));
    MutableComponent message =
        Component.empty()
            .append("Fingerprint changed for " + formatPackage(snapshot) + ". ")
            .append(
                button(
                    "ACCEPT NEW FINGERPRINT AND RERUN",
                    "mqp packages _accept-fingerprint " + token,
                    "Accept " + abbreviate(snapshot.fingerprint())));
    MqpCommandFeedback.sendError(source, message);
  }

  public int acceptFingerprint(FabricClientCommandSource source, String token) {
    FingerprintSession session = fingerprintSessions.remove(token);
    if (session == null || session.expiresAt().isBefore(Instant.now())) {
      MqpCommandFeedback.sendError(source, "Fingerprint review expired; run the operation again");
      return ClientCommandResult.FAILURE;
    }
    PackageOperationResult result =
        packageManager.acceptPackageFingerprint(
            session.snapshot().id(), session.snapshot().fingerprint());
    sendDiagnostics(source, result);
    if (!result.successful()) {
      return ClientCommandResult.FAILURE;
    }
    MqpCommandFeedback.sendInfo(
        source, "Accepted the new fingerprint for " + formatPackage(session.snapshot()));
    return rerun(source, session.snapshot().id(), session.originalOperation());
  }

  private TrustSession getTrustSession(FabricClientCommandSource source, String token) {
    TrustSession session = trustSessions.get(token);
    if (session == null || session.expiresAt().isBefore(Instant.now())) {
      trustSessions.remove(token);
      MqpCommandFeedback.sendError(source, "Trust review expired; start it again");
      return null;
    }
    return session;
  }

  private void sendStepOne(
      FabricClientCommandSource source, String token, PackageTrustSnapshot snapshot) {
    MqpCommandFeedback.sendHeader(source);
    MqpCommandFeedback.sendLine(source, "Trust package - Step 1/2");
    MqpCommandFeedback.sendLine(
        source,
        "Do you want to trust " + formatPackage(snapshot) + " version " + snapshot.version() + "?");
    MqpCommandFeedback.sendLine(source, Component.empty());
    MqpCommandFeedback.sendLine(
        source,
        button(
            "EXACT =" + snapshot.version(),
            versionCommand(token, "exact"),
            "Trust only this version"));
    MqpCommandFeedback.sendLine(
        source,
        button(
            "PATCH UPDATES ~" + snapshot.version(),
            versionCommand(token, "patch"),
            "Trust patch updates"));
    MqpCommandFeedback.sendLine(
        source,
        button(
            "COMPATIBLE UPDATES ^" + snapshot.version(),
            versionCommand(token, "compatible"),
            "Trust compatible updates"));
    MqpCommandFeedback.sendLine(
        source, button("ALL VERSIONS *", versionCommand(token, "all"), "Trust all versions"));
  }

  private void sendStepTwo(
      FabricClientCommandSource source, String token, PackageTrustSnapshot snapshot) {
    MqpCommandFeedback.sendHeader(source);
    MqpCommandFeedback.sendLine(source, "Trust package - Step 2/2");
    MqpCommandFeedback.sendLine(
        source, "How should fingerprint changes be handled for " + formatPackage(snapshot) + "?");
    MqpCommandFeedback.sendLine(source, Component.empty());
    MqpCommandFeedback.sendLine(
        source,
        button("DISABLED", fingerprintCommand(token, "disabled"), "Do not check fingerprints"));
    MqpCommandFeedback.sendLine(
        source, button("LOG ONLY", fingerprintCommand(token, "log_only"), "Log and continue"));
    MqpCommandFeedback.sendLine(
        source,
        button(
            "CHAT WARNING",
            fingerprintCommand(token, "chat_warning"),
            "Log, warn in chat, and continue"));
    MqpCommandFeedback.sendLine(
        source, button("BLOCK", fingerprintCommand(token, "block"), "Block changed packages"));
  }

  private int rerun(
      FabricClientCommandSource source, String packageId, OriginalOperation originalOperation) {
    if (originalOperation == OriginalOperation.NONE) {
      return ClientCommandResult.SUCCESS;
    }
    PackageOperationResult result =
        originalOperation == OriginalOperation.RELOAD
            ? packageManager.reloadPackage(packageId)
            : packageManager.enablePackage(packageId);
    sendDiagnostics(source, result);
    if (result.successful()) {
      MqpCommandFeedback.sendInfo(
          source,
          packageId
              + (originalOperation == OriginalOperation.RELOAD
                  ? " was reloaded"
                  : " is now enabled"));
      return ClientCommandResult.SUCCESS;
    }
    MqpCommandFeedback.sendError(source, packageId + " was trusted but the operation failed");
    return ClientCommandResult.FAILURE;
  }

  private static void sendDiagnostics(
      FabricClientCommandSource source, PackageOperationResult result) {
    result.diagnostics().stream()
        .filter(PackageDiagnostic::chatVisible)
        .forEach(diagnostic -> sendDiagnostic(source, diagnostic));
  }

  private static void sendDiagnostic(
      FabricClientCommandSource source, PackageDiagnostic diagnostic) {
    String message = diagnostic.packageId() + ": " + diagnostic.message();
    if (diagnostic.error()) {
      MqpCommandFeedback.sendError(source, message);
    } else {
      MqpCommandFeedback.sendWarning(source, message);
    }
  }

  private static Component button(String label, String command, String hoverText) {
    return Component.literal("[" + label + "]")
        .withStyle(
            style ->
                style
                    .withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent.RunCommand(command))
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverText))));
  }

  private static String createToken() {
    return UUID.randomUUID().toString();
  }

  private static String versionCommand(String token, String scope) {
    return "mqp packages _trust-version " + token + " " + scope;
  }

  private static String fingerprintCommand(String token, String behavior) {
    return "mqp packages _trust-fingerprint " + token + " " + behavior;
  }

  private static String formatPackage(PackageTrustSnapshot snapshot) {
    return snapshot.name() + " (" + snapshot.id() + ")";
  }

  private static String formatRange(TrustVersionScope scope, String version) {
    return switch (scope) {
      case EXACT -> "=" + version;
      case PATCH_UPDATES -> "~" + version;
      case COMPATIBLE_UPDATES -> "^" + version;
      case ALL_VERSIONS -> "*";
    };
  }

  private static String formatScope(TrustVersionScope scope) {
    return switch (scope) {
      case EXACT -> "exact version";
      case PATCH_UPDATES -> "patch updates";
      case COMPATIBLE_UPDATES -> "compatible updates";
      case ALL_VERSIONS -> "all versions";
    };
  }

  private static String formatBehavior(FingerprintMismatchBehavior behavior) {
    return switch (behavior) {
      case LOG_ONLY -> "log only";
      case CHAT_WARNING -> "chat warning";
      case BLOCK -> "block execution";
    };
  }

  private static String abbreviate(String fingerprint) {
    if (fingerprint == null || fingerprint.length() <= 23) {
      return String.valueOf(fingerprint);
    }
    return fingerprint.substring(0, 15) + "..." + fingerprint.substring(fingerprint.length() - 8);
  }

  public enum OriginalOperation {
    NONE,
    ENABLE,
    RELOAD
  }

  private record TrustSession(
      PackageTrustSnapshot snapshot,
      OriginalOperation originalOperation,
      TrustVersionScope versionScope,
      Instant expiresAt) {}

  private record FingerprintSession(
      PackageTrustSnapshot snapshot, OriginalOperation originalOperation, Instant expiresAt) {}
}

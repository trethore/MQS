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

import io.github.trethore.myqolpackages.api.packages.PackageTrustSnapshot;
import io.github.trethore.myqolpackages.api.packages.TrustVersionScope;
import io.github.trethore.myqolpackages.command.commands.trust.TrustPackageClientCommand.OriginalOperation;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class PackageTrustInteractionManager {
  private static final Duration SESSION_DURATION = Duration.ofMinutes(5);

  private final Map<String, FingerprintSession> fingerprintSessions = new HashMap<>();
  private final Map<String, TrustSession> trustSessions = new HashMap<>();

  String startTrustSession(PackageTrustSnapshot snapshot, OriginalOperation originalOperation) {
    String token = createToken();
    trustSessions.put(
        token,
        new TrustSession(snapshot, originalOperation, null, Instant.now().plus(SESSION_DURATION)));
    return token;
  }

  TrustSession selectVersion(String token, TrustVersionScope versionScope) {
    TrustSession session = getTrustSession(token);
    if (session == null) {
      return null;
    }
    TrustSession updatedSession =
        new TrustSession(
            session.snapshot(), session.originalOperation(), versionScope, session.expiresAt());
    trustSessions.put(token, updatedSession);
    return updatedSession;
  }

  TrustSession getTrustSession(String token) {
    TrustSession session = trustSessions.get(token);
    if (session == null || session.expiresAt().isBefore(Instant.now())) {
      trustSessions.remove(token);
      return null;
    }
    return session;
  }

  void removeTrustSession(String token) {
    trustSessions.remove(token);
  }

  String startFingerprintSession(
      PackageTrustSnapshot snapshot, OriginalOperation originalOperation) {
    String token = createToken();
    fingerprintSessions.put(
        token,
        new FingerprintSession(snapshot, originalOperation, Instant.now().plus(SESSION_DURATION)));
    return token;
  }

  FingerprintSession takeFingerprintSession(String token) {
    FingerprintSession session = fingerprintSessions.remove(token);
    if (session == null || session.expiresAt().isBefore(Instant.now())) {
      return null;
    }
    return session;
  }

  private static String createToken() {
    return UUID.randomUUID().toString();
  }

  record TrustSession(
      PackageTrustSnapshot snapshot,
      OriginalOperation originalOperation,
      TrustVersionScope versionScope,
      Instant expiresAt) {}

  record FingerprintSession(
      PackageTrustSnapshot snapshot, OriginalOperation originalOperation, Instant expiresAt) {}
}

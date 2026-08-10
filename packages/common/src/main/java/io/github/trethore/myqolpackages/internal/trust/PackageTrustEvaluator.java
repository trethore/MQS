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
package io.github.trethore.myqolpackages.internal.trust;

import io.github.trethore.myqolpackages.api.config.MqpConfig;
import io.github.trethore.myqolpackages.api.config.PackageFingerprintConfig;
import io.github.trethore.myqolpackages.api.config.PackageTrustConfig;
import io.github.trethore.myqolpackages.api.packages.FingerprintMismatchBehavior;
import io.github.trethore.myqolpackages.api.packages.PackageTrustInfo;
import io.github.trethore.myqolpackages.api.packages.PackageTrustState;
import java.nio.file.Path;

public final class PackageTrustEvaluator {
    private final PackageFingerprintService fingerprintService;

    public PackageTrustEvaluator(PackageFingerprintService fingerprintService) {
        this.fingerprintService = fingerprintService;
    }

    public PackageTrustEvaluation evaluate(
            String packageId, SemanticVersion version, Path packageDirectory, MqpConfig configuration) {
        PackageTrustConfig trustConfig = configuration.trust().packages().get(packageId);
        if (trustConfig == null) {
            return blocked(new PackageTrustInfo(
                    PackageTrustState.UNTRUSTED,
                    null,
                    true,
                    FingerprintMismatchBehavior.BLOCK,
                    null,
                    null,
                    "Package is not trusted"));
        }

        TrustedVersionRange trustedRange;
        try {
            trustedRange = TrustedVersionRange.parse(trustConfig.versions());
        } catch (IllegalArgumentException exception) {
            return blocked(new PackageTrustInfo(
                    PackageTrustState.VERSION_NOT_TRUSTED,
                    trustConfig.versions(),
                    true,
                    FingerprintMismatchBehavior.BLOCK,
                    null,
                    null,
                    "Trusted version range is invalid: " + exception.getMessage()));
        }
        if (!trustedRange.matches(version)) {
            return blocked(new PackageTrustInfo(
                    PackageTrustState.VERSION_NOT_TRUSTED,
                    trustConfig.versions(),
                    true,
                    FingerprintMismatchBehavior.BLOCK,
                    null,
                    null,
                    "Package version " + version + " is outside trusted range " + trustedRange));
        }

        PackageFingerprintConfig packageFingerprint = trustConfig.fingerprint();
        boolean fingerprintEnabled = packageFingerprint != null && packageFingerprint.enabled() != null
                ? packageFingerprint.enabled()
                : configuration.trust().fingerprintDefaults().enabled();
        FingerprintMismatchBehavior behavior =
                packageFingerprint != null && packageFingerprint.mismatchBehavior() != null
                        ? packageFingerprint.mismatchBehavior()
                        : configuration.trust().fingerprintDefaults().mismatchBehavior();
        String expectedFingerprint = packageFingerprint == null ? null : packageFingerprint.digest();
        if (!fingerprintEnabled) {
            return allowed(new PackageTrustInfo(
                    PackageTrustState.FINGERPRINT_DISABLED,
                    trustConfig.versions(),
                    false,
                    behavior,
                    expectedFingerprint,
                    null,
                    null));
        }

        String currentFingerprint;
        try {
            currentFingerprint = fingerprintService.fingerprint(packageDirectory);
        } catch (PackageFingerprintException exception) {
            return blocked(new PackageTrustInfo(
                    PackageTrustState.FINGERPRINT_ERROR,
                    trustConfig.versions(),
                    true,
                    behavior,
                    expectedFingerprint,
                    null,
                    exception.getMessage()));
        }
        if (expectedFingerprint == null) {
            return blocked(new PackageTrustInfo(
                    PackageTrustState.FINGERPRINT_MISSING,
                    trustConfig.versions(),
                    true,
                    behavior,
                    null,
                    currentFingerprint,
                    "Package has no accepted fingerprint"));
        }
        if (expectedFingerprint.equals(currentFingerprint)) {
            return allowed(new PackageTrustInfo(
                    PackageTrustState.FINGERPRINT_MATCH,
                    trustConfig.versions(),
                    true,
                    behavior,
                    expectedFingerprint,
                    currentFingerprint,
                    null));
        }

        String message = "Package fingerprint changed";
        if (behavior == FingerprintMismatchBehavior.BLOCK) {
            return blocked(new PackageTrustInfo(
                    PackageTrustState.FINGERPRINT_MISMATCH_BLOCKED,
                    trustConfig.versions(),
                    true,
                    behavior,
                    expectedFingerprint,
                    currentFingerprint,
                    message));
        }
        return new PackageTrustEvaluation(
                new PackageTrustInfo(
                        PackageTrustState.FINGERPRINT_MISMATCH_ALLOWED,
                        trustConfig.versions(),
                        true,
                        behavior,
                        expectedFingerprint,
                        currentFingerprint,
                        message),
                true,
                true,
                behavior == FingerprintMismatchBehavior.CHAT_WARNING);
    }

    private static PackageTrustEvaluation allowed(PackageTrustInfo info) {
        return new PackageTrustEvaluation(info, true, false, false);
    }

    private static PackageTrustEvaluation blocked(PackageTrustInfo info) {
        return new PackageTrustEvaluation(info, false, false, true);
    }
}

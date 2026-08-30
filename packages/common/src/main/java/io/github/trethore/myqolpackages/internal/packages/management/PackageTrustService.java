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
package io.github.trethore.myqolpackages.internal.packages.management;

import io.github.trethore.myqolpackages.api.MqpDiagnostic;
import io.github.trethore.myqolpackages.api.MqpDiagnosticCode;
import io.github.trethore.myqolpackages.api.config.PackageFingerprintConfig;
import io.github.trethore.myqolpackages.api.config.PackageTrustConfig;
import io.github.trethore.myqolpackages.api.packages.management.PackageOperationCode;
import io.github.trethore.myqolpackages.api.packages.management.PackageOperationResult;
import io.github.trethore.myqolpackages.api.packages.trust.PackageTrustRequest;
import io.github.trethore.myqolpackages.api.packages.trust.PackageTrustSnapshot;
import io.github.trethore.myqolpackages.internal.config.MqpConfigStore;
import io.github.trethore.myqolpackages.internal.packages.model.PackageDescriptor;
import io.github.trethore.myqolpackages.internal.trust.PackageFingerprintException;
import io.github.trethore.myqolpackages.internal.trust.PackageFingerprintService;
import io.github.trethore.myqolpackages.internal.trust.PackageTrustEvaluation;
import io.github.trethore.myqolpackages.internal.trust.PackageTrustEvaluator;
import io.github.trethore.myqolpackages.internal.trust.TrustedVersionRange;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PackageTrustService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageTrustService.class);

    private final MqpConfigStore configStore;
    private final PackageFingerprintService fingerprintService;
    private final PackageTrustEvaluator trustEvaluator;

    PackageTrustService(MqpConfigStore configStore, PackageFingerprintService fingerprintService) {
        this.configStore = Objects.requireNonNull(configStore, "configStore");
        this.fingerprintService = Objects.requireNonNull(fingerprintService, "fingerprintService");
        trustEvaluator = new PackageTrustEvaluator(fingerprintService);
    }

    PackageOperationResult trustPackage(PackageInstance packageInstance, PackageTrustRequest request) {
        PackageTrustSnapshot expectedPackage = Objects.requireNonNull(request.expectedPackage(), "expectedPackage");
        PackageDescriptor descriptor = packageInstance.getDescriptor();
        if (!descriptor.packageDirectory().equals(expectedPackage.packageDirectory())
                || !descriptor.manifest().version().equals(expectedPackage.version())) {
            return failedOperation(packageInstance, "Package changed while trust was being reviewed");
        }

        String currentFingerprint = captureFingerprint(descriptor.packageDirectory());
        if (expectedPackage.fingerprint() != null
                && !expectedPackage.fingerprint().equals(currentFingerprint)) {
            return failedOperation(packageInstance, "Package changed while trust was being reviewed");
        }
        if (request.fingerprintEnabled() && currentFingerprint == null) {
            return failedOperation(packageInstance, "Could not fingerprint package");
        }

        String versions = TrustedVersionRange.create(request.versionScope(), descriptor.semanticVersion());
        PackageFingerprintConfig fingerprintConfig = new PackageFingerprintConfig(
                request.fingerprintEnabled(),
                request.mismatchBehavior(),
                request.fingerprintEnabled() ? currentFingerprint : null);
        try {
            configStore.putTrustedPackage(descriptor.id(), new PackageTrustConfig(versions, fingerprintConfig));
        } catch (IOException exception) {
            return failedOperation(
                    descriptor.id(),
                    configStore.getConfigPath(),
                    "Could not save package trust: " + exception.getMessage());
        }
        updateTrustInfo(packageInstance);
        return successfulOperation();
    }

    PackageOperationResult acceptFingerprint(PackageInstance packageInstance, String expectedFingerprint) {
        String id = packageInstance.getId();
        PackageTrustConfig packageTrustConfig =
                configStore.getConfig().trust().packages().get(id);
        if (packageTrustConfig == null) {
            return new PackageOperationResult(
                    PackageOperationCode.TRUST_REQUIRED,
                    List.of(createTrustRequiredDiagnostic(packageInstance, "Package is not trusted")));
        }
        if (!TrustedVersionRange.parse(packageTrustConfig.versions())
                .matches(packageInstance.getDescriptor().semanticVersion())) {
            return new PackageOperationResult(
                    PackageOperationCode.TRUST_REQUIRED,
                    List.of(createTrustRequiredDiagnostic(
                            packageInstance, "Package version is outside its trusted range")));
        }

        String currentFingerprint =
                captureFingerprint(packageInstance.getDescriptor().packageDirectory());
        if (currentFingerprint == null) {
            return failedOperation(packageInstance, "Could not fingerprint package");
        }
        if (expectedFingerprint != null && !expectedFingerprint.equals(currentFingerprint)) {
            return failedOperation(packageInstance, "Package changed before its fingerprint was accepted");
        }
        try {
            configStore.updatePackageFingerprint(id, currentFingerprint);
        } catch (IOException exception) {
            return failedOperation(
                    id, configStore.getConfigPath(), "Could not save package fingerprint: " + exception.getMessage());
        }
        updateTrustInfo(packageInstance);
        return successfulOperation();
    }

    Optional<PackageTrustSnapshot> captureSnapshot(PackageInstance packageInstance) {
        if (!packageInstance.isAvailable()) {
            return Optional.empty();
        }
        PackageDescriptor descriptor = packageInstance.getDescriptor();
        return Optional.of(new PackageTrustSnapshot(
                descriptor.id(),
                descriptor.manifest().name(),
                descriptor.manifest().version(),
                descriptor.packageDirectory(),
                captureFingerprint(descriptor.packageDirectory())));
    }

    PackageTrustEvaluation evaluate(PackageInstance packageInstance) {
        PackageDescriptor descriptor = packageInstance.getDescriptor();
        PackageTrustEvaluation evaluation = trustEvaluator.evaluate(
                descriptor.id(), descriptor.semanticVersion(), descriptor.packageDirectory(), configStore.getConfig());
        packageInstance.setTrustInfo(evaluation.info());
        return evaluation;
    }

    void updateTrustInfo(PackageInstance packageInstance) {
        evaluate(packageInstance);
    }

    void addWarning(
            PackageInstance packageInstance, PackageTrustEvaluation evaluation, List<MqpDiagnostic> diagnostics) {
        if (!evaluation.warning()) {
            return;
        }
        LOGGER.warn(
                "Package {} fingerprint changed: expected {}, found {}",
                packageInstance.getId(),
                evaluation.info().expectedFingerprint(),
                evaluation.info().currentFingerprint());
        if (evaluation.chatVisible()) {
            diagnostics.add(new MqpDiagnostic(
                    MqpDiagnosticCode.FINGERPRINT_WARNING,
                    packageInstance.getId(),
                    packageInstance.getDescriptor().packageDirectory(),
                    evaluation.info().message(),
                    true,
                    false));
        }
    }

    MqpDiagnostic createBlockedDiagnostic(PackageInstance packageInstance, PackageTrustEvaluation evaluation) {
        LOGGER.warn(
                "Blocked package {}: {}",
                packageInstance.getId(),
                evaluation.info().message());
        MqpDiagnosticCode code = evaluation.info().state().requiresTrust()
                ? MqpDiagnosticCode.TRUST_REQUIRED
                : MqpDiagnosticCode.FINGERPRINT_BLOCKED;
        return new MqpDiagnostic(
                code,
                packageInstance.getId(),
                packageInstance.getDescriptor().packageDirectory(),
                evaluation.info().message(),
                true,
                true);
    }

    private MqpDiagnostic createTrustRequiredDiagnostic(PackageInstance packageInstance, String message) {
        return new MqpDiagnostic(
                MqpDiagnosticCode.TRUST_REQUIRED,
                packageInstance.getId(),
                packageInstance.getDescriptor().packageDirectory(),
                message,
                true,
                true);
    }

    private String captureFingerprint(Path packageDirectory) {
        try {
            return fingerprintService.fingerprint(packageDirectory);
        } catch (PackageFingerprintException exception) {
            LOGGER.warn("Could not fingerprint package at {}", packageDirectory, exception);
            return null;
        }
    }

    private static PackageOperationResult successfulOperation() {
        return new PackageOperationResult(PackageOperationCode.SUCCESS, List.of());
    }

    private PackageOperationResult failedOperation(PackageInstance packageInstance, String message) {
        return failedOperation(
                packageInstance.getId(), packageInstance.getDescriptor().packageDirectory(), message);
    }

    private static PackageOperationResult failedOperation(String id, Path path, String message) {
        return new PackageOperationResult(PackageOperationCode.FAILED, List.of(new MqpDiagnostic(id, path, message)));
    }
}

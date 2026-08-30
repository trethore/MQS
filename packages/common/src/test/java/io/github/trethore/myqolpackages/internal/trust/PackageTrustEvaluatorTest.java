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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.myqolpackages.api.config.FingerprintDefaultsConfig;
import io.github.trethore.myqolpackages.api.config.MqpConfig;
import io.github.trethore.myqolpackages.api.config.PackageFingerprintConfig;
import io.github.trethore.myqolpackages.api.config.PackageTrustConfig;
import io.github.trethore.myqolpackages.api.config.TrustConfig;
import io.github.trethore.myqolpackages.api.packages.trust.FingerprintMismatchBehavior;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackageTrustEvaluatorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void blocksUntrustedAndOutOfRangePackages() throws Exception {
        Path packageDirectory = createPackage();
        PackageTrustEvaluator evaluator = evaluator();

        assertFalse(evaluator
                .evaluate("example", SemanticVersion.parse("1.0.0"), packageDirectory, MqpConfig.defaults())
                .allowed());
        assertFalse(evaluator
                .evaluate(
                        "example",
                        SemanticVersion.parse("2.0.0"),
                        packageDirectory,
                        config("=1.0.0", false, FingerprintMismatchBehavior.BLOCK, null))
                .allowed());
    }

    @Test
    void allowsDisabledAndMatchingFingerprints() throws Exception {
        Path packageDirectory = createPackage();
        PackageFingerprintService service = new PackageFingerprintService();
        String digest = service.fingerprint(packageDirectory);

        assertTrue(evaluator()
                .evaluate(
                        "example",
                        SemanticVersion.parse("1.0.0"),
                        packageDirectory,
                        config("*", false, FingerprintMismatchBehavior.BLOCK, null))
                .allowed());
        assertTrue(evaluator()
                .evaluate(
                        "example",
                        SemanticVersion.parse("1.0.0"),
                        packageDirectory,
                        config("*", true, FingerprintMismatchBehavior.BLOCK, digest))
                .allowed());
    }

    @Test
    void appliesAllMismatchBehaviors() throws Exception {
        Path packageDirectory = createPackage();

        PackageTrustEvaluation logOnly = evaluator()
                .evaluate(
                        "example",
                        SemanticVersion.parse("1.0.0"),
                        packageDirectory,
                        config("*", true, FingerprintMismatchBehavior.LOG_ONLY, invalidDigest()));
        PackageTrustEvaluation chatWarning = evaluator()
                .evaluate(
                        "example",
                        SemanticVersion.parse("1.0.0"),
                        packageDirectory,
                        config("*", true, FingerprintMismatchBehavior.CHAT_WARNING, invalidDigest()));
        PackageTrustEvaluation block = evaluator()
                .evaluate(
                        "example",
                        SemanticVersion.parse("1.0.0"),
                        packageDirectory,
                        config("*", true, FingerprintMismatchBehavior.BLOCK, invalidDigest()));

        assertTrue(logOnly.allowed());
        assertFalse(logOnly.chatVisible());
        assertTrue(chatWarning.allowed());
        assertTrue(chatWarning.chatVisible());
        assertFalse(block.allowed());
    }

    private Path createPackage() throws Exception {
        Path packageDirectory = temporaryDirectory.resolve("example");
        Files.createDirectories(packageDirectory);
        Files.writeString(packageDirectory.resolve("index.js"), "source");
        return packageDirectory;
    }

    private PackageTrustEvaluator evaluator() {
        return new PackageTrustEvaluator(new PackageFingerprintService());
    }

    private MqpConfig config(String range, boolean enabled, FingerprintMismatchBehavior behavior, String digest) {
        return new MqpConfig(
                MqpConfig.CURRENT_CONFIG_VERSION,
                List.of(),
                List.of(),
                new TrustConfig(
                        new FingerprintDefaultsConfig(true, FingerprintMismatchBehavior.BLOCK),
                        Map.of(
                                "example",
                                new PackageTrustConfig(
                                        range, new PackageFingerprintConfig(enabled, behavior, digest)))));
    }

    private String invalidDigest() {
        return "sha256:" + "0".repeat(64);
    }
}

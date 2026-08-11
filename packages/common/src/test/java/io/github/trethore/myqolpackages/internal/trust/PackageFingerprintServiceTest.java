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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.myqolpackages.internal.packages.model.PackageDirectories;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackageFingerprintServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void producesDeterministicDigestAndDetectsChanges() throws Exception {
        Path packageDirectory = temporaryDirectory.resolve("example");
        Files.createDirectories(packageDirectory.resolve("src"));
        Files.writeString(packageDirectory.resolve("manifest.json"), "manifest");
        Files.writeString(packageDirectory.resolve("src/index.js"), "first");
        PackageFingerprintService service = new PackageFingerprintService();

        String first = service.fingerprint(packageDirectory);
        assertEquals(first, service.fingerprint(packageDirectory));
        assertTrue(PackageFingerprintService.isValidDigest(first));

        Files.writeString(packageDirectory.resolve("src/index.js"), "second");
        assertNotEquals(first, service.fingerprint(packageDirectory));
    }

    @Test
    void packageDataSiblingDoesNotAffectDigest() throws Exception {
        Path packageDirectory = temporaryDirectory.resolve("example");
        Files.createDirectories(packageDirectory);
        Files.writeString(packageDirectory.resolve("index.js"), "source");
        PackageFingerprintService service = new PackageFingerprintService();
        String before = service.fingerprint(packageDirectory);

        Path dataDirectory = temporaryDirectory
                .resolve(PackageDirectories.DATA_DIRECTORY_NAME)
                .resolve("example");
        Files.createDirectories(dataDirectory);
        Files.writeString(dataDirectory.resolve("state.json"), "state");

        assertEquals(before, service.fingerprint(packageDirectory));
    }

    @Test
    void rejectsSymbolicLinks() throws IOException {
        Path packageDirectory = temporaryDirectory.resolve("example");
        Files.createDirectories(packageDirectory);
        Path target = temporaryDirectory.resolve("target.js");
        Files.writeString(target, "source");
        Files.createSymbolicLink(packageDirectory.resolve("index.js"), target);

        assertThrows(
                PackageFingerprintException.class, () -> new PackageFingerprintService().fingerprint(packageDirectory));
    }
}

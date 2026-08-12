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
package io.github.trethore.myqolpackages.internal.runtime.graal.interop.generation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class GeneratedTypeNameResolver {
    private static final String CLASS_PREFIX = "mqp.generated.clazz.p";
    private static final String INTERFACE_PREFIX = "mqp.generated.iface.p";

    String resolve(String requestedName, String packageId, GeneratedTypeDefinition.Kind kind) {
        if (requestedName == null || requestedName.isBlank()) {
            throw new IllegalArgumentException("Generated type name must not be blank");
        }
        validateBinaryName(requestedName);
        if (requestedName.indexOf('.') >= 0) {
            validateAllowedPackage(requestedName);
            return requestedName;
        }
        String prefix = kind == GeneratedTypeDefinition.Kind.CLASS ? CLASS_PREFIX : INTERFACE_PREFIX;
        return prefix + hash(packageId) + "." + requestedName;
    }

    private static void validateBinaryName(String binaryName) {
        String[] segments = binaryName.split("\\.", -1);
        for (String segment : segments) {
            if (!isJavaIdentifier(segment)) {
                throw new IllegalArgumentException("Invalid JVM binary name: " + binaryName);
            }
        }
    }

    private static boolean isJavaIdentifier(String value) {
        if (value.isEmpty() || !Character.isJavaIdentifierStart(value.codePointAt(0))) {
            return false;
        }
        int offset = Character.charCount(value.codePointAt(0));
        while (offset < value.length()) {
            int codePoint = value.codePointAt(offset);
            if (!Character.isJavaIdentifierPart(codePoint)) {
                return false;
            }
            offset += Character.charCount(codePoint);
        }
        return true;
    }

    private static void validateAllowedPackage(String binaryName) {
        if (binaryName.startsWith("java.")
                || binaryName.startsWith("mqp.generated.")
                || binaryName.startsWith("io.github.trethore.myqolpackages.internal.")) {
            throw new IllegalArgumentException("Generated type cannot use reserved package: " + binaryName);
        }
    }

    private static String hash(String packageId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(packageId.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}

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
package io.github.trethore.myqolpackages.internal.runtime.graal;

import io.github.trethore.myqolpackages.internal.runtime.PackageLifecycleException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.SourceSection;

final class GraalPackageExceptionSupport {
    private GraalPackageExceptionSupport() {}

    static PackageLifecycleException createFailure(String message, Throwable cause, Path packageDirectory) {
        String detail = cause.getMessage();
        StringBuilder formattedMessage = new StringBuilder(message);
        if (detail != null && !detail.isBlank()) {
            formattedMessage.append(": ").append(detail);
        }
        String location = findLocation(cause, packageDirectory);
        if (location != null) {
            formattedMessage.append(" (").append(location).append(')');
        }
        return new PackageLifecycleException(formattedMessage.toString(), cause);
    }

    private static String findLocation(Throwable throwable, Path packageDirectory) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof PolyglotException polyglotException) {
                SourceSection sourceSection = polyglotException.getSourceLocation();
                if (isAvailable(sourceSection)) {
                    return formatLocation(sourceSection, packageDirectory);
                }
                for (PolyglotException.StackFrame stackFrame : polyglotException.getPolyglotStackTrace()) {
                    sourceSection = stackFrame.getSourceLocation();
                    if (isAvailable(sourceSection)) {
                        return formatLocation(sourceSection, packageDirectory);
                    }
                }
            }
            current = current.getCause();
        }
        return null;
    }

    private static String formatLocation(SourceSection sourceSection, Path packageDirectory) {
        StringBuilder location = new StringBuilder(formatSource(sourceSection.getSource(), packageDirectory));
        if (sourceSection.hasLines()) {
            location.append(':').append(sourceSection.getStartLine());
            if (sourceSection.hasColumns()) {
                location.append(':').append(sourceSection.getStartColumn());
            }
        }
        return location.toString();
    }

    private static String formatSource(Source source, Path packageDirectory) {
        String sourcePath = source.getPath();
        if (sourcePath != null) {
            try {
                Path path = Path.of(sourcePath);
                if (!path.isAbsolute()) {
                    path = packageDirectory.resolve(path);
                }
                path = path.toAbsolutePath().normalize();
                Path normalizedPackageDirectory =
                        packageDirectory.toAbsolutePath().normalize();
                if (path.startsWith(normalizedPackageDirectory)) {
                    return normalizedPackageDirectory
                            .relativize(path)
                            .toString()
                            .replace('\\', '/');
                }
            } catch (InvalidPathException ignored) {
                return source.getName();
            }
        }
        return source.getName();
    }

    private static boolean isAvailable(SourceSection sourceSection) {
        return sourceSection != null && sourceSection.isAvailable();
    }
}

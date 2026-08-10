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
import io.github.trethore.myqolpackages.internal.runtime.graal.api.PackageApiSession;
import java.nio.file.Path;
import org.graalvm.polyglot.Context;

final class GraalPackageContextResources implements AutoCloseable {
    private final Context context;
    private final PackageLogOutputStream errorOutput;
    private final PackageApiSession apiSession;
    private final PackageLogOutputStream output;
    private final Path packageDirectory;

    private boolean closed;

    GraalPackageContextResources(
            Context context,
            PackageLogOutputStream output,
            PackageLogOutputStream errorOutput,
            PackageApiSession apiSession,
            Path packageDirectory) {
        this.context = context;
        this.output = output;
        this.errorOutput = errorOutput;
        this.apiSession = apiSession;
        this.packageDirectory = packageDirectory;
    }

    @Override
    public void close() throws PackageLifecycleException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            RuntimeException failure = null;
            try {
                apiSession.close();
            } catch (RuntimeException exception) {
                failure = exception;
            }
            try {
                context.close();
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
            if (failure != null) {
                throw GraalPackageExceptionSupport.createFailure(
                        "Could not close JavaScript context", failure, packageDirectory);
            }
        } finally {
            output.close();
            errorOutput.close();
        }
    }

    Context getContext() {
        return context;
    }

    void tick() throws PackageLifecycleException {
        try {
            apiSession.tick();
        } catch (RuntimeException exception) {
            throw GraalPackageExceptionSupport.createFailure(
                    "Could not process an HTTP completion", exception, packageDirectory);
        }
    }
}

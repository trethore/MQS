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
import io.github.trethore.myqolpackages.internal.runtime.PackageScriptContext;
import java.nio.file.Path;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

final class GraalPackageScriptContext implements PackageScriptContext {
    private final Value onDisable;
    private final Value onEnable;
    private final Path packageDirectory;
    private final GraalPackageContextResources resources;

    private boolean closed;

    GraalPackageScriptContext(
            GraalPackageContextResources resources, Value onEnable, Value onDisable, Path packageDirectory) {
        this.resources = resources;
        this.onEnable = onEnable;
        this.onDisable = onDisable;
        this.packageDirectory = packageDirectory;
    }

    @Override
    public void invokeEnable() throws PackageLifecycleException {
        invokeLifecycleHook(onEnable, "onEnable");
    }

    @Override
    public void invokeDisable() throws PackageLifecycleException {
        invokeLifecycleHook(onDisable, "onDisable");
    }

    @Override
    public void tick() throws PackageLifecycleException {
        if (closed) {
            return;
        }
        resources.tick();
    }

    @Override
    public void close() throws PackageLifecycleException {
        if (closed) {
            return;
        }
        closed = true;
        resources.close();
    }

    private void invokeLifecycleHook(Value hook, String hookName) throws PackageLifecycleException {
        if (closed) {
            throw new PackageLifecycleException("JavaScript context is already closed");
        }
        try {
            Value result = hook.execute();
            if (result.hasMember("then") && result.getMember("then").canExecute()) {
                throw new PackageLifecycleException(
                        hookName + " returned a Promise; asynchronous lifecycle hooks are not supported");
            }
        } catch (PolyglotException exception) {
            throw GraalPackageExceptionSupport.createFailure(hookName + " failed", exception, packageDirectory);
        }
    }
}

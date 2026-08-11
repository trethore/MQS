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
package io.github.trethore.myqolpackages.internal.runtime.graal.api.java.generation;

import io.github.trethore.myqolpackages.internal.runtime.graal.api.PackageApiSession;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

final class JavaTypeGenerationSession implements PackageApiSession {
    private final List<GeneratedCallbackBinding> bindings = new ArrayList<>();
    private final ReentrantLock callbackLock = new ReentrantLock(true);
    private final String packageId;

    private boolean active = true;

    JavaTypeGenerationSession(String packageId) {
        this.packageId = packageId;
    }

    String packageId() {
        return packageId;
    }

    <T> T execute(CallbackOperation<T> operation) {
        callbackLock.lock();
        try {
            if (!active) {
                throw new IllegalStateException(
                        "Generated Java callback belongs to closed package context " + packageId);
            }
            return operation.execute();
        } finally {
            callbackLock.unlock();
        }
    }

    void addBinding(GeneratedCallbackBinding binding) {
        callbackLock.lock();
        try {
            if (!active) {
                throw new IllegalStateException("Package context is already closed: " + packageId);
            }
            bindings.add(binding);
        } finally {
            callbackLock.unlock();
        }
    }

    @Override
    public void close() {
        callbackLock.lock();
        try {
            if (!active) {
                return;
            }
            active = false;
            bindings.forEach(GeneratedCallbackBinding::close);
            bindings.clear();
        } finally {
            callbackLock.unlock();
        }
    }

    @FunctionalInterface
    interface CallbackOperation<T> {
        T execute();
    }
}

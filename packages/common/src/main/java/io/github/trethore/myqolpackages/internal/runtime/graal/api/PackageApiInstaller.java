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
package io.github.trethore.myqolpackages.internal.runtime.graal.api;

import io.github.trethore.myqolpackages.api.MqpRuntimeEnvironment;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextSpec;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.graalvm.polyglot.Context;

public final class PackageApiInstaller {
    private final List<PackageApiModule> modules;

    public PackageApiInstaller(String mqpVersion, MqpRuntimeEnvironment environment, HttpClient httpClient) {
        this(List.of(
                new MqpApiModule(Objects.requireNonNull(mqpVersion, "mqpVersion")),
                new JavaInteropApiModule(Objects.requireNonNull(environment, "environment")),
                new FetchApiModule(Objects.requireNonNull(httpClient, "httpClient"))));
    }

    public PackageApiInstaller(List<PackageApiModule> modules) {
        this.modules = List.copyOf(modules);
    }

    public PackageApiSession install(Context context, PackageContextSpec spec) {
        JavaScriptApiBridge bridge = new JavaScriptApiBridge(context);
        List<PackageApiSession> sessions = new ArrayList<>();
        try {
            for (PackageApiModule module : modules) {
                sessions.add(module.install(bridge, spec));
            }
            return new CompositePackageApiSession(sessions);
        } catch (RuntimeException exception) {
            try {
                new CompositePackageApiSession(sessions).close();
            } catch (RuntimeException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }
}

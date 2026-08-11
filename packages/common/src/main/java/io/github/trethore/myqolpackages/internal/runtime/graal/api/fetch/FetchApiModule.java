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
package io.github.trethore.myqolpackages.internal.runtime.graal.api.fetch;

import io.github.trethore.myqolpackages.internal.runtime.graal.api.PackageApiInstallContext;
import io.github.trethore.myqolpackages.internal.runtime.graal.api.PackageApiModule;
import io.github.trethore.myqolpackages.internal.runtime.graal.api.PackageApiSession;
import io.github.trethore.myqolpackages.internal.runtime.graal.http.PackageHttpClient;
import java.net.http.HttpClient;
import java.util.Objects;

public final class FetchApiModule implements PackageApiModule {
    private final HttpClient httpClient;

    public FetchApiModule(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    @Override
    public PackageApiSession install(PackageApiInstallContext context) {
        PackageHttpClient packageHttpClient = new PackageHttpClient(httpClient);
        try {
            FetchApi fetchApi = new FetchApi(context.javaScriptValues(), packageHttpClient);
            context.globals().define("fetch", FetchApiScript.create(context.javaScriptModules(), fetchApi));
            return new FetchApiSession(fetchApi, packageHttpClient);
        } catch (RuntimeException exception) {
            packageHttpClient.close();
            throw exception;
        }
    }

    private static final class FetchApiSession implements PackageApiSession {
        private final FetchApi fetchApi;
        private final PackageHttpClient httpClient;
        private boolean closed;

        private FetchApiSession(FetchApi fetchApi, PackageHttpClient httpClient) {
            this.fetchApi = fetchApi;
            this.httpClient = httpClient;
        }

        @Override
        public void tick() {
            if (!closed) {
                fetchApi.tick();
            }
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            try {
                fetchApi.close();
            } finally {
                httpClient.close();
            }
        }
    }
}

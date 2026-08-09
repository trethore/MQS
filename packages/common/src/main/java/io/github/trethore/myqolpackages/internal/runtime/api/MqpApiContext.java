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
package io.github.trethore.myqolpackages.internal.runtime.api;

import io.github.trethore.myqolpackages.internal.runtime.api.fetch.FetchApi;
import io.github.trethore.myqolpackages.internal.runtime.http.PackageHttpClient;
import java.util.Objects;

public final class MqpApiContext implements AutoCloseable {
  private final FetchApi fetchApi;
  private final PackageHttpClient httpClient;

  private boolean closed;

  MqpApiContext(FetchApi fetchApi, PackageHttpClient httpClient) {
    this.fetchApi = Objects.requireNonNull(fetchApi, "fetchApi");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
  }

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

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
package io.github.trethore.myqolpackages.internal.runtime.graal.api.fetch.http;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record PackageHttpResponse(
        int status, String url, boolean redirected, Map<String, List<String>> headers, byte[] body) {
    public PackageHttpResponse {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(headers, "headers");
        Objects.requireNonNull(body, "body");
        Map<String, List<String>> copiedHeaders = new LinkedHashMap<>();
        headers.forEach((name, values) -> copiedHeaders.put(name, List.copyOf(values)));
        headers = Collections.unmodifiableMap(copiedHeaders);
        body = body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object
                instanceof
                PackageHttpResponse(
                        int otherStatus,
                        String otherUrl,
                        boolean otherRedirected,
                        Map<String, List<String>> otherHeaders,
                        byte[] otherBody))) {
            return false;
        }
        return status == otherStatus
                && redirected == otherRedirected
                && url.equals(otherUrl)
                && headers.equals(otherHeaders)
                && Arrays.equals(body, otherBody);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, url, redirected, headers, Arrays.hashCode(body));
    }

    @Override
    public @NotNull String toString() {
        return "PackageHttpResponse[status=%s, url=%s, redirected=%s, headers=%s, bodyLength=%s]"
                .formatted(status, url, redirected, headers, body.length);
    }
}

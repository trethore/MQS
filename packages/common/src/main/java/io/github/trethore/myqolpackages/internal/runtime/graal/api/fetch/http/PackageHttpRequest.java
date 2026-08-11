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

import java.net.URI;
import java.util.List;
import java.util.Objects;

public final class PackageHttpRequest {
    private final byte[] body;
    private final List<PackageHttpHeader> headers;
    private final String method;
    private final URI uri;

    private PackageHttpRequest(URI uri, String method, List<PackageHttpHeader> headers, byte[] body) {
        this.uri = Objects.requireNonNull(uri, "uri");
        this.method = Objects.requireNonNull(method, "method");
        this.headers = List.copyOf(headers);
        this.body = body == null ? null : body.clone();
    }

    public static PackageHttpRequest withBody(URI uri, String method, List<PackageHttpHeader> headers, byte[] body) {
        return new PackageHttpRequest(uri, method, headers, Objects.requireNonNull(body, "body"));
    }

    public static PackageHttpRequest withoutBody(URI uri, String method, List<PackageHttpHeader> headers) {
        return new PackageHttpRequest(uri, method, headers, null);
    }

    boolean hasBody() {
        return body != null;
    }

    byte[] bodyData() {
        if (body == null) {
            throw new IllegalStateException("HTTP request does not have a body");
        }
        return body;
    }

    public URI uri() {
        return uri;
    }

    public String method() {
        return method;
    }

    public List<PackageHttpHeader> headers() {
        return headers;
    }
}

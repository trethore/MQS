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
package io.github.trethore.myqolpackages.internal.runtime.graal.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class PackageHttpClient implements AutoCloseable {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_REDIRECTS = 10;
    private static final int MAX_REQUEST_BYTES = 1024 * 1024;
    private static final int MAX_RESPONSE_BYTES = 8 * 1024 * 1024;
    private static final int MAX_PENDING_REQUESTS = 128;
    private static final int MAX_HEADERS = 100;
    private static final int MAX_HEADER_CHARACTERS = 64 * 1024;
    private static final Pattern METHOD_PATTERN = Pattern.compile("[A-Z!#$%&'*+.^_`|~-]+");
    private static final Set<String> RESTRICTED_HEADERS = Set.of(
            "connection",
            "content-length",
            "expect",
            "host",
            "proxy-authorization",
            "proxy-connection",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade");

    private final HttpClient httpClient;
    private final Set<CompletableFuture<?>> requests = ConcurrentHashMap.newKeySet();

    private boolean closed;

    public PackageHttpClient(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    public synchronized CompletableFuture<PackageHttpResponse> send(PackageHttpRequest request) {
        Objects.requireNonNull(request, "request");
        validateRequest(request);
        if (closed) {
            return CompletableFuture.failedFuture(new IllegalStateException("Package HTTP client is closed"));
        }
        if (requests.size() >= MAX_PENDING_REQUESTS) {
            return CompletableFuture.failedFuture(new IllegalStateException("Too many pending HTTP requests"));
        }
        CompletableFuture<PackageHttpResponse> future = send(request, 0, false);
        requests.add(future);
        future.whenComplete((response, throwable) -> requests.remove(future));
        return future;
    }

    @Override
    public synchronized void close() {
        closed = true;
        for (CompletableFuture<?> request : requests) {
            request.cancel(true);
        }
        requests.clear();
    }

    private CompletableFuture<PackageHttpResponse> send(
            PackageHttpRequest request, int redirectCount, boolean redirected) {
        return httpClient
                .sendAsync(buildRequest(request), HttpResponse.BodyHandlers.ofInputStream())
                .thenCompose(response -> {
                    String location = response.headers().firstValue("location").orElse(null);
                    if (location != null && isRedirect(response.statusCode())) {
                        closeResponseBody(response.body());
                        if (redirectCount >= MAX_REDIRECTS) {
                            return CompletableFuture.failedFuture(new IllegalStateException("Too many HTTP redirects"));
                        }
                        URI redirectUri = request.uri().resolve(location);
                        return send(
                                redirectedRequest(request, redirectUri, response.statusCode()),
                                redirectCount + 1,
                                true);
                    }
                    return CompletableFuture.supplyAsync(() -> readResponse(response, redirected));
                });
    }

    private static HttpRequest buildRequest(PackageHttpRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
                .timeout(REQUEST_TIMEOUT)
                .method(
                        request.method(),
                        request.hasBody()
                                ? HttpRequest.BodyPublishers.ofByteArray(request.bodyData())
                                : HttpRequest.BodyPublishers.noBody());
        for (PackageHttpHeader header : request.headers()) {
            builder.header(header.name(), header.value());
        }
        return builder.build();
    }

    private static PackageHttpRequest redirectedRequest(PackageHttpRequest request, URI redirectUri, int statusCode) {
        boolean switchToGet = statusCode == 303
                || ((statusCode == 301 || statusCode == 302) && request.method().equals("POST"));
        boolean sameOrigin = hasSameOrigin(request.uri(), redirectUri);
        List<PackageHttpHeader> headers = request.headers().stream()
                .filter(header -> sameOrigin
                        || !(header.name().equalsIgnoreCase("authorization")
                                || header.name().equalsIgnoreCase("cookie")))
                .filter(header -> !switchToGet || !header.name().equalsIgnoreCase("content-type"))
                .toList();
        if (switchToGet) {
            return PackageHttpRequest.withoutBody(redirectUri, "GET", headers);
        }
        if (request.hasBody()) {
            return PackageHttpRequest.withBody(redirectUri, request.method(), headers, request.bodyData());
        }
        return PackageHttpRequest.withoutBody(redirectUri, request.method(), headers);
    }

    private static void validateRequest(PackageHttpRequest request) {
        validateUri(request.uri());
        String method = validateMethod(request.method());
        validateBody(request, method);
        validateHeaders(request.headers());
    }

    private static void validateUri(URI uri) {
        if (uri.getRawFragment() != null) {
            throw new IllegalArgumentException("URL fragments are not supported");
        }
    }

    private static String validateMethod(String requestMethod) {
        String method = requestMethod.toUpperCase(Locale.ROOT);
        if (!requestMethod.equals(method) || !METHOD_PATTERN.matcher(method).matches()) {
            throw new IllegalArgumentException("Invalid HTTP method");
        }
        return method;
    }

    private static void validateBody(PackageHttpRequest request, String method) {
        boolean hasBody = request.hasBody();
        if (hasBody && request.bodyData().length > MAX_REQUEST_BYTES) {
            throw new IllegalArgumentException("HTTP request body exceeded 1 MiB");
        }
        if (hasBody && (method.equals("GET") || method.equals("HEAD"))) {
            throw new IllegalArgumentException(method + " requests cannot contain a body");
        }
    }

    private static void validateHeaders(List<PackageHttpHeader> headers) {
        if (headers.size() > MAX_HEADERS) {
            throw new IllegalArgumentException("Too many HTTP headers");
        }
        int headerCharacters = 0;
        for (PackageHttpHeader header : headers) {
            validateHeader(header);
            String name = header.name();
            String value = header.value();
            headerCharacters += name.length() + value.length();
            if (headerCharacters > MAX_HEADER_CHARACTERS) {
                throw new IllegalArgumentException("HTTP headers exceeded 64 KiB");
            }
        }
    }

    private static void validateHeader(PackageHttpHeader header) {
        String name = header.name();
        String value = header.value();
        if (name.isEmpty() || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("Invalid HTTP header");
        }
        if (RESTRICTED_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Restricted HTTP header: " + name);
        }
    }

    private static boolean hasSameOrigin(URI first, URI second) {
        return equalsIgnoreCase(first.getScheme(), second.getScheme())
                && equalsIgnoreCase(first.getHost(), second.getHost())
                && effectivePort(first) == effectivePort(second);
    }

    private static boolean equalsIgnoreCase(String first, String second) {
        return first != null && first.equalsIgnoreCase(second);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return uri.getScheme().equalsIgnoreCase("https") ? 443 : 80;
    }

    private static PackageHttpResponse readResponse(HttpResponse<InputStream> response, boolean redirected) {
        try (InputStream bodyInput = response.body()) {
            byte[] body = bodyInput.readNBytes(MAX_RESPONSE_BYTES + 1);
            if (body.length > MAX_RESPONSE_BYTES) {
                throw new IllegalStateException("HTTP response exceeded 8 MiB");
            }
            return new PackageHttpResponse(
                    response.statusCode(),
                    response.uri().toString(),
                    redirected,
                    response.headers().map(),
                    body);
        } catch (IOException exception) {
            throw new CompletionException(exception);
        }
    }

    private static void closeResponseBody(InputStream body) {
        try {
            body.close();
        } catch (IOException exception) {
            throw new CompletionException(exception);
        }
    }

    private static boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
    }
}

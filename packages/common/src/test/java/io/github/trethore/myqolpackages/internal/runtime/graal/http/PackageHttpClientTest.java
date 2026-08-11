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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PackageHttpClientTest {
    @Test
    void sendsRequestsAndReadsResponses() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> requestHeader = new AtomicReference<>();
        AtomicReference<byte[]> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/test", exchange -> {
            method.set(exchange.getRequestMethod());
            requestHeader.set(exchange.getRequestHeaders().getFirst("X-Test"));
            requestBody.set(exchange.getRequestBody().readAllBytes());
            exchange.getResponseHeaders().add("X-Result", "first");
            exchange.getResponseHeaders().add("X-Result", "second");
            byte[] body = "response".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(201, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try (PackageHttpClient client = new PackageHttpClient(HttpClient.newHttpClient())) {
            PackageHttpResponse response = client.send(PackageHttpRequest.withBody(
                            URI.create("http://127.0.0.1:%d/test"
                                    .formatted(server.getAddress().getPort())),
                            "POST",
                            List.of(new PackageHttpHeader("X-Test", "value")),
                            "request".getBytes(StandardCharsets.UTF_8)))
                    .get(5, TimeUnit.SECONDS);

            assertEquals("POST", method.get());
            assertEquals("value", requestHeader.get());
            assertArrayEquals("request".getBytes(StandardCharsets.UTF_8), requestBody.get());
            assertEquals(201, response.status());
            assertFalse(response.redirected());
            assertEquals("response", new String(response.body(), StandardCharsets.UTF_8));
            assertEquals(List.of("first", "second"), response.headers().get("x-result"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void appliesRedirectSecurityRules() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> cookie = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<byte[]> requestBody = new AtomicReference<>();
        HttpServer destination = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        destination.createContext("/destination", exchange -> {
            method.set(exchange.getRequestMethod());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            cookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            requestBody.set(exchange.getRequestBody().readAllBytes());
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        HttpServer origin = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        origin.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders()
                    .set(
                            "Location",
                            "http://127.0.0.1:%d/destination"
                                    .formatted(destination.getAddress().getPort()));
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        destination.start();
        origin.start();
        try (PackageHttpClient client = new PackageHttpClient(HttpClient.newHttpClient())) {
            PackageHttpResponse response = client.send(PackageHttpRequest.withBody(
                            URI.create("http://127.0.0.1:%d/redirect"
                                    .formatted(origin.getAddress().getPort())),
                            "POST",
                            List.of(
                                    new PackageHttpHeader("Authorization", "secret"),
                                    new PackageHttpHeader("Cookie", "secret=value"),
                                    new PackageHttpHeader("Content-Type", "text/plain")),
                            "request".getBytes(StandardCharsets.UTF_8)))
                    .get(5, TimeUnit.SECONDS);

            assertTrue(response.redirected());
            assertEquals("GET", method.get());
            assertNull(authorization.get());
            assertNull(cookie.get());
            assertNull(contentType.get());
            assertArrayEquals(new byte[0], requestBody.get());
        } finally {
            origin.stop(0);
            destination.stop(0);
        }
    }

    @Test
    void rejectsInvalidRequestsBeforeSending() {
        try (PackageHttpClient client = new PackageHttpClient(HttpClient.newHttpClient())) {
            PackageHttpRequest restrictedHeaderRequest = PackageHttpRequest.withoutBody(
                    URI.create("http://127.0.0.1/test"), "GET", List.of(new PackageHttpHeader("Host", "example.com")));
            PackageHttpRequest getWithEmptyBody =
                    PackageHttpRequest.withBody(URI.create("http://127.0.0.1/test"), "GET", List.of(), new byte[0]);

            IllegalArgumentException restrictedHeaderException =
                    assertThrows(IllegalArgumentException.class, () -> client.send(restrictedHeaderRequest));
            IllegalArgumentException bodyException =
                    assertThrows(IllegalArgumentException.class, () -> client.send(getWithEmptyBody));

            assertTrue(restrictedHeaderException.getMessage().contains("Restricted HTTP header"));
            assertEquals("GET requests cannot contain a body", bodyException.getMessage());
        }
    }
}

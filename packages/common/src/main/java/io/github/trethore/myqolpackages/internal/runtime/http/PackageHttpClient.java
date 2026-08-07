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
package io.github.trethore.myqolpackages.internal.runtime.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

public final class PackageHttpClient implements ProxyExecutable, AutoCloseable {
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final int MAX_REDIRECTS = 10;
  private static final int MAX_REQUEST_BYTES = 1024 * 1024;
  private static final int MAX_RESPONSE_BYTES = 8 * 1024 * 1024;
  private static final int MAX_PENDING_REQUESTS = 128;
  private static final int MAX_HEADERS = 100;
  private static final int MAX_HEADER_CHARACTERS = 64 * 1024;
  private static final int MAX_COMPLETIONS_PER_TICK = 64;
  private static final Pattern METHOD_PATTERN = Pattern.compile("[A-Z!#$%&'*+.^_`|~-]+");
  private static final Set<String> RESTRICTED_HEADERS =
      Set.of(
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

  private final ConcurrentLinkedQueue<Runnable> completions = new ConcurrentLinkedQueue<>();
  private final HttpClient httpClient;
  private final Set<CompletableFuture<?>> requests = ConcurrentHashMap.newKeySet();

  private volatile boolean closed;

  public PackageHttpClient(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public Object execute(Value... arguments) {
    if (arguments.length != 3 || !arguments[1].canExecute() || !arguments[2].canExecute()) {
      throw new IllegalArgumentException("Invalid fetch bridge invocation");
    }
    if (closed) {
      arguments[2].execute("Package HTTP client is closed");
      return false;
    }
    if (requests.size() >= MAX_PENDING_REQUESTS) {
      arguments[2].execute("Too many pending HTTP requests");
      return false;
    }
    HttpRequestData request = parseRequest(arguments[0]);
    Value resolve = arguments[1];
    Value reject = arguments[2];
    CompletableFuture<HttpResponseData> future = send(request, 0, false);
    requests.add(future);
    future.whenComplete(
        (response, throwable) -> {
          requests.remove(future);
          if (closed) {
            return;
          }
          completions.add(
              () -> {
                if (closed) {
                  return;
                }
                if (throwable == null) {
                  resolve.execute(toGuestResponse(response));
                } else {
                  reject.execute(errorMessage(throwable));
                }
              });
        });
    return true;
  }

  public void tick() {
    for (int count = 0; count < MAX_COMPLETIONS_PER_TICK; count++) {
      Runnable completion = completions.poll();
      if (completion == null) {
        return;
      }
      completion.run();
    }
  }

  @Override
  public void close() {
    closed = true;
    for (CompletableFuture<?> request : requests) {
      request.cancel(true);
    }
    requests.clear();
    completions.clear();
  }

  private CompletableFuture<HttpResponseData> send(
      HttpRequestData request, int redirectCount, boolean redirected) {
    return httpClient
        .sendAsync(buildRequest(request), HttpResponse.BodyHandlers.ofInputStream())
        .thenCompose(
            response -> {
              String location = response.headers().firstValue("location").orElse(null);
              if (location != null && isRedirect(response.statusCode())) {
                closeResponseBody(response.body());
                if (redirectCount >= MAX_REDIRECTS) {
                  return CompletableFuture.failedFuture(
                      new IllegalStateException("Too many HTTP redirects"));
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

  private static HttpRequest buildRequest(HttpRequestData request) {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(request.uri())
            .timeout(REQUEST_TIMEOUT)
            .method(
                request.method(),
                request.body() == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofByteArray(request.body()));
    for (Header header : request.headers()) {
      builder.header(header.name(), header.value());
    }
    return builder.build();
  }

  private static HttpRequestData redirectedRequest(
      HttpRequestData request, URI redirectUri, int statusCode) {
    boolean switchToGet =
        statusCode == 303
            || ((statusCode == 301 || statusCode == 302) && request.method().equals("POST"));
    boolean sameOrigin = hasSameOrigin(request.uri(), redirectUri);
    List<Header> headers =
        request.headers().stream()
            .filter(
                header ->
                    sameOrigin
                        || !(header.name().equalsIgnoreCase("authorization")
                            || header.name().equalsIgnoreCase("cookie")))
            .filter(header -> !switchToGet || !header.name().equalsIgnoreCase("content-type"))
            .toList();
    return new HttpRequestData(
        redirectUri,
        switchToGet ? "GET" : request.method(),
        headers,
        switchToGet ? null : request.body());
  }

  private static HttpRequestData parseRequest(Value request) {
    if (request == null || !request.hasMembers()) {
      throw new IllegalArgumentException("fetch request must be an object");
    }
    URI uri = URI.create(requireString(request, "url"));
    if (uri.getRawFragment() != null) {
      throw new IllegalArgumentException("URL fragments are not supported");
    }
    String method = requireString(request, "method").toUpperCase(Locale.ROOT);
    if (!METHOD_PATTERN.matcher(method).matches()) {
      throw new IllegalArgumentException("Invalid HTTP method");
    }
    List<Header> headers = parseHeaders(request.getMember("headers"));
    Value bodyValue = request.getMember("body");
    byte[] body =
        bodyValue == null || bodyValue.isNull()
            ? null
            : bodyValue.asString().getBytes(StandardCharsets.UTF_8);
    if (body != null && body.length > MAX_REQUEST_BYTES) {
      throw new IllegalArgumentException("HTTP request body exceeded 1 MiB");
    }
    if (body != null && (method.equals("GET") || method.equals("HEAD"))) {
      throw new IllegalArgumentException(method + " requests cannot contain a body");
    }
    return new HttpRequestData(uri, method, headers, body);
  }

  private static boolean hasSameOrigin(URI first, URI second) {
    return first.getScheme().equalsIgnoreCase(second.getScheme())
        && first.getHost().equalsIgnoreCase(second.getHost())
        && effectivePort(first) == effectivePort(second);
  }

  private static int effectivePort(URI uri) {
    if (uri.getPort() >= 0) {
      return uri.getPort();
    }
    return uri.getScheme().equalsIgnoreCase("https") ? 443 : 80;
  }

  private static List<Header> parseHeaders(Value headersValue) {
    if (headersValue == null || !headersValue.hasArrayElements()) {
      throw new IllegalArgumentException("fetch headers must be an array");
    }
    List<Header> headers = new ArrayList<>();
    int headerCharacters = 0;
    for (long index = 0; index < headersValue.getArraySize(); index++) {
      if (headers.size() >= MAX_HEADERS) {
        throw new IllegalArgumentException("Too many HTTP headers");
      }
      Value pair = headersValue.getArrayElement(index);
      if (!pair.hasArrayElements() || pair.getArraySize() != 2) {
        throw new IllegalArgumentException("Invalid HTTP header entry");
      }
      String name = pair.getArrayElement(0).asString().trim();
      String value = pair.getArrayElement(1).asString().trim();
      String lowercaseName = name.toLowerCase(Locale.ROOT);
      if (name.isEmpty() || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
        throw new IllegalArgumentException("Invalid HTTP header");
      }
      if (RESTRICTED_HEADERS.contains(lowercaseName)) {
        throw new IllegalArgumentException("Restricted HTTP header: " + name);
      }
      headerCharacters += name.length() + value.length();
      if (headerCharacters > MAX_HEADER_CHARACTERS) {
        throw new IllegalArgumentException("HTTP headers exceeded 64 KiB");
      }
      headers.add(new Header(name, value));
    }
    return List.copyOf(headers);
  }

  private static String requireString(Value object, String member) {
    Value value = object.getMember(member);
    if (value == null || !value.isString()) {
      throw new IllegalArgumentException("fetch " + member + " must be a string");
    }
    return value.asString();
  }

  private static ProxyObject toGuestResponse(HttpResponseData response) {
    List<Object> headers = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : response.headers().entrySet()) {
      for (String value : entry.getValue()) {
        headers.add(ProxyArray.fromArray(entry.getKey(), value));
      }
    }
    Map<String, Object> responseValues = new LinkedHashMap<>();
    responseValues.put("status", response.status());
    responseValues.put("url", response.url());
    responseValues.put("redirected", response.redirected());
    responseValues.put("headers", ProxyArray.fromList(headers));
    responseValues.put("text", new String(response.body(), StandardCharsets.UTF_8));
    responseValues.put("base64", Base64.getEncoder().encodeToString(response.body()));
    return ProxyObject.fromMap(responseValues);
  }

  private static HttpResponseData readResponse(
      HttpResponse<InputStream> response, boolean redirected) {
    try (InputStream bodyInput = response.body()) {
      byte[] body = bodyInput.readNBytes(MAX_RESPONSE_BYTES + 1);
      if (body.length > MAX_RESPONSE_BYTES) {
        throw new IllegalStateException("HTTP response exceeded 8 MiB");
      }
      return new HttpResponseData(
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
    return statusCode == 301
        || statusCode == 302
        || statusCode == 303
        || statusCode == 307
        || statusCode == 308;
  }

  private static String errorMessage(Throwable throwable) {
    Throwable cause = throwable;
    while ((cause instanceof CompletionException || cause.getClass() == RuntimeException.class)
        && cause.getCause() != null) {
      cause = cause.getCause();
    }
    String message = cause.getMessage();
    return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
  }

  private record Header(String name, String value) {}

  private record HttpRequestData(URI uri, String method, List<Header> headers, byte[] body) {
    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (!(object
          instanceof
          HttpRequestData(
              URI otherUri,
              String otherMethod,
              List<Header> otherHeaders,
              byte[] otherBody))) {
        return false;
      }
      return uri.equals(otherUri)
          && method.equals(otherMethod)
          && headers.equals(otherHeaders)
          && Arrays.equals(body, otherBody);
    }

    @Override
    public int hashCode() {
      return Objects.hash(uri, method, headers, Arrays.hashCode(body));
    }

    @Override
    public @NotNull String toString() {
      return "HttpRequestData[uri=%s, method=%s, headers=%s, body=%s]"
          .formatted(uri, method, headers, Arrays.toString(body));
    }
  }

  private record HttpResponseData(
      int status, String url, boolean redirected, Map<String, List<String>> headers, byte[] body) {
    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (!(object
          instanceof
          HttpResponseData(
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
      return "HttpResponseData[status=%s, url=%s, redirected=%s, headers=%s, body=%s]"
          .formatted(status, url, redirected, headers, Arrays.toString(body));
    }
  }
}

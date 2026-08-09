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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.github.trethore.myqolpackages.api.MqpRuntimeEnvironment;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextSpec;
import io.github.trethore.myqolpackages.internal.runtime.PackageLifecycleException;
import io.github.trethore.myqolpackages.internal.runtime.PackageScriptContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GraalPackageContextFactoryTest {
  private static final String TEST_RESOURCE_DIRECTORY =
      "io/github/trethore/myqolpackages/internal/runtime/graal/";

  @TempDir Path temporaryDirectory;

  @Test
  void invokesRequiredLifecycleExports() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            let enabled = false;

            export function onEnable() {
              if (enabled) throw new Error("already enabled");
              enabled = true;
            }

            export function onDisable() {
              if (!enabled) throw new Error("not enabled");
              enabled = false;
            }
            """);
    try (GraalPackageContextFactory contextFactory = createContextFactory()) {
      try (PackageScriptContext context = contextFactory.create(createSpec(entrypoint))) {
        assertDoesNotThrow(context::invokeEnable);
        assertDoesNotThrow(context::invokeDisable);
      }
    }
  }

  @Test
  void reportsRuntimeErrorSourceLocation() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            import { fail } from "./failure.js";

            export function onEnable() {
              fail();
            }

            export function onDisable() {}
            """);
    Files.writeString(
        entrypoint.resolveSibling("failure.js"),
        """
        export function fail() {
          BlocPos;
        }
        """);

    try (GraalPackageContextFactory contextFactory = createContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint))) {
      PackageLifecycleException exception =
          assertThrows(PackageLifecycleException.class, context::invokeEnable);

      assertTrue(
          exception
              .getMessage()
              .endsWith("ReferenceError: BlocPos is not defined (src/failure.js:2:3)"),
          exception.getMessage());
    }
  }

  @Test
  void reportsEntrypointLoadErrorSourceLocation() throws IOException {
    Path entrypoint =
        createEntrypoint(
            """
            const invalid = ;
            export function onEnable() {}
            export function onDisable() {}
            """);

    try (GraalPackageContextFactory contextFactory = createContextFactory()) {
      PackageLifecycleException exception =
          assertThrows(
              PackageLifecycleException.class, () -> contextFactory.create(createSpec(entrypoint)));

      assertTrue(exception.getMessage().contains("(src/index.js:1:"), exception.getMessage());
    } catch (PackageLifecycleException exception) {
      throw new AssertionError(exception);
    }
  }

  @Test
  void rejectsMissingLifecycleExport() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            export function onEnable() {}
            """);

    PackageLifecycleException exception;
    try (GraalPackageContextFactory contextFactory = createContextFactory()) {
      exception =
          assertThrows(
              PackageLifecycleException.class, () -> contextFactory.create(createSpec(entrypoint)));
    }

    assertTrue(exception.getMessage().contains("onDisable"));
  }

  @Test
  void rejectsAsynchronousLifecycleHook() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            export async function onEnable() {}
            export function onDisable() {}
            """);
    try (GraalPackageContextFactory contextFactory = createContextFactory()) {
      try (PackageScriptContext context = contextFactory.create(createSpec(entrypoint))) {
        PackageLifecycleException exception =
            assertThrows(PackageLifecycleException.class, context::invokeEnable);

        assertTrue(exception.getMessage().contains("asynchronous lifecycle hooks"));
      }
    }
  }

  @Test
  void rejectsContextCreationAfterClose() throws PackageLifecycleException {
    GraalPackageContextFactory contextFactory = createContextFactory();
    contextFactory.close();

    PackageLifecycleException exception =
        assertThrows(
            PackageLifecycleException.class,
            () -> contextFactory.create(createSpec(temporaryDirectory.resolve("src/index.js"))));

    assertTrue(exception.getMessage().contains("already closed"));
  }

  @Test
  void exposesRuntimeApis() throws IOException, PackageLifecycleException {
    Path dataDirectory = getDataDirectory();
    Path entrypoint = createEntrypointFromResource("exposes-runtime-apis.js");
    Files.writeString(entrypoint.resolveSibling("value.js"), readScriptResource("value.js"));

    try (GraalPackageContextFactory contextFactory = createContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint))) {
      assertDoesNotThrow(context::invokeEnable);
    }
    assertTrue(Files.isRegularFile(dataDirectory.resolve("created-during-load.txt")));
  }

  @Test
  void exposesMappedConstructorsMethodsAndFields() throws IOException, PackageLifecycleException {
    Path entrypoint = createEntrypointFromResource("mapped-interop.js");
    try (GraalPackageContextFactory contextFactory = createMappedContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint))) {
      assertDoesNotThrow(context::invokeEnable);
    }
  }

  @Test
  void resolvesPartialClassesFromIdentityCatalog() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            const fullyQualified = importClass(
              "io.github.trethore.myqolpackages.internal.runtime.graal.MappedClassFixture"
            );
            const partial = importClass("MappedClassFixture");
            if (fullyQualified !== partial) throw new Error("class proxies differ");

            export function onEnable() {}
            export function onDisable() {}
            """);
    try (GraalPackageContextFactory contextFactory = createCatalogContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint))) {
      assertDoesNotThrow(context::invokeEnable);
    }
  }

  @Test
  void rejectsAmbiguousPartialClassImports() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            importClass("Component");
            export function onEnable() {}
            export function onDisable() {}
            """);
    PackageLifecycleException exception;
    try (GraalPackageContextFactory contextFactory = createMappedContextFactory()) {
      exception =
          assertThrows(
              PackageLifecycleException.class, () -> contextFactory.create(createSpec(entrypoint)));
    }

    assertTrue(exception.getMessage().contains("Ambiguous class name"));
  }

  @Test
  void fetchesLocalDestinations() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/test",
        exchange -> {
          byte[] response = "local response".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    try {
      Path entrypoint =
          createEntrypoint(
              """
              let result;

              export function onEnable() {
                fetch("http://127.0.0.1:%d/test")
                  .then((response) => response.text())
                  .then((text) => { result = text; });
              }

              export function onDisable() {
                if (result !== "local response") throw new Error("fetch did not complete");
              }
              """
                  .formatted(server.getAddress().getPort()));

      try (GraalPackageContextFactory contextFactory = createContextFactory();
          PackageScriptContext context = contextFactory.create(createSpec(entrypoint))) {
        context.invokeEnable();
        awaitSuccessfulDisable(context);
      }
    } finally {
      server.stop(0);
    }
  }

  @Test
  void exposesFetchRequestAndResponseBehavior() throws Exception {
    AtomicReference<String> method = new AtomicReference<>();
    AtomicReference<String> requestHeader = new AtomicReference<>();
    AtomicReference<String> requestBody = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/json",
        exchange -> {
          method.set(exchange.getRequestMethod());
          requestHeader.set(exchange.getRequestHeaders().getFirst("X-Request"));
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          exchange.getResponseHeaders().add("X-Result", "first");
          exchange.getResponseHeaders().add("X-Result", "second");
          byte[] response = "{\"value\":42}".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(201, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.createContext(
        "/binary",
        exchange -> {
          byte[] response = {0, 1, 127, (byte) 128, (byte) 255};
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    try {
      String source =
          readScriptResource("fetch-request-response.js")
              .replace("__PORT__", Integer.toString(server.getAddress().getPort()));
      Path entrypoint = createEntrypoint(source);

      try (GraalPackageContextFactory contextFactory = createContextFactory();
          PackageScriptContext context = contextFactory.create(createSpec(entrypoint))) {
        context.invokeEnable();
        awaitSuccessfulDisable(context);
      }

      assertEquals("POST", method.get());
      assertEquals("42", requestHeader.get());
      assertEquals("123", requestBody.get());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void rejectsInvalidFetchOptionsWithTypeError() throws Exception {
    Path entrypoint =
        createEntrypoint(
            """
            let complete = false;
            let validError = false;

            export function onEnable() {
              const result = fetch("http://127.0.0.1/", { unsupported: true });
              if (!(result instanceof Promise)) throw new Error("fetch did not return a Promise");
              result.catch((error) => {
                validError = error instanceof TypeError
                  && error.message.includes("Unsupported fetch option");
                complete = true;
              });
            }

            export function onDisable() {
              if (!complete) throw new Error("fetch rejection did not complete");
              if (!validError) throw new Error("invalid fetch rejection");
            }
            """);

    try (GraalPackageContextFactory contextFactory = createContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint))) {
      context.invokeEnable();
      assertDoesNotThrow(context::invokeDisable);
    }
  }

  private Path createEntrypointFromResource(String resourceName) throws IOException {
    return createEntrypoint(readScriptResource(resourceName));
  }

  private String readScriptResource(String resourceName) throws IOException {
    String resourcePath = "/" + TEST_RESOURCE_DIRECTORY + resourceName;
    try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        throw new IOException("Missing test script resource: " + resourcePath);
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private Path createEntrypoint(String source) throws IOException {
    Path packageDirectory = temporaryDirectory.resolve("example-package");
    Path entrypoint = packageDirectory.resolve("src/index.js");
    Files.createDirectories(entrypoint.getParent());
    Files.writeString(entrypoint, source);
    return entrypoint;
  }

  private GraalPackageContextFactory createContextFactory() {
    return new GraalPackageContextFactory(temporaryDirectory, "0.0.1");
  }

  private GraalPackageContextFactory createMappedContextFactory() {
    MqpRuntimeEnvironment environment =
        new MqpRuntimeEnvironment(
            getClass().getClassLoader(),
            Optional.empty(),
            Optional.of(TEST_RESOURCE_DIRECTORY + "mappings/test-client.txt"));
    return new GraalPackageContextFactory(temporaryDirectory, "0.0.1", environment);
  }

  private GraalPackageContextFactory createCatalogContextFactory() {
    MqpRuntimeEnvironment environment =
        new MqpRuntimeEnvironment(
            getClass().getClassLoader(),
            Optional.of(TEST_RESOURCE_DIRECTORY + "catalog/test-client.txt"),
            Optional.empty());
    return new GraalPackageContextFactory(temporaryDirectory, "0.0.1", environment);
  }

  private PackageContextSpec createSpec(Path entrypoint) {
    return new PackageContextSpec(
        "example-package", entrypoint.getParent().getParent(), entrypoint, getDataDirectory());
  }

  private Path getDataDirectory() {
    return temporaryDirectory.resolve("package-data/example-package").toAbsolutePath().normalize();
  }

  @SuppressWarnings("java:S2925")
  private static void awaitSuccessfulDisable(PackageScriptContext context)
      throws PackageLifecycleException, InterruptedException {
    PackageLifecycleException failure = null;
    for (int attempt = 0; attempt < 1000; attempt++) {
      context.tick();
      try {
        context.invokeDisable();
        failure = null;
        break;
      } catch (PackageLifecycleException exception) {
        failure = exception;
        Thread.sleep(1);
      }
    }
    assertNull(failure);
  }
}

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
package io.github.trethore.myqolpackages.internal.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.github.trethore.myqolpackages.api.MqpRuntimeEnvironment;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GraalPackageContextFactoryTest {
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
    Path entrypoint =
        createEntrypoint(
            """
            import { value } from "./value.js";
            if (value !== 42) throw new Error("module was not loaded");
            if (mqp.version !== "0.0.1") throw new Error("unexpected MQP version");
            if (typeof mqp.dataDirectory !== "string") throw new Error("invalid data directory");
            if (mqp.package.id !== "example-package") throw new Error("unexpected package ID");
            const Files = Java.type("java.nio.file.Files");
            const Path = Java.type("java.nio.file.Path");
            if (!Files.isDirectory(Path.of(mqp.dataDirectory))) {
              throw new Error("missing data directory");
            }
            Files.writeString(
              Path.of(mqp.dataDirectory).resolve("created-during-load.txt"),
              "data"
            );
            if (typeof fetch !== "function") throw new Error("missing fetch");
            for (const name of ["mqp", "importClass", "wrap", "packages", "net", "fetch"]) {
              const descriptor = Object.getOwnPropertyDescriptor(globalThis, name);
              if (!descriptor || descriptor.configurable || !descriptor.enumerable || descriptor.writable) {
                throw new Error(`invalid global descriptor: ${name}`);
              }
            }
            if (!Object.isFrozen(mqp) || !Object.isFrozen(mqp.package)) {
              throw new Error("mutable MQP metadata");
            }
            for (const name of Object.getOwnPropertyNames(globalThis)) {
              if (name.startsWith("__mqp")) throw new Error(`leaked host bridge: ${name}`);
            }
            for (const name of [
              "createMqpBootstrap",
              "createRuntimeAdapter",
              "installMqp",
              "installJavaInterop",
              "installFetch"
            ]) {
              if (Object.hasOwn(globalThis, name)) throw new Error(`leaked API installer: ${name}`);
            }
            try { mqp.version = "changed"; } catch (error) {}
            if (mqp.version !== "0.0.1") throw new Error("mutable MQP API");
            const originalDataDirectory = mqp.dataDirectory;
            try { mqp.dataDirectory = "changed"; } catch (error) {}
            if (mqp.dataDirectory !== originalDataDirectory) throw new Error("mutable data directory");
            const HostString = Java.type("java.lang.String");
            if (HostString.valueOf(42) !== "42") throw new Error("host lookup failed");
            const imported = importClass("java.lang.Double");
            const packaged = packages.java.lang.Double;
            if (!imported || imported !== packaged) throw new Error("class proxies differ");

            export function onEnable() {}
            export function onDisable() {}
            """);
    Files.writeString(entrypoint.resolveSibling("value.js"), "export const value = 42;");

    try (GraalPackageContextFactory contextFactory = createContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint))) {
      assertDoesNotThrow(context::invokeEnable);
    }
    assertTrue(Files.isRegularFile(dataDirectory.resolve("created-during-load.txt")));
  }

  @Test
  void exposesMappedConstructorsMethodsAndFields() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            const Fixture = importClass("net.minecraft.test.FakeMappedClass");
            const partialFixture = importClass("FakeMappedClass");
            const packagedFixture = packages.net.minecraft.test.FakeMappedClass;
            const aliasedFixture = net.minecraft.test.FakeMappedClass;
            if (Fixture !== partialFixture
                || partialFixture !== packagedFixture
                || packagedFixture !== aliasedFixture) {
              throw new Error("class proxy identity differs");
            }
            const AlphaComponent = importClass("net.minecraft.alpha.Component");
            const BetaComponent = importClass("net.minecraft.beta.Component");
            if (AlphaComponent.alphaValue !== "initial-static"
                || BetaComponent.betaValue !== "initial-static") {
              throw new Error("class alias mappings interfered");
            }
            if (Fixture.greeting("MQP") !== "hello MQP") throw new Error("static method failed");
            if (Fixture.choose(4) !== "number:4") throw new Error("numeric overload failed");
            if (Fixture.choose("four") !== "string:four") throw new Error("string overload failed");
            if (Fixture.specific("value") !== "string:value") {
              throw new Error("specific overload failed");
            }
            if (Fixture.specific(null) !== "string:null") {
              throw new Error("null overload specificity failed");
            }
            if (Fixture.numberOnly(4) !== "shared-number:4") {
              throw new Error("mapped numeric signature failed");
            }
            if (Fixture.stringOnly("four") !== "shared-string:four") {
              throw new Error("mapped string signature failed");
            }
            let mismatchedSignatureRejected = false;
            try { Fixture.numberOnly("four"); } catch (error) { mismatchedSignatureRejected = true; }
            if (!mismatchedSignatureRejected) throw new Error("mapped signature leaked overload");
            if (Fixture.staticValue !== "initial-static") throw new Error("static field failed");
            if (Fixture.staticValue$ !== "initial-static") throw new Error("static $ field failed");
            Fixture.staticValue$ = "changed-static";
            if (Fixture.staticValue !== "changed-static") throw new Error("static write failed");
            if (typeof Fixture.staticCollision !== "function") {
              throw new Error("static method did not win collision");
            }
            if (Fixture.staticCollision() !== "static-method") {
              throw new Error("static collision method failed");
            }
            if (Fixture.staticCollision$ !== "static-field") {
              throw new Error("static collision field failed");
            }
            let ambiguousWriteRejected = false;
            try { Fixture.staticCollision = "changed"; } catch (error) { ambiguousWriteRejected = true; }
            if (!ambiguousWriteRejected) throw new Error("ambiguous static write was accepted");

            const instance = new Fixture("fixture", 2);
            if (instance.name !== "fixture" || instance.count !== 2) {
              throw new Error("private constructor or fields failed");
            }
            if (instance.increment(3) !== 5 || instance.count$ !== 5) {
              throw new Error("instance method failed");
            }
            instance.name$ = "renamed";
            if (instance.name !== "renamed") throw new Error("instance write failed");
            instance.name = "renamed-again";
            if (instance.name$ !== "renamed-again") throw new Error("plain field write failed");
            if (typeof instance.value !== "function") {
              throw new Error("instance method did not win collision");
            }
            if (instance.value() !== "instance-method" || instance.value$ !== "instance-field") {
              throw new Error("instance collision failed");
            }
            ambiguousWriteRejected = false;
            try { instance.value = "changed"; } catch (error) { ambiguousWriteRejected = true; }
            if (!ambiguousWriteRejected) throw new Error("ambiguous instance write was accepted");
            instance.value$ = "changed-field";
            if (instance.value$ !== "changed-field") throw new Error("collision write failed");
            if (instance.join("joined", "a", "b") !== "joined:a,b") {
              throw new Error("varargs method failed");
            }
            if (instance.baseValue !== "base-field" || instance.baseMethod() !== "base-method") {
              throw new Error("inherited private members failed");
            }

            const copy = instance.copy();
            if (copy.name !== "renamed-again" || copy.count !== 5) {
              throw new Error("return wrapping failed");
            }
            if (!instance.same(instance) || instance.same(copy)) {
              throw new Error("wrapped object argument failed");
            }
            if (!instance._self || !instance._class || Fixture._class !== instance._class) {
              throw new Error("raw escape members failed");
            }
            if (!instance._equals(instance) || !instance._equals(instance._self)) {
              throw new Error("Java equality failed for the same object");
            }
            if (instance._equals(copy) || instance._equals(copy._self)) {
              throw new Error("Java equality failed for different objects");
            }
            const BaseFixture = importClass("net.minecraft.test.FakeMappedBase");
            if (!instance._instanceof(Fixture)
                || !instance._instanceof(Fixture._class)
                || !instance._instanceof(BaseFixture)) {
              throw new Error("Java instanceof failed");
            }
            let objectTypeRejected = false;
            try { instance._instanceof(copy); } catch (error) { objectTypeRejected = true; }
            if (!objectTypeRejected) throw new Error("instanceof accepted an object as a type");
            let nullTypeRejected = false;
            try { instance._instanceof(null); } catch (error) { nullTypeRejected = true; }
            if (!nullTypeRejected) throw new Error("instanceof accepted null as a type");
            const wrappedAgain = wrap(instance._self);
            if (wrappedAgain.hiddenName !== "renamed-again") {
              throw new Error("wrapped field read failed");
            }
            if (!wrappedAgain.hiddenSame(instance)) {
              throw new Error("wrapped object identity failed");
            }
            if (!wrap(instance).same(instance)) {
              throw new Error("wrapper wrapping failed");
            }

            let finalWriteRejected = false;
            try { instance.finalValue$ = "changed"; } catch (error) { finalWriteRejected = true; }
            if (!finalWriteRejected || instance.finalValue !== "instance-final") {
              throw new Error("final instance field was writable");
            }
            finalWriteRejected = false;
            try { Fixture.staticFinalValue$ = "changed"; } catch (error) { finalWriteRejected = true; }
            if (!finalWriteRejected || Fixture.staticFinalValue !== "static-final") {
              throw new Error("final static field was writable");
            }

            export function onEnable() {}
            export function onDisable() {}
            """);
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
              "io.github.trethore.myqolpackages.internal.runtime.MappedClassFixture"
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
      Path entrypoint =
          createEntrypoint(
              """
              let complete = false;
              let failure;

              const assert = (condition, message) => {
                if (!condition) throw new Error(message);
              };

              export function onEnable() {
                fetch("http://127.0.0.1:%1$d/json", {
                  method: "post",
                  headers: { "X-Request": 42 },
                  body: 123
                })
                  .then((response) => {
                    assert(response.status === 201, "invalid status");
                    assert(response.statusText === "Created", "invalid status text");
                    assert(response.ok, "invalid ok value");
                    assert(!response.redirected, "invalid redirected value");
                    assert(response.url.endsWith("/json"), "invalid URL");
                    assert(Object.isFrozen(response.headers), "mutable headers");
                    const header = response.headers.get("X-Result");
                    assert(header.includes("first") && header.includes("second"), "invalid header");
                    assert(response.headers.has("x-result"), "missing header");
                    assert([...response.headers].some(([name]) => name === "x-result"), "invalid entries");
                    assert([...response.headers.keys()].includes("x-result"), "invalid keys");
                    assert([...response.headers.values()].includes("first"), "invalid values");
                    const result = response.json();
                    assert(result instanceof Promise, "json did not return a Promise");
                    assert(response.bodyUsed, "body was not consumed synchronously");
                    return result.then((value) => {
                      assert(value.value === 42, "invalid JSON body");
                      return response.text().then(
                        () => { throw new Error("body was consumed twice"); },
                        (error) => assert(error instanceof TypeError, "invalid body error")
                      );
                    });
                  })
                  .then(() => fetch("http://127.0.0.1:%1$d/binary"))
                  .then((response) => response.arrayBuffer())
                  .then((buffer) => {
                    assert(buffer instanceof ArrayBuffer, "invalid array buffer");
                    assert(
                      Array.from(new Uint8Array(buffer)).join(",") === "0,1,127,128,255",
                      "invalid binary body"
                    );
                    complete = true;
                  })
                  .catch((error) => {
                    failure = String(error?.stack ?? error);
                    complete = true;
                  });
              }

              export function onDisable() {
                if (!complete) throw new Error("fetch did not complete");
                if (failure !== undefined) throw new Error(failure);
              }
              """
                  .formatted(server.getAddress().getPort()));

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
            getClass().getClassLoader(), Optional.empty(), Optional.of("mappings/test-client.txt"));
    return new GraalPackageContextFactory(temporaryDirectory, "0.0.1", environment);
  }

  private GraalPackageContextFactory createCatalogContextFactory() {
    MqpRuntimeEnvironment environment =
        new MqpRuntimeEnvironment(
            getClass().getClassLoader(), Optional.of("catalog/test-client.txt"), Optional.empty());
    return new GraalPackageContextFactory(temporaryDirectory, "0.0.1", environment);
  }

  private PackageContextSpec createSpec(Path entrypoint) {
    return new PackageContextSpec(
        "example-package", entrypoint.getParent().getParent(), entrypoint, getDataDirectory());
  }

  private Path getDataDirectory() {
    return temporaryDirectory.resolve("package-data/example-package").toAbsolutePath().normalize();
  }

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

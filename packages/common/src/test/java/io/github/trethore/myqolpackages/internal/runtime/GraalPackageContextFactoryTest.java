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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.myqolpackages.api.MqpRuntimeEnvironment;
import io.github.trethore.myqolpackages.api.config.FileSystemPermissions;
import io.github.trethore.myqolpackages.api.config.FileSystemReadPermission;
import io.github.trethore.myqolpackages.api.config.FileSystemWritePermission;
import io.github.trethore.myqolpackages.api.config.HostAccessPermission;
import io.github.trethore.myqolpackages.api.config.HostClassLookupPermission;
import io.github.trethore.myqolpackages.api.config.PackagePermissions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
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
  void exposesImmutableMqpMetadata() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            if (mqp.version !== "0.0.1") throw new Error("unexpected MQP version");
            if (mqp.package.id !== "example-package") throw new Error("unexpected package ID");
            if (mqp.permissions.hostAccess !== "none") throw new Error("unexpected host access");
            if (mqp.permissions.filesystem.read !== "none") throw new Error("unexpected read access");
            if (mqp.permissions.has("hostAccess.full")) throw new Error("unexpected full access");
            try { mqp.version = "changed"; } catch (error) {}
            if (mqp.version !== "0.0.1") throw new Error("mutable MQP API");

            export function onEnable() {}
            export function onDisable() {}
            """);

    try (GraalPackageContextFactory contextFactory = createContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint))) {
      assertDoesNotThrow(context::invokeEnable);
    }
  }

  @Test
  void loadsModulesWithPackageReadPermission() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            import { value } from "./value.js";
            if (value !== 42) throw new Error("module was not loaded");

            export function onEnable() {}
            export function onDisable() {}
            """);
    Files.writeString(entrypoint.resolveSibling("value.js"), "export const value = 42;");
    PackagePermissions permissions =
        new PackagePermissions(
            HostAccessPermission.NONE,
            HostClassLookupPermission.NONE,
            new FileSystemPermissions(
                FileSystemReadPermission.PACKAGE, FileSystemWritePermission.NONE));

    try (GraalPackageContextFactory contextFactory = createContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint, permissions))) {
      assertDoesNotThrow(context::invokeEnable);
    }
  }

  @Test
  void rejectsModuleLoadingWithoutReadPermission() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            import "./value.js";
            export function onEnable() {}
            export function onDisable() {}
            """);
    Files.writeString(entrypoint.resolveSibling("value.js"), "export const value = 42;");

    try (GraalPackageContextFactory contextFactory = createContextFactory()) {
      assertThrows(
          PackageLifecycleException.class, () -> contextFactory.create(createSpec(entrypoint)));
    }
  }

  @Test
  void permitsAllHostClassLookupWhenGranted() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            const HostString = Java.type("java.lang.String");
            if (HostString.valueOf(42) !== "42") throw new Error("host lookup failed");

            export function onEnable() {}
            export function onDisable() {}
            """);
    PackagePermissions permissions =
        new PackagePermissions(
            HostAccessPermission.FULL, HostClassLookupPermission.ALL, FileSystemPermissions.none());

    try (GraalPackageContextFactory contextFactory = createContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint, permissions))) {
      assertDoesNotThrow(context::invokeEnable);
    }
  }

  @Test
  void importsArbitraryClassesByFullyQualifiedName() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            const imported = importClass("java.lang.Double");
            const packaged = packages.java.lang.Double;
            if (!imported || imported !== packaged) throw new Error("class proxies differ");

            export function onEnable() {}
            export function onDisable() {}
            """);
    PackagePermissions permissions =
        new PackagePermissions(
            HostAccessPermission.NONE, HostClassLookupPermission.ALL, FileSystemPermissions.none());

    try (GraalPackageContextFactory contextFactory = createContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint, permissions))) {
      assertDoesNotThrow(context::invokeEnable);
    }
  }

  @Test
  void resolvesMappedMinecraftClassesThroughAllMqpApis()
      throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            const fullyQualified = importClass("net.minecraft.test.FakeMappedClass");
            const partial = importClass("FakeMappedClass");
            const packaged = packages.net.minecraft.test.FakeMappedClass;
            const aliased = net.minecraft.test.FakeMappedClass;
            if (fullyQualified !== partial || partial !== packaged || packaged !== aliased) {
              throw new Error("class proxies differ");
            }

            export function onEnable() {}
            export function onDisable() {}
            """);
    PackagePermissions permissions =
        new PackagePermissions(
            HostAccessPermission.NONE,
            HostClassLookupPermission.MINECRAFT,
            FileSystemPermissions.none());

    try (GraalPackageContextFactory contextFactory = createMappedContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint, permissions))) {
      assertDoesNotThrow(context::invokeEnable);
    }
  }

  @Test
  void exposesMappedConstructorsMethodsAndFieldsWithFullHostAccess()
      throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            const Fixture = importClass("net.minecraft.test.FakeMappedClass");
            if (Fixture !== packages.net.minecraft.test.FakeMappedClass) {
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
    PackagePermissions permissions =
        new PackagePermissions(
            HostAccessPermission.FULL,
            HostClassLookupPermission.MINECRAFT,
            FileSystemPermissions.none());

    try (GraalPackageContextFactory contextFactory = createMappedContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint, permissions))) {
      assertDoesNotThrow(context::invokeEnable);
    }
  }

  @Test
  void keepsMappedClassOpaqueWithoutHostAccess() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            const Fixture = importClass("net.minecraft.test.FakeMappedClass");
            if ("_class" in Fixture || "greeting" in Fixture || Fixture.greeting !== undefined) {
              throw new Error("opaque class exposed members");
            }
            let constructionRejected = false;
            try { new Fixture("fixture", 1); } catch (error) { constructionRejected = true; }
            if (!constructionRejected) throw new Error("opaque class was instantiable");
            let wrapRejected = false;
            try { wrap(Fixture); } catch (error) { wrapRejected = true; }
            if (!wrapRejected) throw new Error("wrap was available without host access");

            export function onEnable() {}
            export function onDisable() {}
            """);
    PackagePermissions permissions =
        new PackagePermissions(
            HostAccessPermission.NONE,
            HostClassLookupPermission.MINECRAFT,
            FileSystemPermissions.none());

    try (GraalPackageContextFactory contextFactory = createMappedContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint, permissions))) {
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
    PackagePermissions permissions =
        new PackagePermissions(
            HostAccessPermission.NONE, HostClassLookupPermission.ALL, FileSystemPermissions.none());

    try (GraalPackageContextFactory contextFactory = createCatalogContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint, permissions))) {
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
    PackagePermissions permissions =
        new PackagePermissions(
            HostAccessPermission.NONE,
            HostClassLookupPermission.MINECRAFT,
            FileSystemPermissions.none());

    PackageLifecycleException exception;
    try (GraalPackageContextFactory contextFactory = createMappedContextFactory()) {
      exception =
          assertThrows(
              PackageLifecycleException.class,
              () -> contextFactory.create(createSpec(entrypoint, permissions)));
    }

    assertTrue(exception.getMessage().contains("Ambiguous class name"));
  }

  @Test
  void minecraftHostClassLookupRejectsJavaClasses() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            Java.type("java.lang.String");
            export function onEnable() {}
            export function onDisable() {}
            """);
    PackagePermissions permissions =
        new PackagePermissions(
            HostAccessPermission.FULL,
            HostClassLookupPermission.MINECRAFT,
            FileSystemPermissions.none());

    try (GraalPackageContextFactory contextFactory = createContextFactory()) {
      assertThrows(
          PackageLifecycleException.class,
          () -> contextFactory.create(createSpec(entrypoint, permissions)));
    }
  }

  @Test
  void createsContextWithDataOnlyWritePermission() throws IOException, PackageLifecycleException {
    Path entrypoint =
        createEntrypoint(
            """
            if (!mqp.permissions.has("filesystem.read.data")) throw new Error("data is not readable");
            if (!mqp.permissions.has("filesystem.write.data")) throw new Error("data is not writable");

            export function onEnable() {}
            export function onDisable() {}
            """);
    PackagePermissions permissions =
        new PackagePermissions(
            HostAccessPermission.NONE,
            HostClassLookupPermission.NONE,
            new FileSystemPermissions(
                FileSystemReadPermission.NONE, FileSystemWritePermission.DATA));

    try (GraalPackageContextFactory contextFactory = createContextFactory();
        PackageScriptContext context = contextFactory.create(createSpec(entrypoint, permissions))) {
      assertDoesNotThrow(context::invokeEnable);
      assertTrue(Files.isDirectory(temporaryDirectory.resolve(".data/example-package")));
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
            List.of("net.minecraft.", "com.mojang."),
            Optional.empty(),
            Optional.of("mappings/test-client.txt"));
    return new GraalPackageContextFactory(temporaryDirectory, "0.0.1", environment);
  }

  private GraalPackageContextFactory createCatalogContextFactory() {
    MqpRuntimeEnvironment environment =
        new MqpRuntimeEnvironment(
            getClass().getClassLoader(),
            List.of(),
            Optional.of("catalog/test-client.txt"),
            Optional.empty());
    return new GraalPackageContextFactory(temporaryDirectory, "0.0.1", environment);
  }

  private PackageContextSpec createSpec(Path entrypoint) {
    return createSpec(entrypoint, PackagePermissions.none());
  }

  private PackageContextSpec createSpec(Path entrypoint, PackagePermissions permissions) {
    return new PackageContextSpec(
        "example-package",
        entrypoint.getParent().getParent(),
        entrypoint,
        permissions,
        List.of(temporaryDirectory));
  }
}

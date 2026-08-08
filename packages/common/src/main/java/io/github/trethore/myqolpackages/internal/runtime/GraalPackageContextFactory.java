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

import io.github.trethore.myqolpackages.api.MqpRuntimeEnvironment;
import io.github.trethore.myqolpackages.internal.runtime.api.MqpApiInstaller;
import io.github.trethore.myqolpackages.internal.runtime.http.PackageHttpClient;
import java.io.IOException;
import java.io.OutputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GraalPackageContextFactory implements PackageContextFactory {
  private static final String JAVASCRIPT_LANGUAGE_ID = "js";
  private static final String JAVASCRIPT_MODULE_MIME_TYPE = "application/javascript+module";
  private static final Logger LOGGER = LoggerFactory.getLogger(GraalPackageContextFactory.class);
  private static final Source WARM_UP_SOURCE =
      Source.newBuilder(
              JAVASCRIPT_LANGUAGE_ID,
              "export function onEnable() {} export function onDisable() {}",
              "mqp-warm-up.mjs")
          .mimeType(JAVASCRIPT_MODULE_MIME_TYPE)
          .cached(false)
          .buildLiteral();
  private final Engine engine;
  private final HttpClient httpClient;
  private final MqpApiInstaller mqpApiInstaller;

  private boolean closed;

  public GraalPackageContextFactory(Path mqpDirectory, String mqpVersion) {
    this(
        mqpDirectory,
        mqpVersion,
        MqpRuntimeEnvironment.identity(GraalPackageContextFactory.class.getClassLoader()));
  }

  public GraalPackageContextFactory(
      Path mqpDirectory, String mqpVersion, MqpRuntimeEnvironment environment) {
    Objects.requireNonNull(mqpDirectory, "mqpDirectory");
    Objects.requireNonNull(environment, "environment");
    this.mqpApiInstaller =
        new MqpApiInstaller(Objects.requireNonNull(mqpVersion, "mqpVersion"), environment);
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    this.engine =
        Engine.newBuilder(JAVASCRIPT_LANGUAGE_ID)
            .option("engine.WarnInterpreterOnly", "false")
            .build();
    try {
      warmUpEngine();
    } catch (RuntimeException exception) {
      engine.close();
      throw exception;
    }
  }

  @Override
  public synchronized PackageScriptContext create(PackageContextSpec spec)
      throws PackageLifecycleException {
    if (closed) {
      throw new PackageLifecycleException("Package context factory is already closed");
    }
    GraalPackageContextResources resources;
    try {
      resources = createResources(spec);
    } catch (IOException | RuntimeException exception) {
      throw createLoadFailure(exception);
    }
    try {
      Source entrypointSource =
          Source.newBuilder(JAVASCRIPT_LANGUAGE_ID, spec.entrypoint().toFile())
              .mimeType(JAVASCRIPT_MODULE_MIME_TYPE)
              .build();
      Value exports = resources.getContext().eval(entrypointSource);
      Value onEnable = requireLifecycleHook(exports, "onEnable");
      Value onDisable = requireLifecycleHook(exports, "onDisable");
      return new GraalPackageScriptContext(resources, onEnable, onDisable);
    } catch (IOException | RuntimeException | PackageLifecycleException exception) {
      PackageLifecycleException failure = createLoadFailure(exception);
      try {
        resources.close();
      } catch (PackageLifecycleException closeException) {
        failure.addSuppressed(closeException);
      }
      throw failure;
    }
  }

  @Override
  public synchronized void close() throws PackageLifecycleException {
    if (closed) {
      return;
    }
    closed = true;
    try {
      engine.close();
    } catch (RuntimeException exception) {
      throw new PackageLifecycleException("Could not close JavaScript engine", exception);
    }
  }

  private static Value requireLifecycleHook(Value exports, String hookName)
      throws PackageLifecycleException {
    if (!exports.hasMember(hookName)) {
      throw new PackageLifecycleException("Entrypoint must export " + hookName + "()");
    }
    Value hook = exports.getMember(hookName);
    if (!hook.canExecute()) {
      throw new PackageLifecycleException("Entrypoint export " + hookName + " must be a function");
    }
    return hook;
  }

  private GraalPackageContextResources createResources(PackageContextSpec spec) throws IOException {
    Files.createDirectories(spec.dataDirectory());
    PackageLogOutputStream output = new PackageLogOutputStream(LOGGER, spec.packageId(), false);
    PackageLogOutputStream errorOutput = new PackageLogOutputStream(LOGGER, spec.packageId(), true);
    PackageHttpClient packageHttpClient = new PackageHttpClient(httpClient);
    Context context = null;
    try {
      context = createContext(spec, output, errorOutput);
      mqpApiInstaller.install(context, spec, packageHttpClient);
      return new GraalPackageContextResources(context, output, errorOutput, packageHttpClient);
    } catch (RuntimeException exception) {
      if (context != null) {
        try {
          context.close();
        } catch (RuntimeException closeException) {
          exception.addSuppressed(closeException);
        }
      }
      output.close();
      errorOutput.close();
      packageHttpClient.close();
      throw exception;
    }
  }

  private static PackageLifecycleException createLoadFailure(Exception exception) {
    if (exception instanceof PackageLifecycleException lifecycleException) {
      return lifecycleException;
    }
    return new PackageLifecycleException(
        "Could not load entrypoint: " + exception.getMessage(), exception);
  }

  private void warmUpEngine() {
    try (Context context =
        createContext(null, OutputStream.nullOutputStream(), OutputStream.nullOutputStream())) {
      Value exports = context.eval(WARM_UP_SOURCE);
      exports.getMember("onEnable").execute();
      exports.getMember("onDisable").execute();
    } catch (RuntimeException exception) {
      throw new IllegalStateException("Could not warm up JavaScript engine", exception);
    }
  }

  private Context createContext(
      PackageContextSpec spec, OutputStream output, OutputStream errorOutput) {
    // Giving full power to packages.
    Context.Builder builder =
        Context.newBuilder(JAVASCRIPT_LANGUAGE_ID)
            .engine(engine)
            .allowHostAccess(HostAccess.ALL)
            .allowHostClassLookup(className -> true)
            .allowHostClassLoading(true)
            .allowCreateThread(true)
            .allowCreateProcess(true)
            .allowNativeAccess(true)
            .allowEnvironmentAccess(EnvironmentAccess.NONE)
            .allowPolyglotAccess(PolyglotAccess.NONE)
            .allowIO(IOAccess.ALL)
            .option("js.esm-eval-returns-exports", "true")
            .option("js.ecmascript-version", "2026")
            .out(output)
            .err(errorOutput);
    if (spec != null) {
      builder.currentWorkingDirectory(spec.packageDirectory());
    }
    return builder.build();
  }
}

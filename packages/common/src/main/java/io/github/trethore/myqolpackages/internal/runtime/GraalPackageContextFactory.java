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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
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

  private boolean closed;

  public GraalPackageContextFactory() {
    engine =
        Engine.newBuilder(JAVASCRIPT_LANGUAGE_ID)
            .option("engine.WarnInterpreterOnly", "false")
            .build();
    warmUpEngine();
  }

  @Override
  public synchronized PackageScriptContext create(String packageId, Path entrypoint)
      throws PackageLifecycleException {
    if (closed) {
      throw new PackageLifecycleException("Package context factory is already closed");
    }
    GraalPackageContextResources resources;
    try {
      resources = createResources(packageId);
    } catch (RuntimeException exception) {
      throw createLoadFailure(exception);
    }
    try {
      Source entrypointSource =
          Source.newBuilder(JAVASCRIPT_LANGUAGE_ID, entrypoint.toFile())
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

  private GraalPackageContextResources createResources(String packageId) {
    PackageLogOutputStream output = new PackageLogOutputStream(LOGGER, packageId, false);
    PackageLogOutputStream errorOutput = new PackageLogOutputStream(LOGGER, packageId, true);
    try {
      Context context = createContext(output, errorOutput);
      return new GraalPackageContextResources(context, output, errorOutput);
    } catch (RuntimeException exception) {
      output.close();
      errorOutput.close();
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
        createContext(OutputStream.nullOutputStream(), OutputStream.nullOutputStream())) {
      Value exports = context.eval(WARM_UP_SOURCE);
      exports.getMember("onEnable").execute();
      exports.getMember("onDisable").execute();
    } catch (RuntimeException exception) {
      try {
        engine.close();
      } catch (RuntimeException closeException) {
        exception.addSuppressed(closeException);
      }
      throw new IllegalStateException("Could not warm up JavaScript engine", exception);
    }
  }

  private Context createContext(OutputStream output, OutputStream errorOutput) {
    return Context.newBuilder(JAVASCRIPT_LANGUAGE_ID)
        .engine(engine)
        .allowHostAccess(HostAccess.NONE)
        .allowHostClassLookup(ignored -> false)
        .allowCreateThread(false)
        .allowCreateProcess(false)
        .allowNativeAccess(false)
        .allowEnvironmentAccess(EnvironmentAccess.NONE)
        .allowIO(IOAccess.NONE)
        .option("js.esm-eval-returns-exports", "true")
        .out(output)
        .err(errorOutput)
        .build();
  }
}

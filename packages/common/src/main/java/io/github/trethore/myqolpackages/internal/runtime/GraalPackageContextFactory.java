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

import io.github.trethore.myqolpackages.api.config.FileSystemPermissions;
import io.github.trethore.myqolpackages.api.config.FileSystemReadPermission;
import io.github.trethore.myqolpackages.api.config.FileSystemWritePermission;
import io.github.trethore.myqolpackages.api.config.HostAccessPermission;
import io.github.trethore.myqolpackages.api.config.HostClassLookupPermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GraalPackageContextFactory implements PackageContextFactory {
  private static final String JAVASCRIPT_LANGUAGE_ID = "js";
  private static final String JAVASCRIPT_MODULE_MIME_TYPE = "application/javascript+module";
  private static final String MQP_API_RESOURCE_NAME = "mqp-api.js";
  private static final String PACKAGE_DATA_DIRECTORY_NAME = ".data";
  private static final Logger LOGGER = LoggerFactory.getLogger(GraalPackageContextFactory.class);
  private static final Source WARM_UP_SOURCE =
      Source.newBuilder(
              JAVASCRIPT_LANGUAGE_ID,
              "export function onEnable() {} export function onDisable() {}",
              "mqp-warm-up.mjs")
          .mimeType(JAVASCRIPT_MODULE_MIME_TYPE)
          .cached(false)
          .buildLiteral();
  private static final Source MQP_API_SOURCE = loadMqpApiSource();

  private final EnumMap<HostAccessPermission, Engine> engines =
      new EnumMap<>(HostAccessPermission.class);
  private final Path mqpDirectory;
  private final String mqpVersion;

  private boolean closed;

  public GraalPackageContextFactory(Path mqpDirectory, String mqpVersion) {
    this.mqpDirectory =
        Objects.requireNonNull(mqpDirectory, "mqpDirectory").toAbsolutePath().normalize();
    this.mqpVersion = Objects.requireNonNull(mqpVersion, "mqpVersion");
    getOrCreateEngine(HostAccessPermission.NONE);
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
    PackageLifecycleException failure = null;
    for (Engine engine : engines.values()) {
      try {
        engine.close();
      } catch (RuntimeException exception) {
        if (failure == null) {
          failure = new PackageLifecycleException("Could not close JavaScript engine", exception);
        } else {
          failure.addSuppressed(exception);
        }
      }
    }
    if (failure != null) {
      throw failure;
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
    PackageLogOutputStream output = new PackageLogOutputStream(LOGGER, spec.packageId(), false);
    PackageLogOutputStream errorOutput = new PackageLogOutputStream(LOGGER, spec.packageId(), true);
    Context context = null;
    try {
      context = createContext(spec, output, errorOutput);
      installMqpApi(context, spec);
      return new GraalPackageContextResources(context, output, errorOutput);
    } catch (IOException | RuntimeException exception) {
      if (context != null) {
        try {
          context.close();
        } catch (RuntimeException closeException) {
          exception.addSuppressed(closeException);
        }
      }
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

  private void warmUpEngine(HostAccessPermission hostAccess) {
    try (Context context =
        createContext(
            null, hostAccess, OutputStream.nullOutputStream(), OutputStream.nullOutputStream())) {
      Value exports = context.eval(WARM_UP_SOURCE);
      exports.getMember("onEnable").execute();
      exports.getMember("onDisable").execute();
    } catch (IOException | RuntimeException exception) {
      try {
        engines.get(hostAccess).close();
      } catch (RuntimeException closeException) {
        exception.addSuppressed(closeException);
      }
      throw new IllegalStateException("Could not warm up JavaScript engine", exception);
    }
  }

  private Context createContext(
      PackageContextSpec spec, OutputStream output, OutputStream errorOutput) throws IOException {
    HostAccessPermission hostAccess =
        spec == null ? HostAccessPermission.NONE : spec.permissions().hostAccess();
    return createContext(spec, hostAccess, output, errorOutput);
  }

  private Context createContext(
      PackageContextSpec spec,
      HostAccessPermission hostAccess,
      OutputStream output,
      OutputStream errorOutput)
      throws IOException {
    Context.Builder builder =
        Context.newBuilder(JAVASCRIPT_LANGUAGE_ID)
            .engine(getOrCreateEngine(hostAccess))
            .allowHostAccess(createHostAccess(hostAccess))
            .allowHostClassLookup(className -> isHostClassAllowed(spec, className))
            .allowHostClassLoading(false)
            .allowCreateThread(false)
            .allowCreateProcess(false)
            .allowNativeAccess(false)
            .allowEnvironmentAccess(EnvironmentAccess.NONE)
            .allowPolyglotAccess(PolyglotAccess.NONE)
            .allowIO(createIoAccess(spec))
            .option("js.esm-eval-returns-exports", "true")
            .option("js.ecmascript-version", "2026")
            .out(output)
            .err(errorOutput);
    if (spec != null) {
      Path currentWorkingDirectory = getCurrentWorkingDirectory(spec);
      if (currentWorkingDirectory != null) {
        builder.currentWorkingDirectory(currentWorkingDirectory);
      }
    }
    return builder.build();
  }

  private static HostAccess createHostAccess(HostAccessPermission permission) {
    return permission == HostAccessPermission.FULL ? HostAccess.ALL : HostAccess.NONE;
  }

  private static boolean isHostClassAllowed(PackageContextSpec spec, String className) {
    if (spec == null) {
      return false;
    }
    HostClassLookupPermission permission = spec.permissions().hostClassLookup();
    return permission == HostClassLookupPermission.ALL
        || (permission == HostClassLookupPermission.MINECRAFT
            && (className.startsWith("net.minecraft.")
                || className.startsWith("com.mojang.blaze3d.")));
  }

  private IOAccess createIoAccess(PackageContextSpec spec) throws IOException {
    if (spec == null) {
      return IOAccess.NONE;
    }

    FileSystemPermissions filesystemPermissions = spec.permissions().filesystem();
    FileSystemReadPermission readPermission = filesystemPermissions.read();
    FileSystemWritePermission writePermission = filesystemPermissions.write();
    if (readPermission == FileSystemReadPermission.NONE
        && writePermission == FileSystemWritePermission.NONE) {
      return IOAccess.NONE;
    }
    List<Path> mqpRoots =
        readPermission == FileSystemReadPermission.MQP
                || writePermission == FileSystemWritePermission.MQP
            ? createMqpRoots(spec)
            : List.of();
    List<Path> readRoots = new ArrayList<>();
    if (readPermission == FileSystemReadPermission.PACKAGE) {
      readRoots.add(spec.packageDirectory());
    } else if (readPermission == FileSystemReadPermission.MQP) {
      readRoots.addAll(mqpRoots);
    }
    List<Path> writeRoots = new ArrayList<>();
    if (writePermission == FileSystemWritePermission.DATA) {
      Path dataDirectory = getPackageDataDirectory(spec);
      java.nio.file.Files.createDirectories(dataDirectory);
      writeRoots.add(dataDirectory);
    } else if (writePermission == FileSystemWritePermission.MQP) {
      writeRoots.addAll(mqpRoots);
    } else if (writePermission == FileSystemWritePermission.ALL) {
      return IOAccess.newBuilder().allowHostFileAccess(true).allowHostSocketAccess(false).build();
    }

    FileSystem fileSystem =
        new ScopedPackageFileSystem(
            readRoots, writeRoots, readPermission == FileSystemReadPermission.ALL);
    return IOAccess.newBuilder()
        .fileSystem(FileSystem.allowInternalResources(fileSystem))
        .allowHostSocketAccess(false)
        .build();
  }

  private List<Path> createMqpRoots(PackageContextSpec spec) {
    List<Path> roots = new ArrayList<>(spec.packageRoots());
    if (!roots.contains(mqpDirectory)) {
      roots.add(mqpDirectory);
    }
    return roots;
  }

  private Path getCurrentWorkingDirectory(PackageContextSpec spec) {
    FileSystemPermissions filesystemPermissions = spec.permissions().filesystem();
    if (filesystemPermissions.read() != FileSystemReadPermission.NONE
        || filesystemPermissions.write() == FileSystemWritePermission.MQP
        || filesystemPermissions.write() == FileSystemWritePermission.ALL) {
      return spec.packageDirectory();
    }
    if (filesystemPermissions.write() == FileSystemWritePermission.DATA) {
      return getPackageDataDirectory(spec);
    }
    return null;
  }

  private Path getPackageDataDirectory(PackageContextSpec spec) {
    return mqpDirectory.resolve(PACKAGE_DATA_DIRECTORY_NAME).resolve(spec.packageId());
  }

  private void installMqpApi(Context context, PackageContextSpec spec) {
    FileSystemPermissions filesystemPermissions = spec.permissions().filesystem();
    Value bindings = context.getBindings(JAVASCRIPT_LANGUAGE_ID);
    bindings.putMember("__mqpVersion", mqpVersion);
    bindings.putMember("__mqpPackageId", spec.packageId());
    bindings.putMember("__mqpHostAccess", permissionName(spec.permissions().hostAccess()));
    bindings.putMember(
        "__mqpHostClassLookup", permissionName(spec.permissions().hostClassLookup()));
    bindings.putMember("__mqpFilesystemRead", permissionName(filesystemPermissions.read()));
    bindings.putMember("__mqpFilesystemWrite", permissionName(filesystemPermissions.write()));
    context.eval(MQP_API_SOURCE);
  }

  private static String permissionName(Enum<?> permission) {
    return permission.name().toLowerCase(Locale.ROOT);
  }

  private static Source loadMqpApiSource() {
    try (InputStream input =
        GraalPackageContextFactory.class.getResourceAsStream(MQP_API_RESOURCE_NAME)) {
      if (input == null) {
        throw new IllegalStateException("Missing JavaScript resource: " + MQP_API_RESOURCE_NAME);
      }
      String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      return Source.newBuilder(JAVASCRIPT_LANGUAGE_ID, source, MQP_API_RESOURCE_NAME)
          .cached(true)
          .buildLiteral();
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Could not read JavaScript resource: " + MQP_API_RESOURCE_NAME, exception);
    }
  }

  @SuppressWarnings("resource")
  private Engine getOrCreateEngine(HostAccessPermission hostAccess) {
    Engine existingEngine = engines.get(hostAccess);
    if (existingEngine != null) {
      return existingEngine;
    }
    Engine engine =
        Engine.newBuilder(JAVASCRIPT_LANGUAGE_ID)
            .option("engine.WarnInterpreterOnly", "false")
            .build();
    engines.put(hostAccess, engine);
    try {
      warmUpEngine(hostAccess);
      return engine;
    } catch (RuntimeException exception) {
      engines.remove(hostAccess);
      throw exception;
    }
  }
}

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
package io.github.trethore.myqolpackages.internal.packages;

import io.github.trethore.myqolpackages.api.packages.PackageInfo;
import io.github.trethore.myqolpackages.api.packages.PackageState;
import io.github.trethore.myqolpackages.internal.runtime.PackageContextFactory;
import io.github.trethore.myqolpackages.internal.runtime.PackageLifecycleException;
import io.github.trethore.myqolpackages.internal.runtime.PackageScriptContext;
import java.util.Objects;

final class PackageInstance {
  private final PackageContextFactory contextFactory;

  private PackageDescriptor descriptor;
  private PackageScriptContext scriptContext;
  private PackageState state = PackageState.DISABLED;
  private boolean available = true;

  PackageInstance(PackageDescriptor descriptor, PackageContextFactory contextFactory) {
    this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    this.contextFactory = Objects.requireNonNull(contextFactory, "contextFactory");
  }

  void enable() throws PackageLifecycleException {
    if (state == PackageState.ENABLED) {
      return;
    }
    if (!available) {
      throw new PackageLifecycleException("Package is not currently available");
    }

    PackageScriptContext createdContext;
    try {
      createdContext = contextFactory.create(descriptor.id(), descriptor.entrypoint());
    } catch (PackageLifecycleException exception) {
      state = PackageState.ERROR;
      throw exception;
    }
    try {
      createdContext.invokeEnable();
      scriptContext = createdContext;
      state = PackageState.ENABLED;
    } catch (PackageLifecycleException exception) {
      state = PackageState.ERROR;
      try {
        createdContext.close();
      } catch (PackageLifecycleException closeException) {
        exception.addSuppressed(closeException);
      }
      throw exception;
    }
  }

  void disable() throws PackageLifecycleException {
    PackageScriptContext activeContext = scriptContext;
    scriptContext = null;
    state = PackageState.DISABLED;
    if (activeContext == null) {
      return;
    }

    PackageLifecycleException failure = null;
    try {
      activeContext.invokeDisable();
    } catch (PackageLifecycleException exception) {
      failure = exception;
    }
    try {
      activeContext.close();
    } catch (PackageLifecycleException exception) {
      if (failure == null) {
        failure = exception;
      } else {
        failure.addSuppressed(exception);
      }
    }
    if (failure != null) {
      throw failure;
    }
  }

  void updateDescriptor(PackageDescriptor updatedDescriptor) {
    descriptor = Objects.requireNonNull(updatedDescriptor, "updatedDescriptor");
    available = true;
  }

  void markUnavailable() {
    available = false;
  }

  String getId() {
    return descriptor.id();
  }

  PackageDescriptor getDescriptor() {
    return descriptor;
  }

  PackageInfo getInfo() {
    return descriptor.toInfo(state);
  }

  PackageState getState() {
    return state;
  }

  boolean isAvailable() {
    return available;
  }
}

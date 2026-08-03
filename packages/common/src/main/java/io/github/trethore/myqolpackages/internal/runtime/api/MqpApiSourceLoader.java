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
package io.github.trethore.myqolpackages.internal.runtime.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.graalvm.polyglot.Source;

final class MqpApiSourceLoader {
  private static final String JAVASCRIPT_LANGUAGE_ID = "js";

  private MqpApiSourceLoader() {}

  static MqpApiSources load() {
    return new MqpApiSources(
        load("bootstrap.js", "createMqpBootstrap"),
        load("permissions.js", "createMqpPermissions"),
        load("mqp.js", "installMqp"),
        load("java-interop.js", "installJavaInterop"),
        load("fetch.js", "installFetch"));
  }

  private static Source load(String resourceName, String installerName) {
    try (InputStream input = MqpApiSourceLoader.class.getResourceAsStream(resourceName)) {
      if (input == null) {
        throw new IllegalStateException("Missing JavaScript API resource: " + resourceName);
      }
      String resourceSource = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      String source = "(() => {%n%s%nreturn %s;%n})()".formatted(resourceSource, installerName);
      return Source.newBuilder(JAVASCRIPT_LANGUAGE_ID, source, resourceName)
          .cached(true)
          .buildLiteral();
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Could not read JavaScript API resource: " + resourceName, exception);
    }
  }

  record MqpApiSources(
      Source bootstrap, Source permissions, Source mqp, Source javaInterop, Source fetch) {}
}

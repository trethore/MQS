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
  private static final String JAVASCRIPT_MODULE_MIME_TYPE = "application/javascript+module";

  private MqpApiSourceLoader() {}

  static Source load() {
    return load("runtime-adapter.js");
  }

  @SuppressWarnings("SameParameterValue")
  private static Source load(String resourceName) {
    try (InputStream input = MqpApiSourceLoader.class.getResourceAsStream(resourceName)) {
      if (input == null) {
        throw new IllegalStateException("Missing JavaScript API resource: " + resourceName);
      }
      String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      return Source.newBuilder(JAVASCRIPT_LANGUAGE_ID, source, resourceName)
          .mimeType(JAVASCRIPT_MODULE_MIME_TYPE)
          .cached(true)
          .buildLiteral();
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Could not read JavaScript API resource: " + resourceName, exception);
    }
  }
}

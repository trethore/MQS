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

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import io.github.trethore.myqolpackages.internal.config.MqpGson;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

final class PackageManifestReader {
  private final Gson gson;

  PackageManifestReader() {
    gson = MqpGson.newBuilder().create();
  }

  PackageManifest read(Path manifestPath) throws IOException, PackageValidationException {
    try (Reader reader = Files.newBufferedReader(manifestPath)) {
      PackageManifest manifest = gson.fromJson(reader, PackageManifest.class);
      if (manifest == null) {
        throw new PackageValidationException("Manifest must contain a JSON object");
      }
      return manifest;
    } catch (JsonIOException | JsonSyntaxException exception) {
      throw new PackageValidationException("Invalid manifest.json: " + exception.getMessage());
    }
  }
}

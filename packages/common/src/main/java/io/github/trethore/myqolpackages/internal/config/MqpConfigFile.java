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
package io.github.trethore.myqolpackages.internal.config;

import io.github.trethore.myqolpackages.api.config.MqpConfig;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class MqpConfigFile {
  private static final String CONFIG_FILE_NAME = "config.json";

  private final MqpConfigCodec codec;
  private final Path path;

  MqpConfigFile(Path mqpDirectory, MqpConfigCodec codec) {
    this.path = mqpDirectory.resolve(CONFIG_FILE_NAME);
    this.codec = codec;
  }

  MqpConfig read() throws IOException {
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return codec.read(reader);
    }
  }

  void write(MqpConfig config) throws IOException {
    Files.createDirectories(path.getParent());
    Path temporaryConfig = Files.createTempFile(path.getParent(), CONFIG_FILE_NAME + ".", ".tmp");
    try {
      try (Writer writer = Files.newBufferedWriter(temporaryConfig, StandardCharsets.UTF_8)) {
        codec.write(config, writer);
        writer.write(System.lineSeparator());
      }
      moveIntoPlace(temporaryConfig);
    } finally {
      Files.deleteIfExists(temporaryConfig);
    }
  }

  boolean exists() {
    return Files.exists(path);
  }

  Path path() {
    return path;
  }

  private void moveIntoPlace(Path temporaryConfig) throws IOException {
    try {
      Files.move(
          temporaryConfig,
          path,
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException exception) {
      Files.move(temporaryConfig, path, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}

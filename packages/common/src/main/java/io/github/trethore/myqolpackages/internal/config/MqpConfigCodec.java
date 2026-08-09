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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.trethore.myqolpackages.api.config.MqpConfig;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

final class MqpConfigCodec {
  private static final String MISMATCH_BEHAVIOR_FIELD = "mismatchBehavior";

  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private final MqpConfigValidator validator = new MqpConfigValidator();

  MqpConfig read(Reader reader) {
    JsonElement configurationJson = JsonParser.parseReader(reader);
    validateSerializedBehaviors(configurationJson);
    MqpConfig config = gson.fromJson(configurationJson, MqpConfig.class);
    if (config == null) {
      throw new JsonSyntaxException("Configuration must contain a JSON object");
    }
    validator.validate(config);
    return config;
  }

  void write(MqpConfig config, Writer writer) {
    gson.toJson(config, writer);
  }

  private static void validateSerializedBehaviors(JsonElement configurationJson) {
    if (!configurationJson.isJsonObject()) {
      return;
    }
    JsonObject trust = getObject(configurationJson.getAsJsonObject(), "trust");
    if (trust == null) {
      return;
    }
    JsonObject defaults = getObject(trust, "fingerprintDefaults");
    if (defaults != null) {
      validateBehavior(defaults, "fingerprint defaults");
    }
    JsonObject packages = getObject(trust, "packages");
    if (packages == null) {
      return;
    }
    for (Map.Entry<String, JsonElement> entry : packages.entrySet()) {
      if (!entry.getValue().isJsonObject()) {
        continue;
      }
      JsonObject fingerprint = getObject(entry.getValue().getAsJsonObject(), "fingerprint");
      if (fingerprint != null) {
        validateBehavior(fingerprint, entry.getKey());
      }
    }
  }

  private static JsonObject getObject(JsonObject parent, String fieldName) {
    JsonElement value = parent.get(fieldName);
    return value == null || value.isJsonNull() || !value.isJsonObject()
        ? null
        : value.getAsJsonObject();
  }

  private static void validateBehavior(JsonObject parent, String owner) {
    JsonElement value = parent.get(MISMATCH_BEHAVIOR_FIELD);
    if (value == null || value.isJsonNull()) {
      return;
    }
    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
      throw new JsonSyntaxException(
          "Fingerprint mismatch behavior for " + owner + " must be a string");
    }
    String behavior = value.getAsString();
    if (!behavior.equals("log_only")
        && !behavior.equals("chat_warning")
        && !behavior.equals("block")) {
      throw new JsonSyntaxException(
          "Unknown fingerprint mismatch behavior for " + owner + ": " + behavior);
    }
  }
}

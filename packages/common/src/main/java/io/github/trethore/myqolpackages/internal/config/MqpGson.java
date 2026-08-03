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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import io.github.trethore.myqolpackages.api.config.FileSystemReadPermission;
import io.github.trethore.myqolpackages.api.config.FileSystemWritePermission;
import io.github.trethore.myqolpackages.api.config.HostAccessPermission;
import io.github.trethore.myqolpackages.api.config.HostClassLookupPermission;
import io.github.trethore.myqolpackages.api.config.InternetAccessPermission;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MqpGson {
  private MqpGson() {}

  public static GsonBuilder newBuilder() {
    return new GsonBuilder()
        .registerTypeAdapter(
            HostAccessPermission.class, new LowercaseEnumAdapter<>(HostAccessPermission.class))
        .registerTypeAdapter(
            HostClassLookupPermission.class,
            new LowercaseEnumAdapter<>(HostClassLookupPermission.class))
        .registerTypeAdapter(
            FileSystemReadPermission.class,
            new LowercaseEnumAdapter<>(FileSystemReadPermission.class))
        .registerTypeAdapter(
            FileSystemWritePermission.class,
            new LowercaseEnumAdapter<>(FileSystemWritePermission.class))
        .registerTypeAdapter(
            InternetAccessPermission.class,
            new LowercaseEnumAdapter<>(InternetAccessPermission.class));
  }

  private static final class LowercaseEnumAdapter<T extends Enum<T>> extends TypeAdapter<T> {
    private final Map<String, T> valuesByName = new HashMap<>();

    private LowercaseEnumAdapter(Class<T> enumType) {
      for (T value : enumType.getEnumConstants()) {
        valuesByName.put(value.name().toLowerCase(Locale.ROOT), value);
      }
    }

    @Override
    public void write(JsonWriter writer, T value) throws IOException {
      if (value == null) {
        writer.nullValue();
        return;
      }
      writer.value(value.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public T read(JsonReader reader) throws IOException {
      if (reader.peek() == JsonToken.NULL) {
        reader.nextNull();
        return null;
      }
      String name = reader.nextString();
      T value = valuesByName.get(name);
      if (value == null) {
        throw new JsonSyntaxException("Unknown permission value: " + name);
      }
      return value;
    }
  }
}

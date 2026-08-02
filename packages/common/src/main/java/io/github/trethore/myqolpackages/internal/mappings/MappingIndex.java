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
package io.github.trethore.myqolpackages.internal.mappings;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MappingIndex {
  private final Map<String, String> classMappings;

  private MappingIndex(Map<String, String> classMappings) {
    this.classMappings = Map.copyOf(classMappings);
  }

  public static MappingIndex empty() {
    return new MappingIndex(Map.of());
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getRuntimeClassName(String namedClassName) {
    return classMappings.get(namedClassName);
  }

  public static final class Builder {
    private final Map<String, String> classMappings = new LinkedHashMap<>();

    public Builder add(String namedClassName, String runtimeClassName) {
      String existingName = classMappings.putIfAbsent(namedClassName, runtimeClassName);
      if (existingName != null && !existingName.equals(runtimeClassName)) {
        throw new IllegalArgumentException("Conflicting mapping for class " + namedClassName);
      }
      return this;
    }

    public MappingIndex build() {
      return new MappingIndex(classMappings);
    }
  }
}

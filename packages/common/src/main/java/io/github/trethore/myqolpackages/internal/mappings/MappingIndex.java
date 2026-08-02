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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MappingIndex {
  private final Map<String, ClassMapping> classMappings;
  private final Map<String, String> runtimeClassMappings;

  private MappingIndex(Map<String, ClassMapping> classMappings) {
    this.classMappings = Map.copyOf(classMappings);
    Map<String, String> runtimeMappings = new LinkedHashMap<>();
    Set<String> ambiguousRuntimeNames = new HashSet<>();
    for (ClassMapping classMapping : classMappings.values()) {
      String runtimeClassName = classMapping.runtimeClassName();
      if (ambiguousRuntimeNames.contains(runtimeClassName)) {
        continue;
      }
      String existingNamedClassName =
          runtimeMappings.putIfAbsent(runtimeClassName, classMapping.namedClassName());
      if (existingNamedClassName != null
          && !existingNamedClassName.equals(classMapping.namedClassName())) {
        runtimeMappings.remove(runtimeClassName);
        ambiguousRuntimeNames.add(runtimeClassName);
      }
    }
    this.runtimeClassMappings = Map.copyOf(runtimeMappings);
  }

  public static MappingIndex empty() {
    return new MappingIndex(Map.of());
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getRuntimeClassName(String namedClassName) {
    ClassMapping classMapping = classMappings.get(namedClassName);
    return classMapping == null ? null : classMapping.runtimeClassName();
  }

  public ClassMapping getClassMapping(String namedClassName) {
    return classMappings.get(namedClassName);
  }

  public ClassMapping findClassMapping(Class<?> runtimeClass) {
    ClassMapping identityMapping = classMappings.get(runtimeClass.getName());
    if (identityMapping != null) {
      return identityMapping;
    }
    String namedClassName = runtimeClassMappings.get(runtimeClass.getName());
    return namedClassName == null ? null : classMappings.get(namedClassName);
  }

  public record ClassMapping(
      String namedClassName,
      String runtimeClassName,
      Map<String, List<MethodMapping>> methodMappings,
      Map<String, String> fieldMappings) {
    public ClassMapping {
      Map<String, List<MethodMapping>> methods = new LinkedHashMap<>();
      methodMappings.forEach((name, mappings) -> methods.put(name, List.copyOf(mappings)));
      methodMappings = Map.copyOf(methods);
      fieldMappings = Map.copyOf(fieldMappings);
    }
  }

  public record MethodMapping(String runtimeName, List<String> parameterTypes) {
    public MethodMapping {
      parameterTypes = List.copyOf(parameterTypes);
    }
  }

  public static final class Builder {
    private final Map<String, MutableClassMapping> classMappings = new LinkedHashMap<>();

    public Builder add(String namedClassName, String runtimeClassName) {
      MutableClassMapping existingMapping = classMappings.get(namedClassName);
      if (existingMapping != null) {
        if (!existingMapping.runtimeClassName.equals(runtimeClassName)) {
          throw new IllegalArgumentException("Conflicting mapping for class " + namedClassName);
        }
        return this;
      }
      classMappings.put(namedClassName, new MutableClassMapping(runtimeClassName));
      return this;
    }

    public Builder addMethod(
        String namedClassName,
        String namedMethodName,
        String runtimeMethodName,
        List<String> parameterTypes) {
      MutableClassMapping classMapping = requireClassMapping(namedClassName);
      List<MethodMapping> methodMappings =
          classMapping.methodMappings.computeIfAbsent(
              namedMethodName, ignored -> new ArrayList<>());
      MethodMapping methodMapping = new MethodMapping(runtimeMethodName, parameterTypes);
      if (!methodMappings.contains(methodMapping)) {
        methodMappings.add(methodMapping);
      }
      return this;
    }

    public Builder addField(String namedClassName, String namedFieldName, String runtimeFieldName) {
      MutableClassMapping classMapping = requireClassMapping(namedClassName);
      String existingName =
          classMapping.fieldMappings.putIfAbsent(namedFieldName, runtimeFieldName);
      if (existingName != null && !existingName.equals(runtimeFieldName)) {
        throw new IllegalArgumentException(
            "Conflicting mapping for field " + namedClassName + "." + namedFieldName);
      }
      return this;
    }

    public MappingIndex build() {
      Map<String, ClassMapping> mappings = new LinkedHashMap<>();
      classMappings.forEach(
          (namedClassName, mapping) ->
              mappings.put(
                  namedClassName,
                  new ClassMapping(
                      namedClassName,
                      mapping.runtimeClassName,
                      mapping.methodMappings,
                      mapping.fieldMappings)));
      return new MappingIndex(mappings);
    }

    private MutableClassMapping requireClassMapping(String namedClassName) {
      MutableClassMapping classMapping = classMappings.get(namedClassName);
      if (classMapping == null) {
        throw new IllegalArgumentException("Unknown mapped class " + namedClassName);
      }
      return classMapping;
    }
  }

  private static final class MutableClassMapping {
    private final Map<String, String> fieldMappings = new LinkedHashMap<>();
    private final Map<String, List<MethodMapping>> methodMappings = new LinkedHashMap<>();
    private final String runtimeClassName;

    private MutableClassMapping(String runtimeClassName) {
      this.runtimeClassName = runtimeClassName;
    }
  }
}

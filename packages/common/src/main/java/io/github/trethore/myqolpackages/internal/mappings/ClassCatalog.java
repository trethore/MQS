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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClassCatalog {
  private final Set<String> classes;
  private final Set<String> packages;
  private final Map<String, List<String>> suffixMatches;

  private ClassCatalog(
      Set<String> classes, Set<String> packages, Map<String, List<String>> suffixMatches) {
    this.classes = Set.copyOf(classes);
    this.packages = Set.copyOf(packages);
    this.suffixMatches = Map.copyOf(suffixMatches);
  }

  public static ClassCatalog empty() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public boolean containsPackage(String packageName) {
    return packages.contains(packageName);
  }

  public List<String> findBySuffix(String requestedName) {
    return suffixMatches.getOrDefault(requestedName, List.of());
  }

  public static final class Builder {
    private final Set<String> classes = new LinkedHashSet<>();

    public Builder add(String className) {
      classes.add(normalizeClassName(className));
      return this;
    }

    public Builder addAll(ClassCatalog catalog) {
      classes.addAll(catalog.classes);
      return this;
    }

    public ClassCatalog build() {
      Set<String> packageNames = new HashSet<>();
      Map<String, LinkedHashSet<String>> mutableSuffixMatches = new HashMap<>();
      for (String className : classes) {
        indexPackages(className, packageNames);
        indexSuffixes(className, mutableSuffixMatches);
      }
      Map<String, List<String>> finalizedSuffixMatches = new HashMap<>();
      for (Map.Entry<String, LinkedHashSet<String>> entry : mutableSuffixMatches.entrySet()) {
        List<String> matches = new ArrayList<>(entry.getValue());
        matches.sort(String::compareTo);
        finalizedSuffixMatches.put(entry.getKey(), List.copyOf(matches));
      }
      return new ClassCatalog(classes, packageNames, finalizedSuffixMatches);
    }

    private static void indexPackages(String className, Set<String> packageNames) {
      int separatorIndex = className.indexOf('.');
      while (separatorIndex >= 0) {
        packageNames.add(className.substring(0, separatorIndex));
        separatorIndex = className.indexOf('.', separatorIndex + 1);
      }
    }

    private static void indexSuffixes(
        String className, Map<String, LinkedHashSet<String>> suffixMatches) {
      int separatorIndex = className.indexOf('.');
      if (separatorIndex < 0) {
        addSuffix(suffixMatches, className, className);
      }
      while (separatorIndex >= 0 && separatorIndex < className.length() - 1) {
        addSuffix(suffixMatches, className.substring(separatorIndex + 1), className);
        separatorIndex = className.indexOf('.', separatorIndex + 1);
      }
      int innerClassIndex = className.lastIndexOf('$');
      if (innerClassIndex >= 0 && innerClassIndex < className.length() - 1) {
        addSuffix(suffixMatches, className.substring(innerClassIndex + 1), className);
      }
    }

    private static void addSuffix(
        Map<String, LinkedHashSet<String>> suffixMatches, String suffix, String className) {
      suffixMatches.computeIfAbsent(suffix, ignored -> new LinkedHashSet<>()).add(className);
    }

    private static String normalizeClassName(String className) {
      String normalizedName = className.trim().replace('/', '.');
      if (normalizedName.isEmpty()
          || normalizedName.startsWith(".")
          || normalizedName.endsWith(".")
          || normalizedName.contains("..")) {
        throw new IllegalArgumentException("Invalid class name: " + className);
      }
      return normalizedName;
    }
  }
}

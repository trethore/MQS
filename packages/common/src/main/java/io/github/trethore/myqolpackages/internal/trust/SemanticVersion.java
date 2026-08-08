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
package io.github.trethore.myqolpackages.internal.trust;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record SemanticVersion(
    int major, int minor, int patch, List<String> prerelease, List<String> build)
    implements Comparable<SemanticVersion> {
  public SemanticVersion {
    if (major < 0 || minor < 0 || patch < 0) {
      throw new IllegalArgumentException("Semantic version numbers must not be negative");
    }
    prerelease = List.copyOf(prerelease);
    build = List.copyOf(build);
    validateIdentifiers(prerelease, true);
    validateIdentifiers(build, false);
  }

  public static SemanticVersion parse(String value) {
    Objects.requireNonNull(value, "value");
    String versionWithoutBuild = value;
    String buildValue = null;
    int buildSeparator = value.indexOf('+');
    if (buildSeparator >= 0) {
      if (value.indexOf('+', buildSeparator + 1) >= 0) {
        throw invalidVersion(value);
      }
      versionWithoutBuild = value.substring(0, buildSeparator);
      buildValue = value.substring(buildSeparator + 1);
    }

    String coreValue = versionWithoutBuild;
    String prereleaseValue = null;
    int prereleaseSeparator = versionWithoutBuild.indexOf('-');
    if (prereleaseSeparator >= 0) {
      coreValue = versionWithoutBuild.substring(0, prereleaseSeparator);
      prereleaseValue = versionWithoutBuild.substring(prereleaseSeparator + 1);
    }

    String[] coreIdentifiers = coreValue.split("\\.", -1);
    if (coreIdentifiers.length != 3) {
      throw invalidVersion(value);
    }
    try {
      return new SemanticVersion(
          parseCoreNumber(coreIdentifiers[0], value),
          parseCoreNumber(coreIdentifiers[1], value),
          parseCoreNumber(coreIdentifiers[2], value),
          splitIdentifiers(prereleaseValue),
          splitIdentifiers(buildValue));
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException(
          "Semantic version number is too large: " + value, exception);
    }
  }

  @Override
  public int compareTo(@NotNull SemanticVersion other) {
    int coreComparison = compareCore(other);
    if (coreComparison != 0) {
      return coreComparison;
    }
    if (prerelease.isEmpty()) {
      return other.prerelease.isEmpty() ? 0 : 1;
    }
    if (other.prerelease.isEmpty()) {
      return -1;
    }
    int sharedIdentifiers = Math.min(prerelease.size(), other.prerelease.size());
    for (int index = 0; index < sharedIdentifiers; index++) {
      int identifierComparison =
          comparePrereleaseIdentifier(prerelease.get(index), other.prerelease.get(index));
      if (identifierComparison != 0) {
        return identifierComparison;
      }
    }
    return Integer.compare(prerelease.size(), other.prerelease.size());
  }

  public int compareCore(SemanticVersion other) {
    int majorComparison = Integer.compare(major, other.major);
    if (majorComparison != 0) {
      return majorComparison;
    }
    int minorComparison = Integer.compare(minor, other.minor);
    return minorComparison != 0 ? minorComparison : Integer.compare(patch, other.patch);
  }

  @Override
  public @NotNull String toString() {
    StringBuilder value =
        new StringBuilder().append(major).append('.').append(minor).append('.').append(patch);
    if (!prerelease.isEmpty()) {
      value.append('-').append(String.join(".", prerelease));
    }
    if (!build.isEmpty()) {
      value.append('+').append(String.join(".", build));
    }
    return value.toString();
  }

  private static int parseCoreNumber(String identifier, String fullVersion) {
    if (!isNumeric(identifier) || (identifier.length() > 1 && identifier.charAt(0) == '0')) {
      throw invalidVersion(fullVersion);
    }
    return new BigInteger(identifier).intValueExact();
  }

  private static List<String> splitIdentifiers(String value) {
    return value == null ? List.of() : List.of(value.split("\\.", -1));
  }

  private static void validateIdentifiers(List<String> identifiers, boolean prereleaseIdentifiers) {
    for (String identifier : identifiers) {
      if (identifier.isEmpty() || !hasValidIdentifierCharacters(identifier)) {
        throw new IllegalArgumentException("Invalid semantic version identifier: " + identifier);
      }
      if (prereleaseIdentifiers
          && isNumeric(identifier)
          && identifier.length() > 1
          && identifier.charAt(0) == '0') {
        throw new IllegalArgumentException(
            "Numeric prerelease identifiers must not contain leading zeroes");
      }
    }
  }

  private static int comparePrereleaseIdentifier(String first, String second) {
    boolean firstNumeric = isNumeric(first);
    boolean secondNumeric = isNumeric(second);
    if (firstNumeric && secondNumeric) {
      return new BigInteger(first).compareTo(new BigInteger(second));
    }
    if (firstNumeric) {
      return -1;
    }
    if (secondNumeric) {
      return 1;
    }
    return first.compareTo(second);
  }

  private static boolean hasValidIdentifierCharacters(String value) {
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      boolean letter =
          (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z');
      if (!letter && !isDigit(character) && character != '-') {
        return false;
      }
    }
    return true;
  }

  private static boolean isNumeric(String value) {
    if (value.isEmpty()) {
      return false;
    }
    for (int index = 0; index < value.length(); index++) {
      if (!isDigit(value.charAt(index))) {
        return false;
      }
    }
    return true;
  }

  private static boolean isDigit(char character) {
    return character >= '0' && character <= '9';
  }

  private static IllegalArgumentException invalidVersion(String value) {
    return new IllegalArgumentException("Invalid semantic version: " + value);
  }
}

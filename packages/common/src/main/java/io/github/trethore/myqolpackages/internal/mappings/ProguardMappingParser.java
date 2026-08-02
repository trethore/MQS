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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public final class ProguardMappingParser {
  public ParsedMappings parse(Reader source) throws IOException {
    ClassCatalog.Builder catalog = ClassCatalog.builder();
    MappingIndex.Builder mappings = MappingIndex.builder();
    BufferedReader reader = new BufferedReader(source);
    String line;
    int lineNumber = 0;
    while ((line = reader.readLine()) != null) {
      lineNumber++;
      String trimmedLine = line.trim();
      if (!line.isBlank()
          && !Character.isWhitespace(line.charAt(0))
          && !trimmedLine.startsWith("#")) {
        parseClassLine(trimmedLine, lineNumber, catalog, mappings);
      }
    }
    return new ParsedMappings(catalog.build(), mappings.build());
  }

  private static void parseClassLine(
      String line, int lineNumber, ClassCatalog.Builder catalog, MappingIndex.Builder mappings) {
    int arrowIndex = line.indexOf("->");
    if (arrowIndex < 0 || !line.endsWith(":")) {
      throw new IllegalArgumentException("Invalid class mapping at line " + lineNumber);
    }
    String namedClassName = normalize(line.substring(0, arrowIndex));
    String runtimeClassName = normalize(line.substring(arrowIndex + 2, line.length() - 1));
    if (namedClassName.isEmpty() || runtimeClassName.isEmpty()) {
      throw new IllegalArgumentException("Invalid class mapping at line " + lineNumber);
    }
    catalog.add(namedClassName);
    mappings.add(namedClassName, runtimeClassName);
  }

  private static String normalize(String className) {
    return className.trim().replace('/', '.');
  }

  public record ParsedMappings(ClassCatalog catalog, MappingIndex mappings) {}
}

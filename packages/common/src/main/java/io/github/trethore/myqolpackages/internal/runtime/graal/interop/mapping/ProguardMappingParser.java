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
package io.github.trethore.myqolpackages.internal.runtime.graal.interop.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

final class ProguardMappingParser {
    private static final String CLASS_MAPPING_SUFFIX = ":";
    private static final String COMMENT_PREFIX = "#";
    private static final String MAPPING_SEPARATOR = "->";

    ParsedMappings parse(Reader source) throws IOException {
        ClassCatalog.Builder catalog = ClassCatalog.builder();
        MappingIndex.Builder mappings = MappingIndex.builder();
        BufferedReader reader = new BufferedReader(source);
        String currentClassName = null;
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith(COMMENT_PREFIX)) {
                continue;
            }
            if (!Character.isWhitespace(line.charAt(0))) {
                currentClassName = parseClassLine(trimmedLine, lineNumber, catalog, mappings);
            } else if (currentClassName != null) {
                parseMemberLine(currentClassName, trimmedLine, mappings);
            }
        }
        return new ParsedMappings(catalog.build(), mappings.build());
    }

    private static String parseClassLine(
            String line, int lineNumber, ClassCatalog.Builder catalog, MappingIndex.Builder mappings) {
        int arrowIndex = line.indexOf(MAPPING_SEPARATOR);
        if (arrowIndex < 0 || !line.endsWith(CLASS_MAPPING_SUFFIX)) {
            throw new IllegalArgumentException("Invalid class mapping at line " + lineNumber);
        }
        String namedClassName = normalize(line.substring(0, arrowIndex));
        String runtimeClassName = normalize(
                line.substring(arrowIndex + MAPPING_SEPARATOR.length(), line.length() - CLASS_MAPPING_SUFFIX.length()));
        if (namedClassName.isEmpty() || runtimeClassName.isEmpty()) {
            throw new IllegalArgumentException("Invalid class mapping at line " + lineNumber);
        }
        catalog.add(namedClassName);
        mappings.add(namedClassName, runtimeClassName);
        return namedClassName;
    }

    private static void parseMemberLine(String namedClassName, String line, MappingIndex.Builder mappings) {
        int arrowIndex = line.indexOf(MAPPING_SEPARATOR);
        if (arrowIndex < 0) {
            return;
        }
        String declaration = line.substring(0, arrowIndex).trim();
        String runtimeName =
                line.substring(arrowIndex + MAPPING_SEPARATOR.length()).trim();
        if (declaration.isEmpty() || runtimeName.isEmpty()) {
            return;
        }
        int parenthesisIndex = declaration.indexOf('(');
        if (parenthesisIndex >= 0) {
            String namedMethodName = extractMemberName(declaration, parenthesisIndex);
            if (!namedMethodName.isEmpty()
                    && !namedMethodName.equals("<init>")
                    && !namedMethodName.equals("<clinit>")) {
                mappings.addMethod(
                        namedClassName,
                        namedMethodName,
                        runtimeName,
                        extractParameterTypes(declaration, parenthesisIndex));
            }
            return;
        }
        String namedFieldName = extractMemberName(declaration, declaration.length());
        if (!namedFieldName.isEmpty()) {
            mappings.addField(namedClassName, namedFieldName, runtimeName);
        }
    }

    private static String extractMemberName(String declaration, int endIndex) {
        int spaceIndex = declaration.lastIndexOf(' ', endIndex - 1);
        int startIndex = spaceIndex < 0 ? 0 : spaceIndex + 1;
        return declaration.substring(startIndex, endIndex).trim();
    }

    private static List<String> extractParameterTypes(String declaration, int parenthesisIndex) {
        int closingParenthesisIndex = declaration.lastIndexOf(')');
        if (closingParenthesisIndex <= parenthesisIndex + 1) {
            return List.of();
        }
        return Arrays.stream(declaration
                        .substring(parenthesisIndex + 1, closingParenthesisIndex)
                        .split(","))
                .map(String::trim)
                .map(ProguardMappingParser::normalize)
                .toList();
    }

    private static String normalize(String className) {
        return className.trim().replace('/', '.');
    }

    record ParsedMappings(ClassCatalog catalog, MappingIndex mappings) {}
}

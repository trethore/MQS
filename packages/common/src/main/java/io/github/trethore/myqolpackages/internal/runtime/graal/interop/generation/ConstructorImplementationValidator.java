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
package io.github.trethore.myqolpackages.internal.runtime.graal.interop.generation;

import org.graalvm.polyglot.Value;

final class ConstructorImplementationValidator {
    private static final String SUPER_FUNCTION_NAME = "$super";

    void validate(Value implementation) {
        String source = implementation.toString();
        if (source.stripLeading().startsWith("async")) {
            throw new IllegalArgumentException("Constructor implementation cannot be asynchronous");
        }
        int firstToken = findFirstBodyToken(source);
        if (!source.startsWith(SUPER_FUNCTION_NAME, firstToken)) {
            throw invalid();
        }
        int afterName = firstToken + SUPER_FUNCTION_NAME.length();
        int openParenthesis = skipWhitespaceAndComments(source, afterName);
        if (openParenthesis >= source.length() || source.charAt(openParenthesis) != '(') {
            throw invalid();
        }
        int closeParenthesis = findClosingParenthesis(source, openParenthesis);
        if (closeParenthesis < 0) {
            throw invalid();
        }
        if (source.substring(openParenthesis + 1, closeParenthesis).contains("$self")) {
            throw new IllegalArgumentException("$super(...) arguments cannot use $self");
        }
        int occurrences = countSuperCalls(source);
        if (occurrences != 1) {
            throw new IllegalArgumentException("Constructor implementation must call $super(...) exactly once");
        }
    }

    private static int countSuperCalls(String source) {
        int occurrences = 0;
        int searchStart = 0;
        while (searchStart < source.length()) {
            int occurrence = source.indexOf(SUPER_FUNCTION_NAME, searchStart);
            if (occurrence < 0) {
                return occurrences;
            }
            int nextToken = skipWhitespaceAndComments(source, occurrence + SUPER_FUNCTION_NAME.length());
            if (nextToken < source.length() && source.charAt(nextToken) == '(') {
                occurrences++;
            }
            searchStart = occurrence + SUPER_FUNCTION_NAME.length();
        }
        return occurrences;
    }

    private static int findFirstBodyToken(String source) {
        int arrow = source.indexOf("=>");
        if (arrow >= 0) {
            int token = skipWhitespaceAndComments(source, arrow + 2);
            return token < source.length() && source.charAt(token) == '{'
                    ? skipWhitespaceAndComments(source, token + 1)
                    : token;
        }
        int bodyStart = source.indexOf('{');
        if (bodyStart < 0) {
            throw invalid();
        }
        return skipWhitespaceAndComments(source, bodyStart + 1);
    }

    private static int findClosingParenthesis(String source, int openParenthesis) {
        int depth = 0;
        char quote = 0;
        boolean escaped = false;
        for (int index = openParenthesis; index < source.length(); index++) {
            char character = source.charAt(index);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == quote) {
                    quote = 0;
                }
                continue;
            }
            if (character == '\'' || character == '"' || character == '`') {
                quote = character;
            } else if (character == '(') {
                depth++;
            } else if (character == ')' && --depth == 0) {
                return index;
            }
        }
        return -1;
    }

    private static int skipWhitespaceAndComments(String source, int start) {
        int index = start;
        while (index < source.length()) {
            int nextNonWhitespace = skipWhitespace(source, index);
            if (nextNonWhitespace != index) {
                index = nextNonWhitespace;
            } else if (source.startsWith("//", index)) {
                int newline = source.indexOf('\n', index + 2);
                index = newline < 0 ? source.length() : newline + 1;
            } else if (source.startsWith("/*", index)) {
                int end = source.indexOf("*/", index + 2);
                index = end < 0 ? source.length() : end + 2;
            } else {
                return index;
            }
        }
        return index;
    }

    private static int skipWhitespace(String source, int start) {
        int index = start;
        while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
            index++;
        }
        return index;
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException(
                "Constructor implementation must begin with one direct top-level $super(...) call");
    }
}

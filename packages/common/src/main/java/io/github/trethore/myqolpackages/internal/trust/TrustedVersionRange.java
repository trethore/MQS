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

import io.github.trethore.myqolpackages.api.packages.TrustVersionScope;
import java.util.Objects;

public final class TrustedVersionRange {
    private final String expression;
    private final char operator;
    private final SemanticVersion lowerBound;
    private final SemanticVersion upperBound;

    private TrustedVersionRange(
            String expression, char operator, SemanticVersion lowerBound, SemanticVersion upperBound) {
        this.expression = expression;
        this.operator = operator;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public static TrustedVersionRange parse(String expression) {
        Objects.requireNonNull(expression, "expression");
        if (expression.equals("*")) {
            return new TrustedVersionRange(expression, '*', null, null);
        }
        if (expression.length() < 2) {
            throw new IllegalArgumentException("Invalid trusted version range: " + expression);
        }
        char operator = expression.charAt(0);
        SemanticVersion lowerBound = SemanticVersion.parse(expression.substring(1));
        SemanticVersion upperBound =
                switch (operator) {
                    case '=' -> null;
                    case '~' ->
                        new SemanticVersion(
                                lowerBound.major(),
                                lowerBound.minor() + 1,
                                0,
                                java.util.List.of(),
                                java.util.List.of());
                    case '^' -> compatibleUpperBound(lowerBound);
                    default -> throw new IllegalArgumentException("Unsupported trusted version range: " + expression);
                };
        return new TrustedVersionRange(expression, operator, lowerBound, upperBound);
    }

    public static String create(TrustVersionScope scope, SemanticVersion version) {
        return switch (scope) {
            case EXACT -> "=" + version;
            case PATCH_UPDATES -> "~" + version;
            case COMPATIBLE_UPDATES -> "^" + version;
            case ALL_VERSIONS -> "*";
        };
    }

    public boolean matches(SemanticVersion version) {
        if (operator == '*') {
            return true;
        }
        if (operator == '=') {
            return lowerBound.equals(version);
        }
        if (version.compareTo(lowerBound) < 0 || version.compareTo(upperBound) >= 0) {
            return false;
        }
        return version.prerelease().isEmpty()
                || (!lowerBound.prerelease().isEmpty() && version.compareCore(lowerBound) == 0);
    }

    @Override
    public String toString() {
        return expression;
    }

    private static SemanticVersion compatibleUpperBound(SemanticVersion version) {
        if (version.major() > 0) {
            return new SemanticVersion(version.major() + 1, 0, 0, java.util.List.of(), java.util.List.of());
        }
        if (version.minor() > 0) {
            return new SemanticVersion(0, version.minor() + 1, 0, java.util.List.of(), java.util.List.of());
        }
        return new SemanticVersion(0, 0, version.patch() + 1, java.util.List.of(), java.util.List.of());
    }
}

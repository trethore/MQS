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
package io.github.trethore.myqolpackages.internal.runtime.graal.api.java.interop.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public final class ClassCatalogParser {
    private static final String COMMENT_PREFIX = "#";

    public ClassCatalog parse(Reader source) throws IOException {
        ClassCatalog.Builder catalog = ClassCatalog.builder();
        BufferedReader reader = new BufferedReader(source);
        String line;
        while ((line = reader.readLine()) != null) {
            String className = line.trim();
            if (!className.isEmpty() && !className.startsWith(COMMENT_PREFIX)) {
                catalog.add(className);
            }
        }
        return catalog.build();
    }
}

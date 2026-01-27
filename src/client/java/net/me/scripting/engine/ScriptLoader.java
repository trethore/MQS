/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
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

package net.me.scripting.engine;

import net.me.Main;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ScriptLoader {
    private ScriptLoader() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Map<String, Value> loadModules(Path scriptPath, Context context, ThreadLocal<Map<String, Value>> perFileExports) {
        perFileExports.set(new HashMap<>());
        try {
            Source source = Source.newBuilder(ScriptConstants.JS, scriptPath.toFile())
                    .mimeType("application/javascript+module")
                    .build();
            context.eval(source);

            return perFileExports.get();
        } catch (Exception e) {
            if (e instanceof PolyglotException polyEx && polyEx.isHostException()) {
                Main.LOGGER.error("A Java error occurred while loading script: {}", scriptPath, polyEx.asHostException());
            } else {
                Main.LOGGER.error("Failed to load or parse script file for modules: {}", scriptPath, e);
            }
            return Collections.emptyMap();
        } finally {
            perFileExports.remove();
        }
    }
}

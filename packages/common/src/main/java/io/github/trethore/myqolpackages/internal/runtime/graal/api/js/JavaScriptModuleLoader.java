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
package io.github.trethore.myqolpackages.internal.runtime.graal.api.js;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

public final class JavaScriptModuleLoader {
    private static final String JAVASCRIPT_LANGUAGE_ID = "js";
    private static final String JAVASCRIPT_MODULE_MIME_TYPE = "application/javascript+module";
    private static final ClassValue<Map<String, Source>> SOURCES = new ClassValue<>() {
        @Override
        protected Map<String, Source> computeValue(@NotNull Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    private final Context context;

    public JavaScriptModuleLoader(Context context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public Value loadFunction(Class<?> resourceOwner, String resourceName) {
        Objects.requireNonNull(resourceOwner, "resourceOwner");
        Objects.requireNonNull(resourceName, "resourceName");
        Source source =
                SOURCES.get(resourceOwner).computeIfAbsent(resourceName, name -> loadSource(resourceOwner, name));
        Value function = context.eval(source).getMember("default");
        if (function == null || !function.canExecute()) {
            throw new IllegalStateException(
                    "JavaScript API resource must have a default function export: " + source.getName());
        }
        return function;
    }

    private static Source loadSource(Class<?> resourceOwner, String resourceName) {
        String sourceName = resourceOwner.getPackageName().replace('.', '/') + '/' + resourceName;
        try (InputStream input = resourceOwner.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException("Missing JavaScript API resource: " + sourceName);
            }
            String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return Source.newBuilder(JAVASCRIPT_LANGUAGE_ID, source, sourceName)
                    .mimeType(JAVASCRIPT_MODULE_MIME_TYPE)
                    .cached(true)
                    .buildLiteral();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read JavaScript API resource: " + sourceName, exception);
        }
    }

    public static Value requireFunction(Value object, String member) {
        Value function = Objects.requireNonNull(object, "object").getMember(member);
        if (function == null || !function.canExecute()) {
            throw new IllegalStateException("Missing JavaScript API function: " + member);
        }
        return function;
    }
}

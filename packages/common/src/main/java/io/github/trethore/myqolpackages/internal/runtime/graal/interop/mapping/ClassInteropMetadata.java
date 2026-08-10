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

import io.github.trethore.myqolpackages.api.MqpRuntimeEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public record ClassInteropMetadata(ClassCatalog catalog, MappingIndex mappings) {
    public static ClassInteropMetadata load(MqpRuntimeEnvironment environment) {
        Optional<ClassCatalog> standaloneCatalog =
                environment.classCatalogResource().map(name -> loadCatalog(environment, name));
        Optional<ProguardMappingParser.ParsedMappings> parsedMappings =
                environment.mappingsResource().map(name -> loadMappings(environment, name));
        if (parsedMappings.isEmpty()) {
            return new ClassInteropMetadata(standaloneCatalog.orElseGet(ClassCatalog::empty), MappingIndex.empty());
        }
        ProguardMappingParser.ParsedMappings mappings = parsedMappings.orElseThrow();
        if (standaloneCatalog.isEmpty()) {
            return new ClassInteropMetadata(mappings.catalog(), mappings.mappings());
        }
        ClassCatalog combinedCatalog = ClassCatalog.builder()
                .addAll(standaloneCatalog.orElseThrow())
                .addAll(mappings.catalog())
                .build();
        return new ClassInteropMetadata(combinedCatalog, mappings.mappings());
    }

    private static ClassCatalog loadCatalog(MqpRuntimeEnvironment environment, String resourceName) {
        try (InputStream input = openResource(environment, resourceName)) {
            return new ClassCatalogParser().parse(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Could not load class catalog " + resourceName, exception);
        }
    }

    private static ProguardMappingParser.ParsedMappings loadMappings(
            MqpRuntimeEnvironment environment, String resourceName) {
        try (InputStream input = openResource(environment, resourceName)) {
            return new ProguardMappingParser().parse(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Could not load class mappings " + resourceName, exception);
        }
    }

    private static InputStream openResource(MqpRuntimeEnvironment environment, String resourceName) {
        InputStream input = environment.classLoader().getResourceAsStream(resourceName);
        if (input == null) {
            throw new IllegalStateException("Missing resource " + resourceName);
        }
        return input;
    }
}

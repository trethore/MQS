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

package net.me.scripting.mappings;

import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.lib.mappingio.format.tiny.Tiny1FileReader;
import net.fabricmc.loader.impl.lib.mappingio.tree.MemoryMappingTree;
import net.me.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MappingsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MappingsManager.class);

    @Getter
    private Map<String, String> classMap = Collections.emptyMap();
    @Getter
    private Map<String, Map<String, List<String>>> methodMap = Collections.emptyMap();
    @Getter
    private Map<String, Map<String, String>> fieldMap = Collections.emptyMap();
    @Getter
    private Map<String, String> runtimeToYarnClassMap = Collections.emptyMap();

    private CompletableFuture<Void> initFuture;

    public void init() {
        if (this.initFuture != null) {
            LOGGER.debug("Mappings initialization already started.");
            return;
        }

        LOGGER.info("Starting asynchronous mappings initialization...");
        this.initFuture = CompletableFuture.runAsync(() -> {
            try {
                MemoryMappingTree mappingsTree = parseMappings();
                buildLookupTables(mappingsTree);
                LOGGER.info("Asynchronous mappings initialization successful.");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize mappings asynchronously", e);
                throw new RuntimeException("Failed to load mappings", e);
            }
        });
    }

    public boolean isReady() {
        return this.initFuture != null && this.initFuture.isDone() && !this.initFuture.isCompletedExceptionally();
    }

    public void whenReady(Runnable task) {
        if (initFuture == null) {
            LOGGER.error("whenReady called before init. This should not happen.");
            return;
        }
        this.initFuture.thenRunAsync(task).exceptionally(e -> {
            LOGGER.error("A task scheduled to run after mappings loaded has failed.", e);
            return null;
        });
    }

    private MemoryMappingTree parseMappings() throws IOException {
        MemoryMappingTree mappingsTree = new MemoryMappingTree();
        String fileName = "mappings.tiny";
        try (InputStream in = MappingsManager.class.getClassLoader()
                .getResourceAsStream("assets/" + Main.MOD_ID + "/" + fileName)) {
            if (in == null) {
                throw new IOException("Mappings file " + fileName + " not found in resources");
            }
            InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            Tiny1FileReader.read(reader, mappingsTree);
        }
        return mappingsTree;
    }

    private void buildLookupTables(MemoryMappingTree mappingsTree) {
        boolean isDev = FabricLoader.getInstance().isDevelopmentEnvironment();
        int intermediaryId = mappingsTree.getNamespaceId(MappingNames.INTERMEDIARY.getName());
        int namedId = mappingsTree.getNamespaceId(MappingNames.NAMED.getName());

        int runtimeId = isDev ? namedId : intermediaryId;

        Map<String, String> classes = new HashMap<>();
        Map<String, Map<String, List<String>>> methods = new HashMap<>();
        Map<String, Map<String, String>> fields = new HashMap<>();

        for (MemoryMappingTree.ClassMapping cls : mappingsTree.getClasses()) {
            String yarnName = cls.getName(namedId);
            String runtimeName = cls.getName(runtimeId);
            if (yarnName == null || runtimeName == null) continue;

            yarnName = yarnName.replace('/', '.');
            runtimeName = runtimeName.replace('/', '.');
            classes.put(yarnName, runtimeName);

            // Methods
            Map<String, List<String>> methodLookup = cls.getMethods().stream()
                    .filter(m -> m.getName(namedId) != null)
                    .collect(Collectors.groupingBy(
                            m -> m.getName(namedId),
                            Collectors.mapping(m -> m.getName(runtimeId), Collectors.toList())
                    ));
            methods.put(yarnName, methodLookup);

            // Fields
            Map<String, String> fieldLookup = cls.getFields().stream()
                    .filter(f -> f.getName(namedId) != null)
                    .collect(Collectors.toMap(f -> f.getName(namedId), f -> f.getName(runtimeId), (a, b) -> b));
            fields.put(yarnName, fieldLookup);
        }

        classMap = classes;
        methodMap = methods;
        fieldMap = fields;
        runtimeToYarnClassMap = classes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        LOGGER.info("Mappings tables built in {} mode: {} classes, {} methods, {} fields",
                isDev ? "DEV" : "PROD",
                classMap.size(), methodMap.values().stream().mapToInt(Map::size).sum(),
                fieldMap.values().stream().mapToInt(Map::size).sum());
    }

}

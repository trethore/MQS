/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
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

package net.me.scripting.mappings;

import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.me.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MappingsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MappingsManager.class);
    private static final String MAPPINGS_FILE_NAME = "client.txt";

    @Getter
    private Map<String, String> classMap = Collections.emptyMap();
    @Getter
    private Map<String, Map<String, List<String>>> methodMap = Collections.emptyMap();
    @Getter
    private Map<String, Map<String, String>> fieldMap = Collections.emptyMap();
    @Getter
    private Map<String, String> runtimeToNamedClassMap = Collections.emptyMap();

    private CompletableFuture<Void> initFuture;

    public void init() {
        if (this.initFuture != null) {
            LOGGER.debug("Mappings initialization already started.");
            return;
        }

        LOGGER.info("Starting asynchronous mappings initialization...");
        this.initFuture = CompletableFuture.runAsync(() -> {
            try {
                ProguardMappings mappings = parseMappings();
                buildLookupTables(mappings);
                LOGGER.info("Asynchronous mappings initialization successful.");
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to load mappings from resources: " + MAPPINGS_FILE_NAME, exception);
            } catch (RuntimeException exception) {
                throw new IllegalStateException("Failed to build mappings tables for: " + MAPPINGS_FILE_NAME, exception);
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

    private ProguardMappings parseMappings() throws IOException {
        InputStream inputStream = loadMappingsResource();
        ProguardMappings mappings = new ProguardMappings();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            ProguardClassMapping currentClass = null;
            String line;
            while ((line = reader.readLine()) != null) {
                currentClass = processLine(mappings, currentClass, line);
            }
        }

        return mappings;
    }

    private InputStream loadMappingsResource() throws IOException {
        InputStream inputStream = MappingsManager.class.getClassLoader()
                .getResourceAsStream("assets/" + Main.MOD_ID + "/" + MAPPINGS_FILE_NAME);

        if (inputStream == null) {
            throw new IOException("Mappings file " + MAPPINGS_FILE_NAME + " not found in resources");
        }

        return inputStream;
    }

    private ProguardClassMapping processLine(ProguardMappings mappings, ProguardClassMapping currentClass, String line) {
        if (line.isBlank()) {
            return currentClass;
        }

        String trimmed = line.trim();
        if (trimmed.startsWith("#")) {
            return currentClass;
        }

        boolean isClassDefinition = !Character.isWhitespace(line.charAt(0)) && trimmed.endsWith(":");
        if (isClassDefinition) {
            return parseClassLine(mappings, trimmed);
        }

        if (currentClass != null) {
            parseMemberLine(currentClass, trimmed);
        }

        return currentClass;
    }

    private void buildLookupTables(ProguardMappings mappings) {
        boolean isDev = FabricLoader.getInstance().isDevelopmentEnvironment();

        Map<String, String> classes = new HashMap<>();
        Map<String, Map<String, List<String>>> methods = new HashMap<>();
        Map<String, Map<String, String>> fields = new HashMap<>();

        for (ProguardClassMapping cls : mappings.classes.values()) {
            String namedName = normalizeClassName(cls.namedName());
            if (namedName != null) {
                String runtimeName = normalizeClassName(isDev ? cls.namedName() : cls.officialName());
                if (runtimeName != null) {
                    classes.put(namedName, runtimeName);
                    methods.put(namedName, buildMethodLookup(cls.methods(), isDev));
                    fields.put(namedName, buildFieldLookup(cls.fields(), isDev));
                }
            }
        }

        classMap = classes;
        methodMap = methods;
        fieldMap = fields;
        runtimeToNamedClassMap = classes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        LOGGER.info("Mappings tables built in {} mode: {} classes, {} methods, {} fields",
                isDev ? "DEV" : "PROD",
                classMap.size(), methodMap.values().stream().mapToInt(Map::size).sum(),
                fieldMap.values().stream().mapToInt(Map::size).sum());
    }

    private Map<String, List<String>> buildMethodLookup(List<MemberMapping> classMethods, boolean isDev) {
        Map<String, List<String>> methodLookup = new HashMap<>();
        for (MemberMapping method : classMethods) {
            String namedMethod = method.namedName();
            String runtimeMethod = isDev ? method.namedName() : method.officialName();
            if (namedMethod != null && runtimeMethod != null) {
                methodLookup.computeIfAbsent(namedMethod, _ -> new ArrayList<>()).add(runtimeMethod);
            }
        }
        return methodLookup;
    }

    private Map<String, String> buildFieldLookup(List<MemberMapping> classFields, boolean isDev) {
        Map<String, String> fieldLookup = new HashMap<>();
        for (MemberMapping field : classFields) {
            String namedField = field.namedName();
            String runtimeField = isDev ? field.namedName() : field.officialName();
            if (namedField != null && runtimeField != null) {
                fieldLookup.putIfAbsent(namedField, runtimeField);
            }
        }
        return fieldLookup;
    }

    private ProguardClassMapping parseClassLine(ProguardMappings mappings, String line) {
        int arrowIndex = line.indexOf("->");
        if (arrowIndex < 0) {
            return null;
        }

        String namedName = line.substring(0, arrowIndex).trim();
        String officialName = line.substring(arrowIndex + 2).trim();
        if (officialName.endsWith(":")) {
            officialName = officialName.substring(0, officialName.length() - 1).trim();
        }

        if (namedName.isEmpty() || officialName.isEmpty()) {
            return null;
        }

        ProguardClassMapping mapping = new ProguardClassMapping(namedName, officialName);
        mappings.classes.put(namedName, mapping);
        return mapping;
    }

    private void parseMemberLine(ProguardClassMapping currentClass, String line) {
        int arrowIndex = line.indexOf("->");
        if (arrowIndex < 0) {
            return;
        }

        String left = line.substring(0, arrowIndex).trim();
        String right = line.substring(arrowIndex + 2).trim();
        if (right.isEmpty()) {
            return;
        }

        if (left.contains("(")) {
            String methodName = extractMethodName(left);
            if (!methodName.isEmpty()) {
                currentClass.methods().add(new MemberMapping(methodName, right));
            }
        } else {
            String fieldName = extractFieldName(left);
            if (!fieldName.isEmpty()) {
                currentClass.fields().add(new MemberMapping(fieldName, right));
            }
        }
    }

    private String extractMethodName(String left) {
        int parenIndex = left.indexOf('(');
        if (parenIndex <= 0) {
            return "";
        }

        int nameStart = left.lastIndexOf(' ', parenIndex - 1);
        if (nameStart < 0) {
            nameStart = 0;
        } else {
            nameStart += 1;
        }
        return left.substring(nameStart, parenIndex).trim();
    }

    private String extractFieldName(String left) {
        int lastSpace = left.lastIndexOf(' ');
        if (lastSpace < 0 || lastSpace >= left.length() - 1) {
            return "";
        }
        return left.substring(lastSpace + 1).trim();
    }

    private String normalizeClassName(String name) {
        if (name == null) {
            return null;
        }
        return name.replace('/', '.');
    }

    private static final class ProguardMappings {
        private final Map<String, ProguardClassMapping> classes = new LinkedHashMap<>();
    }

    private record ProguardClassMapping(
            String namedName,
            String officialName,
            List<MemberMapping> methods,
            List<MemberMapping> fields
    ) {
        private ProguardClassMapping(String namedName, String officialName) {
            this(namedName, officialName, new ArrayList<>(), new ArrayList<>());
        }
    }

    private record MemberMapping(String namedName, String officialName) {
    }

}

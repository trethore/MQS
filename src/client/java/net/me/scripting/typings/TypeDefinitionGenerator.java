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

package net.me.scripting.typings;

import net.me.Main;
import net.me.scripting.mappings.MappingsManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generates TypeScript declaration files (.d.ts) for the My QOL Scripts API and Minecraft mappings.
 * <p>
 * This class relies on the {@link MappingsManager} to retrieve the necessary mappings data and uses
 * the {@link MqsApiDtsEmitter} and {@link MinecraftMappingsDtsEmitter} to generate the appropriate TypeScript
 * declarations. Generated files are saved in myqolscripts/scripts/.
 */
public class TypeDefinitionGenerator {
    private static final Path OUTPUT_DIRECTORY = Main.MOD_DIR.resolve("scripts");
    private static final String CORE_OUTPUT_FILE_NAME = "mqs-core.d.ts";
    private static final String MQS_OUTPUT_FILE_NAME = "mqs.d.ts";
    private static final String MC_OUTPUT_FILE_NAME = "mc.d.ts";
    private static final String TS_CONFIG_FILE_NAME = "tsconfig.json";
    private static final String MINECRAFT_UTILITY_FALLBACKS = """
            declare namespace net {
                namespace minecraft {
                    namespace client {
                        interface Minecraft$Instance extends JavaInstance {
                        }
            
                        namespace multiplayer {
                            interface ClientLevel$Instance extends JavaInstance {
                            }
                        }
            
                        namespace player {
                            interface LocalPlayer$Instance extends JavaInstance {
                            }
                        }
                    }
                }
            }
            
            """;
    private static final String TS_CONFIG_CONTENT = """
            {
              "compilerOptions": {
                "allowJs": true,
                "checkJs": true,
                "noEmit": true
              },
              "include": [
                "*.js",
                "*.d.ts"
              ]
            }
            """;

    private final MappingsManager mappingsManager;
    private final MqsApiDtsEmitter mqsApiDtsEmitter;
    private final MinecraftMappingsDtsEmitter minecraftMappingsDtsEmitter;

    public TypeDefinitionGenerator(MappingsManager mappingsManager) {
        this.mappingsManager = mappingsManager;
        this.mqsApiDtsEmitter = new MqsApiDtsEmitter();
        this.minecraftMappingsDtsEmitter = new MinecraftMappingsDtsEmitter();
    }

    public boolean isReady(GenerationTarget target) {
        if (target.requiresMappings()) {
            return mappingsManager.isReady();
        }
        return true;
    }

    public List<GenerationResult> generate(GenerationTarget target) throws IOException {
        if (!isReady(target)) {
            throw new IllegalStateException("Mappings are not ready yet.");
        }

        Files.createDirectories(OUTPUT_DIRECTORY);
        writeTsConfigIfAbsent();
        generateCoreDefinitions();
        boolean includeMinecraftReferenceInApi = target.includesMinecraft() || minecraftDefinitionsExist();

        List<GenerationResult> results = new ArrayList<>();
        if (target.includesApi()) {
            results.add(generateApiDefinitions(includeMinecraftReferenceInApi));
        }
        if (target.includesMinecraft()) {
            results.add(generateMinecraftDefinitions());
        }

        return results;
    }

    public Path getTsConfigPath() {
        return OUTPUT_DIRECTORY.resolve(TS_CONFIG_FILE_NAME);
    }

    private GenerationResult generateApiDefinitions(boolean includeMinecraftReference) throws IOException {
        StringBuilder builder = new StringBuilder(250_000);
        List<String> additionalReferences = includeMinecraftReference ? List.of(MC_OUTPUT_FILE_NAME) : List.of();
        appendHeader(builder, MQS_OUTPUT_FILE_NAME, 0, true, additionalReferences);
        appendMinecraftUtilityFallbacks(builder);
        mqsApiDtsEmitter.append(builder);

        Path outputPath = OUTPUT_DIRECTORY.resolve(MQS_OUTPUT_FILE_NAME);
        writeOutput(outputPath, builder);

        return new GenerationResult(GenerationTarget.API, outputPath, 0, 0, 0);
    }

    private GenerationResult generateMinecraftDefinitions() throws IOException {
        Map<String, String> sortedClassMap = new TreeMap<>(mappingsManager.getClassMap());
        Map<String, Map<String, List<String>>> methodMap = mappingsManager.getMethodMap();
        Map<String, Map<String, String>> fieldMap = mappingsManager.getFieldMap();

        StringBuilder builder = new StringBuilder(2_000_000);
        appendHeader(builder, MC_OUTPUT_FILE_NAME, sortedClassMap.size(), true, List.of());
        minecraftMappingsDtsEmitter.append(builder, sortedClassMap, methodMap, fieldMap);

        Path outputPath = OUTPUT_DIRECTORY.resolve(MC_OUTPUT_FILE_NAME);
        writeOutput(outputPath, builder);

        int classCount = sortedClassMap.size();
        int methodCount = methodMap.values().stream().mapToInt(Map::size).sum();
        int fieldCount = fieldMap.values().stream().mapToInt(Map::size).sum();

        return new GenerationResult(GenerationTarget.MC, outputPath, classCount, methodCount, fieldCount);
    }

    private void generateCoreDefinitions() throws IOException {
        StringBuilder builder = new StringBuilder(2_048);
        appendHeader(builder, CORE_OUTPUT_FILE_NAME, 0, false, List.of());
        TypingsDeclarationUtils.appendCoreDeclarations(builder);

        Path outputPath = OUTPUT_DIRECTORY.resolve(CORE_OUTPUT_FILE_NAME);
        writeOutput(outputPath, builder);
    }

    private void appendHeader(
            StringBuilder builder,
            String fileName,
            int classCount,
            boolean includeCoreReference,
            List<String> additionalReferences
    ) {
        builder.append("// Auto-generated by /mqs generate\n");
        builder.append("// File: ").append(fileName).append("\n");
        if (classCount > 0) {
            builder.append("// Minecraft mapped classes: ").append(classCount).append("\n");
        }
        if (includeCoreReference) {
            builder.append("/// <reference path=\"./").append(CORE_OUTPUT_FILE_NAME).append("\" />\n");
        }
        for (String additionalReference : additionalReferences) {
            builder.append("/// <reference path=\"./").append(additionalReference).append("\" />\n");
        }
        builder.append("\n");
    }

    private boolean minecraftDefinitionsExist() {
        return Files.exists(OUTPUT_DIRECTORY.resolve(MC_OUTPUT_FILE_NAME));
    }

    private void appendMinecraftUtilityFallbacks(StringBuilder builder) {
        builder.append(MINECRAFT_UTILITY_FALLBACKS);
    }

    private void writeOutput(Path outputPath, StringBuilder builder) throws IOException {
        Files.writeString(
                outputPath,
                builder.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private void writeTsConfigIfAbsent() throws IOException {
        Path outputPath = getTsConfigPath();
        if (Files.exists(outputPath)) {
            return;
        }
        Files.writeString(
                outputPath,
                TS_CONFIG_CONTENT,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
    }

    public enum GenerationTarget {
        API(true, false),
        MC(false, true),
        BOTH(true, true);

        private final boolean includesApi;
        private final boolean includesMinecraft;

        GenerationTarget(boolean includesApi, boolean includesMinecraft) {
            this.includesApi = includesApi;
            this.includesMinecraft = includesMinecraft;
        }

        public boolean includesApi() {
            return includesApi;
        }

        public boolean includesMinecraft() {
            return includesMinecraft;
        }

        public boolean requiresMappings() {
            return includesMinecraft;
        }
    }

    public record GenerationResult(GenerationTarget target, Path outputPath, int classCount, int methodCount,
                                   int fieldCount) {
    }
}

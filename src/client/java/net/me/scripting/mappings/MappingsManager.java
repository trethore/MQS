package net.me.scripting.mappings;

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
import java.util.stream.Collectors;

public class MappingsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MappingsManager.class);

    private Map<String, String> classMap = Collections.emptyMap();
    private Map<String, Map<String, List<String>>> methodMap = Collections.emptyMap();
    private Map<String, Map<String, String>> fieldMap = Collections.emptyMap();
    private Map<String, String> runtimeToYarnClassMap = Collections.emptyMap();

    private boolean initialized = false;

    public void init() {
        if (this.initialized) {
            LOGGER.debug("Mappings already initialized.");
            return;
        }

        LOGGER.info("Starting synchronous mappings initialization...");
        try {
            MemoryMappingTree mappingsTree = parseMappings();
            buildLookupTables(mappingsTree);
            this.initialized = true;
            LOGGER.info("Mappings initialization successful.");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize mappings", e);
        }
    }

    public boolean isReady() {
        return this.initialized;
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

        for (var cls : mappingsTree.getClasses()) {
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
                    .collect(Collectors.toMap(f -> f.getName(namedId), f -> f.getName(runtimeId), (a, b) -> b)); // handle duplicates
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

    public Map<String, String> getClassMap() {
        return classMap;
    }

    public Map<String, Map<String, List<String>>> getMethodMap() {
        return methodMap;
    }

    public Map<String, Map<String, String>> getFieldMap() {
        return fieldMap;
    }

    public Map<String, String> getRuntimeToYarnClassMap() {
        return runtimeToYarnClassMap;
    }
}
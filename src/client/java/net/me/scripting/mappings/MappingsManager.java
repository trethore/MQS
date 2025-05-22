package net.me.scripting.mappings;

import net.fabricmc.loader.api.FabricLoader;
import net.me.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import net.fabricmc.loader.impl.lib.mappingio.format.tiny.Tiny1FileReader;
import net.fabricmc.loader.impl.lib.mappingio.tree.MemoryMappingTree;


public class MappingsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MappingsManager.class);
    private MemoryMappingTree mappingsTree = new MemoryMappingTree();
    private Map<String, String> classMap = Collections.emptyMap();
    private Map<String, Map<String, List<String>>> methodMap = Collections.emptyMap();
    private Map<String, Map<String, String>> fieldMap = Collections.emptyMap();
    private Map<String, String> runtimeToYarnClassMap = Collections.emptyMap();

    private final CompletableFuture<Void> initializationFuture = new CompletableFuture<>();
    private final AtomicBoolean initializationStarted = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MappingsManager-Initializer");
        t.setDaemon(true);
        return t;
    });

    private static final MappingsManager INSTANCE = new MappingsManager();

    private MappingsManager() {}

    public static MappingsManager getInstance() {
        return INSTANCE;
    }


    public void init() {
        if (initializationFuture.isDone()) {
            LOGGER.debug("Mappings initialization already completed or in progress.");
            return;
        }
        if (initializationStarted.compareAndSet(false, true)) {
            LOGGER.info("Starting asynchronous mappings initialization...");
            executor.submit(() -> {
                try {
                    parseMappings();
                    buildLookupTables();
                    mappingsTree = null;
                    LOGGER.info("Mappings tree memory released after successful parsing and table building.");
                    initializationFuture.complete(null);
                    LOGGER.info("Mappings initialization successful.");
                } catch (Exception e) {
                    LOGGER.error("Failed to initialize mappings asynchronously", e);
                    initializationFuture.completeExceptionally(e);
                }
            });
        } else {
            LOGGER.debug("Mappings initialization already started or submitted by another call. Current call will not re-submit.");
        }
    }

    private void parseMappings() {
        if (mappingsTree == null) {
            LOGGER.error("parseMappings called but mappingsTree is null. This should not happen with the init() guard.");
            mappingsTree = new MemoryMappingTree();
        }
        String fileName = "mappings.tiny";
        try {
            Path mqs = Path.of(ClassLoader.getSystemResource("assets/" + Main.MOD_ID + "/" + fileName).toURI());
            InputStream in = Files.newInputStream(mqs);
            InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            Tiny1FileReader.read(reader, mappingsTree);
        } catch (IOException e) {
            LOGGER.error("Error parsing mappings file {}: {}", fileName, e.getMessage(), e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildLookupTables() {
        boolean isDev = FabricLoader.getInstance().isDevelopmentEnvironment();
        @SuppressWarnings("unused")
        int officialId = mappingsTree.getNamespaceId(MappingNames.OFFICIAL.getName());
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
                    .collect(Collectors.toMap(f -> f.getName(namedId), f -> f.getName(runtimeId)));
            fields.put(yarnName, fieldLookup);
        }

        classMap = classes;
        methodMap = methods;
        fieldMap = fields;
        runtimeToYarnClassMap = classes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        LOGGER.info("Mappings initialized in {} mode: {} classes, {} methods, {} fields",
                isDev ? "DEV" : "PROD",
                classMap.size(), methodMap.values().stream().mapToInt(Map::size).sum(),
                fieldMap.values().stream().mapToInt(Map::size).sum());
    }

    public Map<String, String> getClassMap() {
        try {
            initializationFuture.join();
        } catch (Exception e) {
            LOGGER.error("Mappings initialization failed. Returning empty map.", e);
            return Collections.emptyMap();
        }
        return classMap;
    }

    public Map<String, Map<String, List<String>>> getMethodMap() {
        try {
            initializationFuture.join();
        } catch (Exception e) {
            LOGGER.error("Mappings initialization failed. Returning empty map.", e);
            return Collections.emptyMap();
        }
        return methodMap;
    }

    public Map<String, Map<String, String>> getFieldMap() {
        try {
            initializationFuture.join();
        } catch (Exception e) {
            LOGGER.error("Mappings initialization failed. Returning empty map.", e);
            return Collections.emptyMap();
        }
        return fieldMap;
    }

    public Map<String, String> getRuntimeToYarnClassMap() {
        try {
            initializationFuture.join();
        } catch (Exception e) {
            LOGGER.error("Mappings initialization failed. Returning empty map.", e);
            return Collections.emptyMap();
        }
        return runtimeToYarnClassMap;
    }
}

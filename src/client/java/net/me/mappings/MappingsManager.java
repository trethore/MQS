package net.me.mappings;

import net.fabricmc.loader.api.FabricLoader;
import net.me.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import java.util.zip.GZIPInputStream;

import net.fabricmc.loader.impl.lib.mappingio.format.tiny.Tiny1FileReader;
import net.fabricmc.loader.impl.lib.mappingio.tree.MemoryMappingTree;


public class MappingsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MappingsManager.class);
    private static final String DOWNLOAD_URL =
            "https://maven.fabricmc.net/net/fabricmc/yarn/1.21.4%2Bbuild.8/yarn-1.21.4%2Bbuild.8-tiny.gz";
    private static final Path OUTPUT_DIR = Main.MOD_DIR.resolve("mappings");
    private static final Path OUTPUT_FILE = OUTPUT_DIR.resolve("yarn" + Main.MC_VERSION + ".tiny");

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
                    downloadMappings();
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

    private void downloadMappings() {
        try {
            Files.createDirectories(OUTPUT_DIR);
            if (Files.exists(OUTPUT_FILE)) {
                LOGGER.debug("Mappings already present at {}", OUTPUT_FILE);
                return;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DOWNLOAD_URL))
                    .header("User-Agent", Main.MOD_ID + "-MappingsDownloader")
                    .build();

            try (InputStream in = new GZIPInputStream(HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofInputStream()).body());
                 OutputStream out = Files.newOutputStream(OUTPUT_FILE)) {
                in.transferTo(out);
            }
            LOGGER.info("Downloaded Yarn mappings to {}", OUTPUT_FILE);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to download or extract mappings: {}", e.getMessage(), e);
        }
    }

    private void parseMappings() {
        if (mappingsTree == null) {
            LOGGER.error("parseMappings called but mappingsTree is null. This should not happen with the init() guard.");
            mappingsTree = new MemoryMappingTree();
        }
        try (Reader reader = Files.newBufferedReader(OUTPUT_FILE)) {
            Tiny1FileReader.read(reader, mappingsTree);
        } catch (IOException e) {
            LOGGER.error("Error parsing mappings file {}: {}", OUTPUT_FILE, e.getMessage(), e);
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

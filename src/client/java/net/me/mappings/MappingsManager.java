package net.me.mappings;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.lib.mappingio.format.tiny.Tiny1FileReader;
import net.fabricmc.loader.impl.lib.mappingio.tree.MemoryMappingTree;
import net.me.Main;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class MappingsManager {
    private static final String DOWNLOAD_URL =
            "https://maven.fabricmc.net/net/fabricmc/yarn/1.21.4%2Bbuild.8/yarn-1.21.4%2Bbuild.8-tiny.gz";
    private static final Path OUTPUT_DIR = FabricLoader.getInstance().getGameDir().resolve(Main.MOD_ID).resolve("mappings");
    private static final Path OUTPUT_FILE = OUTPUT_DIR.resolve("yarn"+ Main.MC_VERSION+".tiny");

    private MemoryMappingTree mappingsTree;
    private Map<String, String>    classMap;
    private Map<String,Map<String, List<String>>> methodMap;
    private Map<String,Map<String,String>> fieldMap;
    private Map<String, String> runtimeToYarnClassMap; // Add this field

    private static MappingsManager instance;

    private MappingsManager() {}

    public static MappingsManager getInstance() {
        if (instance == null) {
            instance = new MappingsManager();
        }
        return instance;
    }

    public void init() {
        downloadMappings();
        parseMappings();
        buildLookupTables();
    }

    private void buildLookupTables() {
        boolean isDev = FabricLoader.getInstance().isDevelopmentEnvironment();
        String yarnType    = MappingNames.NAMED.getName();
        String runtimeType = isDev ? yarnType : MappingNames.INTERMEDIARY.getName();

        classMap  = new HashMap<>();
        methodMap = new HashMap<>();
        fieldMap  = new HashMap<>();

        for (var cls : mappingsTree.getClasses()) {
            String yarnRaw    = cls.getName(yarnType);
            String runtimeRaw = cls.getName(runtimeType);
            if (yarnRaw == null || runtimeRaw == null) continue;

            String yarnFqcn    = yarnRaw.replace('/', '.');
            String runtimeFqcn = runtimeRaw.replace('/', '.');
            classMap.put(yarnFqcn, runtimeFqcn);

            Map<String, List<String>> mm = new HashMap<>();
            for (var m : cls.getMethods()) {
                String yName = m.getName(yarnType);
                String rName = m.getName(runtimeType);
                if (yName != null && rName != null) {
                    mm.computeIfAbsent(yName, k -> new ArrayList<>()).add(rName);
                }
            }
            methodMap.put(yarnFqcn, mm);

            Map<String, String> fm = new HashMap<>();
            for (var f : cls.getFields()) {
                String yName = f.getName(yarnType);
                String rName = f.getName(runtimeType);
                if (yName != null && rName != null) {
                    fm.put(yName, rName);
                }
            }
            fieldMap.put(yarnFqcn, fm);
        }

        runtimeToYarnClassMap = classMap.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getValue,
                        Map.Entry::getKey
                ));

        Main.LOGGER.info(
                "Lookup tables built for {} mode: {} classes",
                isDev ? "DEV (named)" : "PROD (intermediary)",
                classMap.size()
        );
    }


    private void downloadMappings() {
        try (HttpClient client = HttpClient.newHttpClient()){
            Files.createDirectories(OUTPUT_DIR);

            if (Files.exists(OUTPUT_FILE)) {
                System.out.println("Mappings already downloaded at " + OUTPUT_FILE);
                return;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DOWNLOAD_URL))
                    .header("User-Agent", "MyQOLScripts-Mod")
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream is = new GZIPInputStream(response.body());
                 OutputStream os = Files.newOutputStream(OUTPUT_FILE)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
            }
            Main.LOGGER.info("Mappings downloaded to {}", OUTPUT_FILE);
        } catch (IOException | InterruptedException e) {
            Main.LOGGER.error("Failed to download or extract mappings.", e);
            e.printStackTrace();
        }
    }
    private void parseMappings() {
        mappingsTree = new MemoryMappingTree();
        try (Reader reader = Files.newBufferedReader(OUTPUT_FILE)) {
            Tiny1FileReader.read(reader, mappingsTree);
            Main.LOGGER.info("Parsed {} classes from yarn.tiny", mappingsTree.getClasses().size());
        } catch (IOException e) {
            Main.LOGGER.error("Erreur parsing yarn.tiny", e);
        }
    }

    public MemoryMappingTree getMappingsTree() {
        return mappingsTree;
    }
    public Map<String,String> getClassMap() { return classMap; }
    public Map<String,Map<String,List<String>>> getMethodMap() { return methodMap; }
    public Map<String,Map<String,String>>       getFieldMap()  { return fieldMap;  }
    public Map<String, String> getRuntimeToYarnClassMap() { return runtimeToYarnClassMap; }
}

package net.me.mappings;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.lib.mappingio.format.tiny.Tiny1FileReader;
import net.fabricmc.loader.impl.lib.mappingio.tree.MemoryMappingTree;
import net.me.Main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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
    private Map<String, String> runtimeToYarnClassMap;

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

    // Dans MappingsManager.java

    private void buildLookupTables() {
        boolean isDev = FabricLoader.getInstance().isDevelopmentEnvironment();
        Main.LOGGER.info("[MappingsManager Debug] isDevelopmentEnvironment: {}", isDev);

        String officialNsString = MappingNames.OFFICIAL.getName();     // "official"
        String intermediaryNsString = MappingNames.INTERMEDIARY.getName(); // "intermediary"
        String namedNsString = MappingNames.NAMED.getName();         // "named"

        // Obtenir les IDs numériques de ces namespaces à partir de l'arbre de mapping
        // SRC_NAMESPACE_ID est -2, les Dst sont 0, 1, ...
        int officialId = mappingsTree.getNamespaceId(officialNsString);         // Devrait être -2 (SRC_NAMESPACE_ID)
        int intermediaryId = mappingsTree.getNamespaceId(intermediaryNsString); // Devrait être 0 (premier Dst)
        int namedId = mappingsTree.getNamespaceId(namedNsString);             // Devrait être 1 (second Dst)

        Main.LOGGER.info("[MappingsManager Debug] Raw Namespace IDs from getNamespaceId(String): officialId={}, intermediaryId={}, namedId={}",
                officialId, intermediaryId, namedId);

        // Vérification des IDs
        if (officialId != -2) { // SRC_NAMESPACE_ID est -2
            Main.LOGGER.warn("[MappingsManager Warning] Expected 'official' namespace to be the source (ID {}), but got {}. This might indicate a non-standard tiny file header or structure for 'official'.", mappingsTree.getSrcNamespace(), officialId);
            // Tenter un fallback si le premier namespace de l'en-tête est "official" mais n'est pas le src.
            // Cela ne devrait pas arriver avec un fichier tiny v1 standard "v1 official intermediary named".
            if(mappingsTree.getSrcNamespace().equals(officialNsString)) officialId = 0; else officialId = -2; // Risqué mais tente de corriger.
            Main.LOGGER.warn("[MappingsManager Warning] Corrected officialId to: {}.", officialId);
        }
        if (intermediaryId < 0) { // Devrait être 0 ou plus
            throw new IllegalStateException("Namespace 'intermediary' not found or invalid ID: " + intermediaryId);
        }
        if (namedId < 0) { // Devrait être 0 ou plus
            throw new IllegalStateException("Namespace 'named' not found or invalid ID: " + namedId);
        }

        // IDs à utiliser pour extraire les données des méthodes/champs/classes
        // Ce sont les IDs que les méthodes comme cls.getName(id), m.getName(id), m.getDesc(id) attendent.
        final int yarnNameExtractId = namedId; // Utiliser l'ID du namespace "named" pour les noms Yarn
        final int runtimeNameExtractId = isDev ? namedId : intermediaryId; // "named" en dev, "intermediary" en prod
        final int descriptorExtractId = officialId; // Toujours "official" (SRC_NAMESPACE_ID) pour les descripteurs

        Main.LOGGER.info("[MappingsManager Debug] Using Namespace IDs for extraction -> yarnNameSource: {} (ID {}), runtimeNameSource: {} (ID {}), descriptorSource: {} (ID {})",
                namedNsString, yarnNameExtractId,
                isDev ? namedNsString : intermediaryNsString, runtimeNameExtractId,
                officialNsString, descriptorExtractId);

        classMap = new HashMap<>();
        methodMap = new HashMap<>();
        fieldMap = new HashMap<>();

        for (var cls : mappingsTree.getClasses()) {
            String yarnFqcnRaw = cls.getName(yarnNameExtractId);
            String runtimeFqcnRaw = cls.getName(runtimeNameExtractId);

            if (yarnFqcnRaw == null) {
                // Si le nom "named" n'existe pas, on peut essayer de prendre "intermediary" ou "official" comme fallback pour le nom yarn
                // C'est un cas limite, normalement toutes les classes devraient avoir un nom "named".
                String fallbackName = cls.getName(intermediaryId);
                if (fallbackName == null) fallbackName = cls.getName(officialId);
                Main.LOGGER.warn("[MappingsManager Debug] Class {} has null yarnName (named). Using fallback: {}. RuntimeName: {}",
                        cls.getName(officialId), fallbackName, runtimeFqcnRaw);
                if (fallbackName == null) continue; // Impossible de déterminer un nom Yarn
                yarnFqcnRaw = fallbackName;
            }
            if (runtimeFqcnRaw == null) {
                Main.LOGGER.warn("[MappingsManager Debug] Class {} ({}) has null runtimeName. Skipping.",
                        yarnFqcnRaw, cls.getName(officialId));
                continue;
            }

            String yarnFqcn = yarnFqcnRaw.replace('/', '.');
            String runtimeFqcn = runtimeFqcnRaw.replace('/', '.');
            classMap.put(yarnFqcn, runtimeFqcn);

            Map<String, List<String>> mm = new HashMap<>();

            if ("net.minecraft.util.thread.ThreadExecutor".equals(yarnFqcn)) {
                Main.LOGGER.info("[MappingsManager Debug] Processing methods for ThreadExecutor (Yarn: {})", yarnFqcn);
                boolean foundExecuteInLoop = false;
                for (var m : cls.getMethods()) {
                    String yName = m.getName(yarnNameExtractId);
                    String rName = m.getName(runtimeNameExtractId);
                    String desc = m.getDesc(descriptorExtractId);

                    Main.LOGGER.info("[MappingsManager Debug]   ThreadExecutor Method: yarnName='{}', runtimeName='{}', desc='{}'",
                            yName, rName, desc);

                    if (yName != null && rName != null && desc != null) { // S'assurer que le descripteur est aussi non-null
                        if ("execute".equals(yName) && "(Ljava/lang/Runnable;)V".equals(desc)) {
                            Main.LOGGER.info("[MappingsManager Debug]     ThreadExecutor 'execute(Runnable)' method IS being added to mm: {} [{}] -> {} [{}]", yName, desc, rName, desc);
                            foundExecuteInLoop = true;
                        }
                        mm.computeIfAbsent(yName, k -> new ArrayList<>()).add(rName);
                    } else {
                        if (desc != null && "(Ljava/lang/Runnable;)V".equals(desc) && (yName == null || rName == null)) {
                            Main.LOGGER.warn("[MappingsManager Debug]     ThreadExecutor 'execute(Runnable)' candidate SKIPPED: yName='{}', rName='{}', desc='{}'", yName, rName, desc);
                        } else if (yName != null && "execute".equals(yName) && desc == null) {
                            Main.LOGGER.warn("[MappingsManager Debug]     ThreadExecutor method 'execute' found but desc is null. Skipping.");
                        }
                    }
                }
                if (!foundExecuteInLoop) {
                    Main.LOGGER.warn("[MappingsManager Debug]   ThreadExecutor: 'execute(Runnable)' with yarnName='execute' and desc='(Ljava/lang/Runnable;)V' was NOT FOUND or NOT ADDED in the loop over cls.getMethods() for ThreadExecutor. Yarn does not map it directly or issue with name/desc resolution.");
                }
            } else {
                for (var m : cls.getMethods()) {
                    String yName = m.getName(yarnNameExtractId);
                    String rName = m.getName(runtimeNameExtractId);
                    // String desc = m.getDesc(descriptorExtractId); // Pas nécessaire pour le mapping général
                    if (yName != null && rName != null) {
                        mm.computeIfAbsent(yName, k -> new ArrayList<>()).add(rName);
                    }
                }
            }
            methodMap.put(yarnFqcn, mm);

            Map<String, String> fm = new HashMap<>();
            for (var f : cls.getFields()) {
                String yName = f.getName(yarnNameExtractId);
                String rName = f.getName(runtimeNameExtractId);
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
                "Lookup tables built. Mode: {}, Classes: {}, Methods in map for MCClient: {}, ThreadExecutor: {}",
                isDev ? "DEV (named)" : "PROD (" + intermediaryNsString + ")",
                classMap.size(),
                methodMap.getOrDefault("net.minecraft.client.MinecraftClient", Collections.emptyMap()).size(),
                methodMap.getOrDefault("net.minecraft.util.thread.ThreadExecutor", Collections.emptyMap()).size()
        );
        if (methodMap.containsKey("net.minecraft.util.thread.ThreadExecutor")) {
            Main.LOGGER.info("[MappingsManager Debug] Methods for ThreadExecutor AFTER processing: {}", methodMap.get("net.minecraft.util.thread.ThreadExecutor").keySet());
        }
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
    @SuppressWarnings("unused")
    public MemoryMappingTree getMappingsTree() {
        return mappingsTree;
    }
    public Map<String,String> getClassMap() { return classMap; }
    public Map<String,Map<String,List<String>>> getMethodMap() { return methodMap; }
    public Map<String,Map<String,String>>       getFieldMap()  { return fieldMap;  }
    public Map<String, String> getRuntimeToYarnClassMap() { return runtimeToYarnClassMap; }
}
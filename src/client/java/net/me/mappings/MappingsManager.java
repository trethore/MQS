package net.me.mappings;

import net.fabricmc.loader.api.FabricLoader;
import net.me.Main;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.zip.GZIPInputStream;

public class MappingsManager {
    private static final String DOWNLOAD_URL =
            "https://maven.fabricmc.net/net/fabricmc/yarn/1.21.4%2Bbuild.8/yarn-1.21.4%2Bbuild.8-tiny.gz";
    private static final Path OUTPUT_DIR = FabricLoader.getInstance().getGameDir().resolve(Main.MOD_ID).resolve("mappings");
    private static final Path OUTPUT_FILE = OUTPUT_DIR.resolve("yarn"+ Main.MC_VERSION+".tiny");

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

            System.out.println("Mappings downloaded to " + OUTPUT_FILE);
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to download or extract mappings.");
            e.printStackTrace();
        }
    }
}

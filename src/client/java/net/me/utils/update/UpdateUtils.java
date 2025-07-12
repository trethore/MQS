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

package net.me.utils.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.me.Main;
import net.me.utils.AssetIdentifiers;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class UpdateUtils {

    private static final Path UPDATES_FOLDER = Main.MOD_DIR.resolve("updates");
    private static final String UPDATE_FILE_NAME = "my-qol-scripts-update.jar";

    private static final String GITHUB_KEY_TAG_NAME = "tag_name";
    private static final String GITHUB_KEY_BODY = "body";
    private static final String GITHUB_KEY_ASSETS = "assets";
    private static final String GITHUB_KEY_ASSET_NAME = "name";
    private static final String GITHUB_KEY_DOWNLOAD_URL = "browser_download_url";

    private static boolean shutdownHookAdded = false;

    public static void checkForUpdateAsync(Consumer<UpdateCheckResult> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                UpdateInfo updateInfo = checkForUpdate();

                if (updateInfo == null) {
                    callback.accept(new UpdateCheckResult(UpdateStatus.ERROR, "Failed to check for updates", null));
                    return;
                }

                if (!updateInfo.hasUpdate()) {
                    callback.accept(new UpdateCheckResult(UpdateStatus.UP_TO_DATE,
                            "Already on latest version (" + VersionUtils.getCurrentVersion() + ")", updateInfo));
                    return;
                }

                callback.accept(new UpdateCheckResult(UpdateStatus.UPDATE_AVAILABLE,
                        "Update available: " + updateInfo.version(), updateInfo));

            } catch (Exception e) {
                callback.accept(new UpdateCheckResult(UpdateStatus.ERROR, "Exception: " + e.getMessage(), null));
                Main.LOGGER.error("Update check failed", e);
            }
        });
    }

    public static UpdateResult downloadAndPrepareUpdate(UpdateInfo updateInfo) {
        File currentJar = getCurrentJarFile();
        if (currentJar == null) {
            Main.LOGGER.error("Could not find current JAR file to update.");
            return UpdateResult.ERROR;
        }

        File updateFile = getUpdateFile();

        if (updateFile.exists()) {
            return UpdateResult.UPDATE_PENDING;
        }

        try {
            Files.createDirectories(UPDATES_FOLDER);
            URL downloadUrl = new URI(updateInfo.downloadUrl()).toURL();
            try (InputStream in = downloadUrl.openStream()) {
                Files.copy(in, updateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            addShutdownHook(currentJar, updateFile);

            return UpdateResult.SUCCESSFUL;

        } catch (Exception e) {
            if (updateFile.exists()) {
                try {
                    Files.delete(updateFile.toPath());
                } catch (IOException ioException) {
                    Main.LOGGER.error("Could not delete temporary update file", ioException);
                }
            }
            Main.LOGGER.error("Failed to download or prepare update", e);
            return UpdateResult.ERROR;
        }
    }

    private static UpdateInfo checkForUpdate() {
        try {
            URL url = new URI(AssetIdentifiers.URL_GITHUB_API_BASE + AssetIdentifiers.GITHUB_REPO + "/releases/latest").toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestProperty("User-Agent", "MQS-Updater");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                Main.LOGGER.error("Failed to check for updates. Response code: {}", responseCode);
                return null;
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            JsonObject release = JsonParser.parseString(response.toString()).getAsJsonObject();
            String latestVersion = release.get(GITHUB_KEY_TAG_NAME).getAsString();
            String changelog = release.has(GITHUB_KEY_BODY) && !release.get(GITHUB_KEY_BODY).isJsonNull() ? release.get(GITHUB_KEY_BODY).getAsString() : "No changelog available";

            String downloadUrl = null;
            if (release.has(GITHUB_KEY_ASSETS) && release.get(GITHUB_KEY_ASSETS).isJsonArray()) {
                var assets = release.get(GITHUB_KEY_ASSETS).getAsJsonArray();
                for (var asset : assets) {
                    var assetObj = asset.getAsJsonObject();
                    String fileName = assetObj.get(GITHUB_KEY_ASSET_NAME).getAsString();

                    if (fileName.endsWith(".jar") && !fileName.endsWith("-sources.jar") && !fileName.endsWith("-dev.jar")) {
                        downloadUrl = assetObj.get(GITHUB_KEY_DOWNLOAD_URL).getAsString();
                        break;
                    }
                }
            }

            if (downloadUrl == null) {
                Main.LOGGER.error("No suitable .jar file asset found in latest release on GitHub.");
                return null;
            }

            String currentVersion = VersionUtils.getCurrentVersion();
            boolean hasUpdate = VersionUtils.isVersionNewer(latestVersion, currentVersion);

            return new UpdateInfo(latestVersion, changelog, downloadUrl, hasUpdate);

        } catch (Exception e) {
            Main.LOGGER.error("Exception while checking for updates", e);
            return null;
        }
    }

    private static File getUpdateFile() {
        return UPDATES_FOLDER.resolve(UPDATE_FILE_NAME).toFile();
    }

    private static File getCurrentJarFile() {
        try {
            return FabricLoader.getInstance()
                    .getModContainer(Main.MOD_ID)
                    .flatMap(modContainer -> modContainer.getOrigin().getPaths().stream().findFirst())
                    .map(Path::toFile)
                    .filter(File::exists)
                    .orElse(null);
        } catch (Exception e) {
            Main.LOGGER.error("Failed to get current JAR file", e);
            return null;
        }
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (sourceFile == null || destFile == null) {
            throw new IllegalArgumentException("Source and destination files must not be null.");
        }
        try {
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.FileSystemException e) {
            Main.LOGGER.warn("Standard copy failed, trying with FileChannel transfer. Error: {}", e.getMessage());
            copyFileWithChannel(sourceFile, destFile);
        }
    }

    private static void copyFileWithChannel(File source, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(dest);
             FileChannel sourceChannel = fis.getChannel();
             FileChannel destChannel = fos.getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }

    private static synchronized void addShutdownHook(File oldJar, File newJar) {
        if (shutdownHookAdded) return;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (oldJar == null || !oldJar.exists() || oldJar.isDirectory()) {
                    Main.LOGGER.error("Old JAR file not found or is a directory, cannot apply update.");
                    return;
                }
                if (newJar == null || !newJar.exists() || newJar.isDirectory()) {
                    Main.LOGGER.error("New JAR file not found or is a directory, cannot apply update.");
                    return;
                }
                copyFile(newJar, oldJar);
                Files.delete(newJar.toPath());
                Main.LOGGER.info("Successfully applied update!");
            } catch (IOException e) {
                Main.LOGGER.error("Failed to apply update. You may need to replace the JAR manually.", e);
            }
        }, "MQS-Updater-Hook"));

        shutdownHookAdded = true;
    }


    public enum UpdateStatus {
        UPDATE_AVAILABLE,
        UP_TO_DATE,
        ERROR
    }

    public enum UpdateResult {
        SUCCESSFUL("Update downloaded! It will be applied when you restart Minecraft."),
        UPDATE_PENDING("An update is already downloaded. It will be applied on restart."),
        ERROR("Failed to download update. Check logs for details.");

        private final String message;

        UpdateResult(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public record UpdateInfo(String version, String changelog, String downloadUrl, boolean hasUpdate) {
    }

    public record UpdateCheckResult(UpdateStatus status, String message, UpdateInfo updateInfo) {
    }
}
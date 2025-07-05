package net.me.utils.update;

import net.fabricmc.loader.api.FabricLoader;
import net.me.Main;

public class VersionUtils {
    public static String getCurrentVersion() {
        return FabricLoader.getInstance().getModContainer(Main.MOD_ID)
                .map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");
    }

    public static boolean isVersionNewer(String newVersionStr, String currentVersionStr) {
        if (newVersionStr == null || currentVersionStr == null) return false;

        String newVersion = newVersionStr.replace("v", "");
        String currentVersion = currentVersionStr.replace("v", "");

        String[] newParts = newVersion.split("-")[0].split("\\.");
        String[] currentParts = currentVersion.split("-")[0].split("\\.");

        int length = Math.max(newParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            try {
                int newPart = i < newParts.length ? Integer.parseInt(newParts[i]) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                if (newPart > currentPart) {
                    return true;
                }
                if (newPart < currentPart) {
                    return false;
                }
            } catch (NumberFormatException e) {
                if (newParts[i].compareTo(currentParts[i]) > 0) return true;
                if (newParts[i].compareTo(currentParts[i]) < 0) return false;
            }
        }
        return false;
    }
}
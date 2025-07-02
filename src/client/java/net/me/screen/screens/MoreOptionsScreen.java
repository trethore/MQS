package net.me.screen.screens;

import net.me.Main;
import net.me.screen.MQSScreen;
import net.me.screen.component.components.DarkButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public class MoreOptionsScreen extends MQSScreen {

    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 5;

    private static Boolean isVSCodeInstalledCache = null;

    public MoreOptionsScreen(MQSScreen parent) {
        super("More Options", 200, 150, parent);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = getMiddlePoint().x();
        int startY = getMiddlePoint().y() - 35;

        addDrawableChild(DarkButtonWidget.builder("Open Configs Folder", button -> openFolder("configs")).dimensions(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        startY += BUTTON_HEIGHT + BUTTON_SPACING;
        addDrawableChild(DarkButtonWidget.builder("Open Scripts Folder", button -> openFolder("scripts")).dimensions(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        startY += BUTTON_HEIGHT + BUTTON_SPACING;
        addDrawableChild(DarkButtonWidget.builder("Open in VS Code", button -> {
            Main.LOGGER.info("Opening in VS Code");
            if (isVSCodeInstalled()) {
                openScriptsInVSCodeDesktop();
            } else {
                openScriptsInVSCodeWeb();
            }
        }).dimensions(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        startY += BUTTON_HEIGHT + BUTTON_SPACING;
        addDrawableChild(DarkButtonWidget.builder("Keybinds", button -> {
            System.out.println("Keybinds screen here");
            // Later: new KeybindsScreen(this).open();
        }).dimensions(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());


    }

    private static synchronized boolean isVSCodeInstalled() {
        if (isVSCodeInstalledCache != null) {
            return isVSCodeInstalledCache;
        }

        try {
            String command = System.getProperty("os.name").toLowerCase().contains("win") ? "code.cmd" : "code";
            ProcessBuilder pb = new ProcessBuilder(command, "--version");
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);

            Process process = pb.start();
            int exitCode = process.waitFor();

            isVSCodeInstalledCache = (exitCode == 0);
        } catch (IOException | InterruptedException e) {
            isVSCodeInstalledCache = false;
        }

        if(isVSCodeInstalledCache){
            Main.LOGGER.info("VS Code command-line tool detected.");
        } else {
            Main.LOGGER.info("VS Code command-line tool not found. Will fall back to web version.");
        }

        return isVSCodeInstalledCache;
    }

    private void openScriptsInVSCodeDesktop() {
        MinecraftClient mc = this.client;
        try {
            Path scriptsDir = Main.MOD_DIR.resolve("scripts");
            if (!java.nio.file.Files.exists(scriptsDir)) {
                java.nio.file.Files.createDirectories(scriptsDir);
            }

            String formattedPath = "/" + scriptsDir.toAbsolutePath().toString().replace('\\', '/');

            URI uri = new URI("vscode", "file", formattedPath, null);

            Util.getOperatingSystem().open(uri);

            if (mc != null && mc.player != null) {
                mc.player.sendMessage(Text.literal("Attempting to open scripts folder in VS Code (Desktop)...").formatted(Formatting.GREEN), false);
            }
        } catch (Exception e) {
            Main.LOGGER.error("Could not open scripts folder in VS Code Desktop", e);
            if (mc != null && mc.player != null) {
                mc.player.sendMessage(Text.literal("Failed to open VS Code (Desktop).").formatted(Formatting.RED), false);
                mc.player.sendMessage(Text.literal("Make sure it's installed and the 'code' command is in your PATH.").formatted(Formatting.YELLOW), false);
            }
        }
    }

    private void openScriptsInVSCodeWeb() {
        try {
            Path scriptsDir = Main.MOD_DIR.resolve("scripts");
            if (!java.nio.file.Files.exists(scriptsDir)) {
                java.nio.file.Files.createDirectories(scriptsDir);
            }

            String scriptsPath = scriptsDir.toAbsolutePath().toString();

            assert this.client != null;
            this.client.keyboard.setClipboard(scriptsPath);

            Util.getOperatingSystem().open("https://vscode.dev/");

            if (this.client.player != null) {
                this.client.player.sendMessage(Text.literal("VS Code (Desktop) not found.").formatted(Formatting.YELLOW), false);
                this.client.player.sendMessage(Text.literal("Copied scripts folder path to clipboard.").formatted(Formatting.GREEN), false);
                this.client.player.sendMessage(Text.literal("In your browser, click 'Open Folder' and paste the path.").formatted(Formatting.AQUA), false);
            }

        } catch (Exception e) {
            Main.LOGGER.error("Could not open scripts folder in VS Code Web", e);
            if (this.client != null && this.client.player != null) {
                this.client.player.sendMessage(Text.literal("Failed to open VS Code Web.").formatted(Formatting.RED), false);
            }
        }
    }

    private void openFolder(String folder) {
        try {
            Path configsDir = Main.MOD_DIR.resolve(folder);
            if (!java.nio.file.Files.exists(configsDir)) {
                java.nio.file.Files.createDirectories(configsDir);
            }
            Util.getOperatingSystem().open(configsDir.toFile());
        } catch (Exception e) {
            Main.LOGGER.error("Could not open configs folder", e);
        }
    }
}
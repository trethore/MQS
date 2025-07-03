package net.me.screen.screens;

import net.me.Main;
import net.me.screen.MQSScreen;
import net.me.screen.component.WidgetLayoutHelper;
import net.me.screen.component.components.MQSButtonWidget;
import net.me.utils.ChatUtils;
import net.minecraft.client.MinecraftClient;
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

        if (isVSCodeInstalledCache) {
            Main.LOGGER.info("VS Code command-line tool detected.");
        } else {
            Main.LOGGER.info("VS Code command-line tool not found. Will fall back to web version.");
        }

        return isVSCodeInstalledCache;
    }

    @Override
    protected void init() {
        super.init();
        MQSButtonWidget openConfigsButton = MQSButtonWidget.builder("Open Configs Folder", button -> openFolder("configs"))
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();

        MQSButtonWidget openScriptsButton = MQSButtonWidget.builder("Open Scripts Folder", button -> openFolder("scripts"))
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();

        MQSButtonWidget openVSCodeButton = MQSButtonWidget.builder("Open in VS Code", button -> {
            Main.LOGGER.info("Opening in VS Code");
            if (isVSCodeInstalled()) {
                openScriptsInVSCodeDesktop();
            } else {
                openScriptsInVSCodeWeb();
            }
        }).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();

        MQSButtonWidget keybindsButton = MQSButtonWidget.builder("Keybinds", button -> new KeybindsScreen(this).open())
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();

        this.addDrawableChild(openConfigsButton);
        this.addDrawableChild(openScriptsButton);
        this.addDrawableChild(openVSCodeButton);
        this.addDrawableChild(keybindsButton);

        WidgetLayoutHelper.layoutVertically(
                getMiddlePoint().x() - BUTTON_WIDTH / 2,
                getMiddlePoint().y() - 35,
                BUTTON_SPACING,
                openConfigsButton,
                openScriptsButton,
                openVSCodeButton,
                keybindsButton
        );
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
                ChatUtils.addSuccessChatMessage("Attempting to open scripts folder in VS Code (Desktop)...", true);
            }
        } catch (Exception e) {
            Main.LOGGER.error("Could not open scripts folder in VS Code Desktop", e);
            if (mc != null && mc.player != null) {
                ChatUtils.addErrorChatMessage("Failed to open VS Code (Desktop).", true);
                ChatUtils.addWarnChatMessage("Make sure it's installed and the 'code' command is in your PATH.", true);
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
                ChatUtils.addWarnChatMessage("VS Code (Desktop) not found.", true);
                ChatUtils.addSuccessChatMessage("Copied scripts folder path to clipboard.", true);
                ChatUtils.addInfoChatMessage("In your browser, click 'Open Folder' and paste the path.", true);
            }

        } catch (Exception e) {
            Main.LOGGER.error("Could not open scripts folder in VS Code Web", e);
            if (this.client != null && this.client.player != null) {
                ChatUtils.addErrorChatMessage("Failed to open VS Code Web.", true);
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
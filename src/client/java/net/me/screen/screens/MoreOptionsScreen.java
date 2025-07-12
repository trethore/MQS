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

package net.me.screen.screens;

import net.me.Main;
import net.me.screen.MQSScreen;
import net.me.screen.component.WidgetLayoutHelper;
import net.me.screen.component.components.MQSImageButtonWidget;
import net.me.screen.component.components.MQSToast;
import net.me.utils.AssetIdentifiers;
import net.me.utils.ChatUtils;
import net.me.utils.UIConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public class MoreOptionsScreen extends MQSScreen {

    private static Boolean isVSCodeInstalledCache = null;

    public MoreOptionsScreen(MQSScreen parent) {
        super("More Options", 210, 230, parent);
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

        MQSImageButtonWidget settingsButton = MQSImageButtonWidget.mqsBuilder(AssetIdentifiers.ICON_SETTINGS, Text.literal("Settings"), button -> new SettingsScreen(this, Main.getInstance().getGlobalConfigManager()).open())
                .size(UIConstants.BUTTON_WIDTH_LARGE, UIConstants.BUTTON_HEIGHT)
                .build();

        MQSImageButtonWidget keybindsButton = MQSImageButtonWidget.mqsBuilder(AssetIdentifiers.ICON_KEYBOARD, Text.literal("Keybinds"), button -> new KeybindsScreen(this, Main.getInstance().getKeybindManager()).open())
                .size(UIConstants.BUTTON_WIDTH_LARGE, UIConstants.BUTTON_HEIGHT)
                .build();

        MQSImageButtonWidget openVSCodeButton = MQSImageButtonWidget.mqsBuilder(AssetIdentifiers.ICON_VSCODE, Text.literal("Open in VS Code"), button -> {
            Main.LOGGER.info("Opening in VS Code");
            if (isVSCodeInstalled()) {
                openScriptsInVSCodeDesktop();
            } else {
                openScriptsInVSCodeWeb();
            }
            MQSToast.show("VS Code", "Opening in VS Code", 2000, MQSToast.Corner.TOP_LEFT);
        }).size(UIConstants.BUTTON_WIDTH_LARGE, UIConstants.BUTTON_HEIGHT).build();

        MQSImageButtonWidget createScriptButton = MQSImageButtonWidget.mqsBuilder(AssetIdentifiers.ICON_CREATE_SCRIPT, Text.literal("Create Script"), button -> new CreateScriptScreen(this).open())
                .size(UIConstants.BUTTON_WIDTH_LARGE, UIConstants.BUTTON_HEIGHT)
                .build();

        MQSImageButtonWidget openModFolderButton = MQSImageButtonWidget.mqsBuilder(AssetIdentifiers.ICON_OPEN_FOLDER, Text.literal("Open Mod Folder"), button -> openFolder())
                .size(UIConstants.BUTTON_WIDTH_LARGE, UIConstants.BUTTON_HEIGHT)
                .build();

        MQSImageButtonWidget openWikiButton = MQSImageButtonWidget.mqsBuilder(AssetIdentifiers.ICON_WIKI, Text.literal("Open Wiki"), button -> openWiki())
                .size(UIConstants.BUTTON_WIDTH_LARGE, UIConstants.BUTTON_HEIGHT)
                .build();

        this.addDrawableChild(settingsButton);
        this.addDrawableChild(keybindsButton);
        this.addDrawableChild(openVSCodeButton);
        this.addDrawableChild(createScriptButton);
        this.addDrawableChild(openModFolderButton);
        this.addDrawableChild(openWikiButton);

        WidgetLayoutHelper.layoutVertically(
                getMiddlePoint().x() - UIConstants.BUTTON_WIDTH_LARGE / 2,
                getMiddlePoint().y() - 65,
                UIConstants.WIDGET_SPACING,
                settingsButton,
                keybindsButton,
                openVSCodeButton,
                createScriptButton,
                openModFolderButton,
                openWikiButton
        );
    }

    private void openWiki() {
        try {
            Util.getOperatingSystem().open(AssetIdentifiers.URL_WIKI);
            MQSToast.show("Wiki", "Opening in your browser", 2000, MQSToast.Corner.TOP_LEFT);
        } catch (Exception e) {
            Main.LOGGER.error("Could not open the wiki URL", e);
            if (this.client != null && this.client.player != null) {
                ChatUtils.addErrorChatMessage("Failed to open the wiki in your browser.", true);
            }
        }
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

    private void openFolder() {
        try {
            Path configsDir = Main.MOD_DIR;
            if (!java.nio.file.Files.exists(configsDir)) {
                java.nio.file.Files.createDirectories(configsDir);
            }
            Util.getOperatingSystem().open(configsDir.toFile());
        } catch (Exception e) {
            Main.LOGGER.error("Could not open configs folder", e);
        }
    }
}
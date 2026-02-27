/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Réthoré
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

package net.me.ui;

import net.fabricmc.loader.api.FabricLoader;
import net.me.Main;
import net.me.console.ConsoleManager;
import net.me.config.GlobalConfigManager;
import net.me.keybinds.KeybindManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.ScriptingService;
import net.me.utils.McUtils;
import net.minecraft.client.Minecraft;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.runtime.GrapheneHttpServer;
import tytoo.grapheneui.api.url.GrapheneAppUrls;

import java.util.Objects;

public class UiManager {
    private static final String DEV_UI_ENTRYPOINT = "/scripts/index.html";
    private static final String DEFAULT_PROD_UI_URL = GrapheneAppUrls.asset(Main.MOD_ID, "pages/scripts/index.html");
    private final ScriptingService scriptingService;
    private final ConsoleManager consoleManager;
    private final KeybindManager keybindManager;
    private final ScriptManager scriptManager;
    private final GlobalConfigManager globalConfigManager;

    public UiManager(
            ScriptingService scriptingService,
            ConsoleManager consoleManager,
            KeybindManager keybindManager,
            ScriptManager scriptManager,
            GlobalConfigManager globalConfigManager
    ) {
        this.scriptingService = Objects.requireNonNull(scriptingService, "scriptingService");
        this.consoleManager = Objects.requireNonNull(consoleManager, "consoleManager");
        this.keybindManager = Objects.requireNonNull(keybindManager, "keybindManager");
        this.scriptManager = Objects.requireNonNull(scriptManager, "scriptManager");
        this.globalConfigManager = Objects.requireNonNull(globalConfigManager, "globalConfigManager");
    }

    public void openUi() {
        Minecraft mc = McUtils.getMc();
        String targetUrl;

        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            targetUrl = resolveDevelopmentUiUrl();
        } else {
            targetUrl = DEFAULT_PROD_UI_URL;
        }

        mc.execute(() -> mc.setScreen(new MQSWebScreen(
                targetUrl,
                this.scriptingService,
                this.consoleManager,
                this.keybindManager,
                this.scriptManager,
                this.globalConfigManager
        )));
    }

    /**
     * In development, we want to load the UI from the Graphene HTTP server, which serves the files directly from the filesystem.
     * This allows for hot-reloading and faster development.
     * Fall back to the bundled UI if the HTTP server is not running for some reason (e.g. port already in use).
     */
    private String resolveDevelopmentUiUrl() {
        GrapheneHttpServer httpServer = GrapheneCore.runtime().httpServer();
        if (!httpServer.isRunning()) {
            Main.LOGGER.warn("Graphene HTTP server is not running in dev, falling back to bundled UI.");
            return DEFAULT_PROD_UI_URL;
        }

        return httpServer.baseUrl() + DEV_UI_ENTRYPOINT + "?v=" + System.nanoTime();
    }
}

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
import net.me.utils.McUtils;
import net.minecraft.client.Minecraft;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.runtime.GrapheneHttpServer;
import tytoo.grapheneui.api.url.GrapheneAppUrls;

public class UiManager {
    private static final String DEV_UI_ENTRYPOINT = "/index.html";
    private static final String DEFAULT_PROD_UI_URL = GrapheneAppUrls.asset(Main.MOD_ID, "pages/index.html");

    public void openUi() {
        Minecraft mc = McUtils.getMc();
        String targetUrl;

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            targetUrl = resolveDevelopmentUiUrl();
        } else {
            targetUrl = DEFAULT_PROD_UI_URL;
        }

        String finalTargetUrl = targetUrl;
        mc.execute(() -> mc.setScreen(new MQSWebScreen(finalTargetUrl)));
    }

    private String resolveDevelopmentUiUrl() {
        GrapheneHttpServer httpServer = GrapheneCore.runtime().httpServer();
        if (!httpServer.isRunning()) {
            Main.LOGGER.warn("Graphene HTTP server is not running in dev, falling back to bundled UI.");
            return DEFAULT_PROD_UI_URL;
        }

        return httpServer.baseUrl() + DEV_UI_ENTRYPOINT + "?v=" + System.nanoTime();
    }
}

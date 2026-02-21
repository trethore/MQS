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
import net.minecraft.client.Minecraft;
import tytoo.grapheneui.api.url.GrapheneAppUrls;

public class UiManager {
    private static final String DEFAULT_DEV_UI_PATH = GrapheneAppUrls.asset(Main.MOD_ID,"pages/index.html");
    private static final String DEFAULT_PROD_UI_URL = GrapheneAppUrls.asset(Main.MOD_ID, "pages/index.html");

    public void openUi() {
        Minecraft minecraft = Minecraft.getInstance();

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            minecraft.execute(() -> minecraft.setScreen(new MQSWebScreen(DEFAULT_DEV_UI_PATH)));
        } else {
            minecraft.execute(() -> minecraft.setScreen(new MQSWebScreen(DEFAULT_PROD_UI_URL)));
        }
    }
}

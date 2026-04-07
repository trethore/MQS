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
import net.me.config.GlobalConfigManager;
import net.me.console.ConsoleManager;
import net.me.keybinds.KeybindManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.ScriptingService;
import net.me.utils.IdeCommandUtils;
import net.me.utils.McUtils;
import net.me.utils.VscodeWebUtils;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.util.Objects;

public class UiManager {
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

    private void openUrl(String targetUrl) {
        Minecraft mc = McUtils.getMc();
        mc.execute(() -> mc.setScreen(new MQSWebScreen(
                targetUrl,
                this.scriptingService,
                this.consoleManager,
                this.keybindManager,
                this.scriptManager,
                this.globalConfigManager
        )));
    }

    public void openUi() {
        String targetUrl;

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            targetUrl = resolveDevelopmentUiUrl();
        } else {
            targetUrl = resolveProductionUiUrl();
        }

        openUrl(targetUrl);
    }

    public void openVscodeWeb() {
        openUrl(VscodeWebUtils.CODE_EDITOR_URL);
    }

    public void openIde() throws IOException {
        IdeCommandUtils.openPathInIde(
                this.globalConfigManager.getDefaultIdeCommand(),
                this.globalConfigManager.getDefaultProjectPath()
        );
    }

    /**
     * In development, we load the UI from the Graphene HTTP server backed by the web/out build output.
     * This allows faster UI iteration while keeping the same Graphene/JCEF loading path.
     */
    private String resolveDevelopmentUiUrl() {
        return Main.getGraphene().httpUrl(UiPaths.DEV_HTTP_ENTRYPOINT + "?v=" + System.nanoTime());
    }

    private String resolveProductionUiUrl() {
        return Main.getGraphene().appAssets().asset(Main.MOD_ID, UiPaths.PROD_APP_ENTRYPOINT);
    }
}

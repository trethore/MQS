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

import net.me.console.ConsoleManager;
import net.me.config.GlobalConfigManager;
import net.me.keybinds.KeybindManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.ScriptingService;
import net.me.ui.bridges.MQSCommandsBridge;
import net.me.ui.bridges.MQSConsoleBridge;
import net.me.ui.bridges.MQSKeybindsBridge;
import net.me.ui.bridges.MQSOptionsBridge;
import net.me.ui.bridges.MQSScriptsBridge;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tytoo.grapheneui.api.widget.GrapheneWebViewWidget;

import java.util.Objects;

public class MQSWebScreen extends Screen {
    private static final Component TITLE = Component.literal("MQS UI");
    private static final float FULLSCREEN_RATIO = 0.9F;
    private final String initialUrl;
    private final MQSScriptsBridge scriptsBridge;
    private final MQSConsoleBridge consoleBridge;
    private final MQSKeybindsBridge keybindsBridge;
    private final MQSCommandsBridge commandsBridge;
    private final MQSOptionsBridge optionsBridge;
    private GrapheneWebViewWidget webViewWidget;

    public MQSWebScreen(
            String initialUrl,
            ScriptingService scriptingService,
            ConsoleManager consoleManager,
            KeybindManager keybindManager,
            ScriptManager scriptManager,
            GlobalConfigManager globalConfigManager
    ) {
        super(TITLE);
        this.initialUrl = initialUrl;
        this.scriptsBridge = new MQSScriptsBridge(Objects.requireNonNull(scriptingService, "scriptingService"));
        this.consoleBridge = new MQSConsoleBridge(Objects.requireNonNull(consoleManager, "consoleManager"));
        this.keybindsBridge = new MQSKeybindsBridge(Objects.requireNonNull(keybindManager, "keybindManager"));
        this.commandsBridge = new MQSCommandsBridge(Objects.requireNonNull(scriptManager, "scriptManager"));
        this.optionsBridge = new MQSOptionsBridge(Objects.requireNonNull(globalConfigManager, "globalConfigManager"));
    }

    @Override
    protected void init() {
        closeWebView();

        int webViewWidth = Math.max(1, Math.round(this.width * FULLSCREEN_RATIO));
        int webViewHeight = Math.max(1, Math.round(this.height * FULLSCREEN_RATIO));
        int webViewX = (this.width - webViewWidth) / 2;
        int webViewY = (this.height - webViewHeight) / 2;

        this.webViewWidget = this.addRenderableWidget(
                new GrapheneWebViewWidget(this, webViewX, webViewY, webViewWidth, webViewHeight, Component.empty(), this.initialUrl)
        );

        this.scriptsBridge.attach(this.webViewWidget.bridge());
        this.consoleBridge.attach(this.webViewWidget.bridge());
        this.keybindsBridge.attach(this.webViewWidget.bridge());
        this.commandsBridge.attach(this.webViewWidget.bridge());
        this.optionsBridge.attach(this.webViewWidget.bridge());
    }

    @Override
    public void onClose() {
        closeWebView();
        super.onClose();
    }

    @Override
    public void removed() {
        closeWebView();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void closeWebView() {
        this.scriptsBridge.close();
        this.consoleBridge.close();
        this.keybindsBridge.close();
        this.commandsBridge.close();
        this.optionsBridge.close();

        if (this.webViewWidget == null) {
            return;
        }

        this.webViewWidget.close();
        this.webViewWidget = null;
    }
}

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

package net.me.ui.bridges;

import net.me.Main;
import net.me.ui.bridges.utils.BridgeRequests;
import net.me.ui.bridges.utils.BridgeSubscriptions;
import net.me.utils.McUtils;
import net.minecraft.client.Minecraft;
import tytoo.grapheneui.api.bridge.GrapheneBridge;

import java.util.Objects;

public final class MQSCodeBridge implements AutoCloseable {
    public static final String CHANNEL_PREPARE = "mqs:code:prepare";
    private static final String BRIDGE_NAME = "MQS code";

    private final BridgeSubscriptions subscriptions;

    public MQSCodeBridge() {
        this.subscriptions = new BridgeSubscriptions(BRIDGE_NAME);
    }

    @Override
    public void close() {
        this.subscriptions.close();
    }

    public void attach(GrapheneBridge bridge) {
        close();
        GrapheneBridge validatedBridge = Objects.requireNonNull(bridge, "bridge");

        this.subscriptions.add(BridgeRequests.onRequestJson(validatedBridge, CHANNEL_PREPARE, this::handlePrepare));
    }

    private CodePrepareResponse handlePrepare() {
        String modDirPath = Main.MOD_DIR.toAbsolutePath().normalize().toString();
        boolean copied = false;
        Minecraft minecraft = McUtils.getMc();

        try {
            minecraft.execute(() -> minecraft.keyboardHandler.setClipboard(modDirPath));
            copied = true;
        } catch (RuntimeException exception) {
            Main.LOGGER.error("Failed to copy MQS mod directory to clipboard from UI bridge.", exception);
        }

        return new CodePrepareResponse(copied, modDirPath);
    }

    public record CodePrepareResponse(boolean copied, String modDirPath) {
    }
}

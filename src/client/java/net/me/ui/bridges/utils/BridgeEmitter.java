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

package net.me.ui.bridges.utils;

import net.me.Main;
import net.me.utils.McUtils;
import tytoo.grapheneui.api.bridge.GrapheneBridge;

import java.util.Objects;
import java.util.function.Supplier;

public final class BridgeEmitter {
    private BridgeEmitter() {
    }

    public static void emitJson(GrapheneBridge bridge, String bridgeName, String channel, Object payload) {
        if (bridge == null) {
            return;
        }

        try {
            bridge.emitJson(channel, payload);
        } catch (RuntimeException exception) {
            Main.LOGGER.debug("Failed to emit {} bridge event on channel '{}'.", bridgeName, channel, exception);
        }
    }

    public static void emitJsonOnClientThread(
            Supplier<GrapheneBridge> bridgeSupplier,
            String bridgeName,
            String channel,
            Object payload
    ) {
        Objects.requireNonNull(bridgeSupplier, "bridgeSupplier");

        McUtils.getMc().execute(() -> emitJson(bridgeSupplier.get(), bridgeName, channel, payload));
    }

    public static void emitJsonOnClientThread(
            Supplier<GrapheneBridge> bridgeSupplier,
            String bridgeName,
            String channel,
            Supplier<Object> payloadSupplier
    ) {
        Objects.requireNonNull(bridgeSupplier, "bridgeSupplier");
        Objects.requireNonNull(payloadSupplier, "payloadSupplier");

        McUtils.getMc().execute(() -> {
            Object payload;
            try {
                payload = payloadSupplier.get();
            } catch (RuntimeException exception) {
                Main.LOGGER.debug("Failed to build {} bridge payload for channel '{}'.", bridgeName, channel, exception);
                return;
            }

            emitJson(bridgeSupplier.get(), bridgeName, channel, payload);
        });
    }
}

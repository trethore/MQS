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
import tytoo.grapheneui.api.bridge.GrapheneBridgeSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BridgeSubscriptions implements AutoCloseable {
    private final String bridgeName;
    private final List<GrapheneBridgeSubscription> subscriptions;

    public BridgeSubscriptions(String bridgeName) {
        this.bridgeName = Objects.requireNonNull(bridgeName, "bridgeName");
        this.subscriptions = new ArrayList<>();
    }

    public void add(GrapheneBridgeSubscription subscription) {
        this.subscriptions.add(Objects.requireNonNull(subscription, "subscription"));
    }

    @Override
    public void close() {
        for (GrapheneBridgeSubscription subscription : this.subscriptions) {
            try {
                subscription.unsubscribe();
            } catch (RuntimeException exception) {
                Main.LOGGER.debug("Failed to unsubscribe {} bridge handler.", this.bridgeName, exception);
            }
        }

        this.subscriptions.clear();
    }
}

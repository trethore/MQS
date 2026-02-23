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
import net.me.config.GlobalConfigManager;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.bridge.GrapheneBridgeSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class MQSOptionsBridge implements AutoCloseable {
    private static final String ACTION_UPDATE = "update";

    public static final String CHANNEL_GET = "mqs:options:get";
    public static final String CHANNEL_SET = "mqs:options:set";
    public static final String EVENT_UPDATED = "mqs:options:updated";

    private final GlobalConfigManager globalConfigManager;
    private final List<GrapheneBridgeSubscription> subscriptions;
    private GrapheneBridge bridge;

    public MQSOptionsBridge(GlobalConfigManager globalConfigManager) {
        this.globalConfigManager = Objects.requireNonNull(globalConfigManager, "globalConfigManager");
        this.subscriptions = new ArrayList<>();
    }

    public void attach(GrapheneBridge bridge) {
        close();
        this.bridge = Objects.requireNonNull(bridge, "bridge");

        this.subscriptions.add(this.bridge.onReady(this::emitUpdated));
        this.subscriptions.add(this.bridge.onRequestJson(
                CHANNEL_GET,
                Object.class,
                (ignoredChannel, ignoredPayload) -> CompletableFuture.completedFuture(buildSnapshot())
        ));
        this.subscriptions.add(this.bridge.onRequestJson(
                CHANNEL_SET,
                OptionsUpdateRequest.class,
                (ignoredChannel, payload) -> CompletableFuture.completedFuture(handleSet(payload))
        ));
    }

    @Override
    public void close() {
        for (GrapheneBridgeSubscription subscription : this.subscriptions) {
            try {
                subscription.unsubscribe();
            } catch (RuntimeException exception) {
                Main.LOGGER.debug("Failed to unsubscribe MQS options bridge handler.", exception);
            }
        }

        this.subscriptions.clear();
        this.bridge = null;
    }

    private OptionsUpdateResponse handleSet(OptionsUpdateRequest payload) {
        if (payload == null) {
            return OptionsUpdateResponse.failure(ACTION_UPDATE, "payload is required", buildSnapshot());
        }

        if (payload.logRedirect() != null) {
            this.globalConfigManager.setLogRedirectEnabled(payload.logRedirect());
        }
        if (payload.allowAllClasses() != null) {
            this.globalConfigManager.setAllClassesAllowed(payload.allowAllClasses());
        }
        if (payload.defaultIdeCommand() != null) {
            this.globalConfigManager.setDefaultIdeCommand(payload.defaultIdeCommand());
        }

        OptionsSnapshotResponse snapshot = buildSnapshot();
        emitUpdated(snapshot);
        return OptionsUpdateResponse.success(ACTION_UPDATE, "options updated", snapshot);
    }

    private OptionsSnapshotResponse buildSnapshot() {
        return new OptionsSnapshotResponse(
                this.globalConfigManager.isLogRedirectEnabled(),
                this.globalConfigManager.areAllClassesAllowed(),
                this.globalConfigManager.getDefaultIdeCommand(),
                this.globalConfigManager.getAdditionalScriptDirectories()
        );
    }

    private void emitUpdated() {
        emitUpdated(buildSnapshot());
    }

    private void emitUpdated(OptionsSnapshotResponse snapshot) {
        GrapheneBridge activeBridge = this.bridge;
        if (activeBridge == null) {
            return;
        }

        try {
            activeBridge.emitJson(EVENT_UPDATED, snapshot);
        } catch (RuntimeException exception) {
            Main.LOGGER.debug("Failed to emit options update bridge event.", exception);
        }
    }

    public record OptionsUpdateRequest(Boolean logRedirect, Boolean allowAllClasses, String defaultIdeCommand) {
    }

    public record OptionsSnapshotResponse(
            boolean logRedirect,
            boolean allowAllClasses,
            String defaultIdeCommand,
            List<String> additionalScriptDirectories
    ) {
    }

    public record OptionsUpdateResponse(boolean success, String action, String message, OptionsSnapshotResponse options) {
        public static OptionsUpdateResponse success(String action, String message, OptionsSnapshotResponse options) {
            return new OptionsUpdateResponse(true, action, message, options);
        }

        public static OptionsUpdateResponse failure(String action, String message, OptionsSnapshotResponse options) {
            return new OptionsUpdateResponse(false, action, message, options);
        }
    }
}

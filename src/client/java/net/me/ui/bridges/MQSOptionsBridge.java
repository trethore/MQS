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
import net.me.ui.bridges.utils.BridgeEmitter;
import net.me.ui.bridges.utils.BridgeRequests;
import net.me.ui.bridges.utils.BridgeSubscriptions;
import net.me.utils.IdeCommandUtils;
import tytoo.grapheneui.api.bridge.GrapheneBridge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class MQSOptionsBridge implements AutoCloseable {
    public static final String CHANNEL_GET = "mqs:options:get";
    public static final String CHANNEL_SET = "mqs:options:set";
    public static final String CHANNEL_OPEN_PATH = "mqs:options:open-path";
    public static final String EVENT_UPDATED = "mqs:options:updated";
    private static final String ACTION_UPDATE = "update";
    private static final String ACTION_OPEN_PATH = "openPath";
    private static final String OPEN_TARGET_PICKER = "picker";
    private static final String BRIDGE_NAME = "MQS options";
    private final GlobalConfigManager globalConfigManager;
    private final BridgeSubscriptions subscriptions;
    private GrapheneBridge bridge;

    public MQSOptionsBridge(GlobalConfigManager globalConfigManager) {
        this.globalConfigManager = Objects.requireNonNull(globalConfigManager, "globalConfigManager");
        this.subscriptions = new BridgeSubscriptions(BRIDGE_NAME);
    }

    public void attach(GrapheneBridge bridge) {
        close();
        this.bridge = Objects.requireNonNull(bridge, "bridge");

        this.subscriptions.add(this.bridge.onReady(this::emitUpdated));
        this.subscriptions.add(BridgeRequests.onRequestJson(this.bridge, CHANNEL_GET, this::buildSnapshot));
        this.subscriptions.add(BridgeRequests.onRequestJson(this.bridge, CHANNEL_SET, OptionsUpdateRequest.class, this::handleSet));
        this.subscriptions.add(BridgeRequests.onRequestJson(this.bridge, CHANNEL_OPEN_PATH, OptionsOpenPathRequest.class, this::handleOpenPath));
    }

    @Override
    public void close() {
        this.subscriptions.close();
        this.bridge = null;
    }

    private OptionsUpdateResponse handleSet(OptionsUpdateRequest payload) {
        if (payload == null) {
            return OptionsUpdateResponse.failure(ACTION_UPDATE, "payload is required", buildSnapshot());
        }

        try {
            if (payload.logRedirect() != null) {
                this.globalConfigManager.setLogRedirectEnabled(payload.logRedirect());
            }
            if (payload.allowAllClasses() != null) {
                this.globalConfigManager.setAllClassesAllowed(payload.allowAllClasses());
            }
            if (payload.defaultIdeCommand() != null) {
                this.globalConfigManager.setDefaultIdeCommand(payload.defaultIdeCommand());
            }
            if (payload.defaultProjectPath() != null) {
                this.globalConfigManager.setDefaultProjectPath(payload.defaultProjectPath());
            }
            if (payload.additionalScriptDirectories() != null) {
                this.globalConfigManager.setAdditionalScriptDirectories(payload.additionalScriptDirectories());
            }
        } catch (RuntimeException exception) {
            Main.LOGGER.error("Failed to update options from UI bridge.", exception);
            return OptionsUpdateResponse.failure(ACTION_UPDATE, exception.getMessage(), buildSnapshot());
        }

        OptionsSnapshotResponse snapshot = buildSnapshot();
        emitUpdated(snapshot);
        return OptionsUpdateResponse.success(ACTION_UPDATE, "options updated", snapshot);
    }

    private OptionsOpenPathResponse handleOpenPath(OptionsOpenPathRequest payload) {
        if (payload == null) {
            return OptionsOpenPathResponse.failure(ACTION_OPEN_PATH, "payload is required", null);
        }

        String ideCommand = payload.defaultIdeCommand() != null
                ? payload.defaultIdeCommand()
                : this.globalConfigManager.getDefaultIdeCommand();

        try {
            Path openedPath = openRequestedPath(payload, ideCommand);
            if (openedPath == null) {
                return OptionsOpenPathResponse.success(ACTION_OPEN_PATH, "path selection cancelled", "");
            }

            return OptionsOpenPathResponse.success(ACTION_OPEN_PATH, "path opened", openedPath);
        } catch (IllegalArgumentException | IOException exception) {
            Main.LOGGER.error("Failed to open path from options UI bridge.", exception);
            return OptionsOpenPathResponse.failure(ACTION_OPEN_PATH, exception.getMessage(), null);
        }
    }

    private Path openRequestedPath(OptionsOpenPathRequest payload, String ideCommand) throws IOException {
        if (OPEN_TARGET_PICKER.equalsIgnoreCase(payload.target())) {
            return IdeCommandUtils.pickDirectory(payload.path());
        }

        return IdeCommandUtils.openPathInIde(ideCommand, payload.path());
    }

    private OptionsSnapshotResponse buildSnapshot() {
        GlobalConfigManager.OptionsSnapshot optionsSnapshot = this.globalConfigManager.getOptionsSnapshot();
        return new OptionsSnapshotResponse(
                optionsSnapshot.logRedirect(),
                optionsSnapshot.allowAllClasses(),
                optionsSnapshot.defaultIdeCommand(),
                optionsSnapshot.defaultProjectPath(),
                optionsSnapshot.additionalScriptDirs(),
                IdeCommandUtils.getDefaultScriptsDirectory().toString()
        );
    }

    private void emitUpdated() {
        emitUpdated(buildSnapshot());
    }

    private void emitUpdated(OptionsSnapshotResponse snapshot) {
        BridgeEmitter.emitJsonOnClientThread(() -> this.bridge, BRIDGE_NAME, EVENT_UPDATED, snapshot);
    }

    public record OptionsUpdateRequest(
            Boolean logRedirect,
            Boolean allowAllClasses,
            String defaultIdeCommand,
            String defaultProjectPath,
            List<String> additionalScriptDirectories
    ) {
    }

    public record OptionsOpenPathRequest(String path, String defaultIdeCommand, String target) {
    }

    public record OptionsSnapshotResponse(
            boolean logRedirect,
            boolean allowAllClasses,
            String defaultIdeCommand,
            String defaultProjectPath,
            List<String> additionalScriptDirectories,
            String defaultScriptDirectory
    ) {
    }

    public record OptionsUpdateResponse(boolean success, String action, String message,
                                        OptionsSnapshotResponse options) {
        public static OptionsUpdateResponse success(String action, String message, OptionsSnapshotResponse options) {
            return new OptionsUpdateResponse(true, action, message, options);
        }

        public static OptionsUpdateResponse failure(String action, String message, OptionsSnapshotResponse options) {
            return new OptionsUpdateResponse(false, action, message, options);
        }
    }

    public record OptionsOpenPathResponse(boolean success, String action, String message, String openedPath) {
        public static OptionsOpenPathResponse success(String action, String message, String openedPath) {
            return new OptionsOpenPathResponse(true, action, message, openedPath);
        }

        public static OptionsOpenPathResponse success(String action, String message, Path openedPath) {
            return new OptionsOpenPathResponse(true, action, message, openedPath.toString());
        }

        public static OptionsOpenPathResponse failure(String action, String message, String openedPath) {
            return new OptionsOpenPathResponse(false, action, message, openedPath);
        }
    }
}

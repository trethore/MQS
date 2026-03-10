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
import net.me.scripting.ScriptingService;
import net.me.scripting.script.RunningScript;
import net.me.scripting.script.ScriptDescriptor;
import net.me.ui.bridges.utils.BridgeEmitter;
import net.me.ui.bridges.utils.BridgeRequests;
import net.me.ui.bridges.utils.BridgeSubscriptions;
import tytoo.grapheneui.api.bridge.GrapheneBridge;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class MQSScriptsBridge implements AutoCloseable {
    public static final String CHANNEL_LIST = "mqs:scripts:list";
    public static final String CHANNEL_INFO = "mqs:scripts:info";
    public static final String CHANNEL_TOGGLE = "mqs:scripts:toggle";
    public static final String CHANNEL_REFRESH = "mqs:scripts:refresh";
    public static final String CHANNEL_REFRESH_AND_REENABLE = "mqs:scripts:refresh-and-reenable";
    public static final String CHANNEL_DISABLE_ALL = "mqs:scripts:disable-all";
    public static final String EVENT_UPDATED = "mqs:scripts:updated";
    private static final String ACTION_INFO = "info";
    private static final String ACTION_TOGGLE = "toggle";
    private static final String ACTION_REFRESH = "refresh";
    private static final String ACTION_REFRESH_AND_REENABLE = "refresh-and-reenable";
    private static final String ACTION_DISABLE_ALL = "disable-all";
    private static final String MESSAGE_INVALID_SCRIPT_ID = "payload must include a non-empty scriptId";
    private static final String BRIDGE_NAME = "MQS scripts";
    private final ScriptingService scriptingService;
    private final BridgeSubscriptions subscriptions;
    private GrapheneBridge bridge;

    public MQSScriptsBridge(ScriptingService scriptingService) {
        this.scriptingService = Objects.requireNonNull(scriptingService, "scriptingService");
        this.subscriptions = new BridgeSubscriptions(BRIDGE_NAME);
    }

    public void attach(GrapheneBridge bridge) {
        close();
        this.bridge = Objects.requireNonNull(bridge, "bridge");

        this.subscriptions.add(this.bridge.onReady(this::emitScriptsUpdated));
        this.subscriptions.add(BridgeRequests.onRequestJson(this.bridge, CHANNEL_LIST, this::buildSnapshot));
        this.subscriptions.add(BridgeRequests.onRequestJson(this.bridge, CHANNEL_INFO, ScriptIdRequest.class, this::handleInfo));
        this.subscriptions.add(BridgeRequests.onRequestJson(this.bridge, CHANNEL_TOGGLE, ScriptIdRequest.class, this::handleToggle));
        this.subscriptions.add(BridgeRequests.onRequestJson(this.bridge, CHANNEL_REFRESH, this::handleRefresh));
        this.subscriptions.add(BridgeRequests.onRequestJson(
                this.bridge,
                CHANNEL_REFRESH_AND_REENABLE,
                this::handleRefreshAndReenable
        ));
        this.subscriptions.add(BridgeRequests.onRequestJson(this.bridge, CHANNEL_DISABLE_ALL, this::handleDisableAll));
    }

    @Override
    public void close() {
        this.subscriptions.close();
        this.bridge = null;
    }

    private ScriptOperationResponse handleInfo(ScriptIdRequest payload) {
        ScriptsSnapshotResponse snapshot = buildSnapshot();
        String scriptId = normalizeScriptId(payload);
        if (scriptId == null) {
            return ScriptOperationResponse.failure(ACTION_INFO, MESSAGE_INVALID_SCRIPT_ID, snapshot);
        }

        ScriptStateResponse script = findScript(snapshot, scriptId);
        if (script == null) {
            return ScriptOperationResponse.failure(ACTION_INFO, "script not found: " + scriptId, snapshot);
        }

        return ScriptOperationResponse.success(ACTION_INFO, "script info loaded", script, snapshot);
    }

    private ScriptOperationResponse handleToggle(ScriptIdRequest payload) {
        String scriptId = normalizeScriptId(payload);
        if (scriptId == null) {
            return ScriptOperationResponse.failure(ACTION_TOGGLE, MESSAGE_INVALID_SCRIPT_ID, buildSnapshot());
        }

        ScriptsSnapshotResponse beforeSnapshot = buildSnapshot();
        ScriptStateResponse beforeScript = findScript(beforeSnapshot, scriptId);
        if (beforeScript == null) {
            return ScriptOperationResponse.failure(ACTION_TOGGLE, "script not found: " + scriptId, beforeSnapshot);
        }

        boolean targetRunning = !beforeScript.running();
        try {
            if (targetRunning) {
                scriptingService.enable(scriptId);
            } else {
                scriptingService.disable(scriptId);
            }
        } catch (RuntimeException exception) {
            Main.LOGGER.error("Failed to toggle script '{}' from UI bridge.", scriptId, exception);
            return ScriptOperationResponse.failure(ACTION_TOGGLE, exception.getMessage(), buildSnapshot());
        }

        ScriptsSnapshotResponse afterSnapshot = buildSnapshot();
        ScriptStateResponse afterScript = findScript(afterSnapshot, scriptId);
        boolean success = afterScript != null && afterScript.running() == targetRunning;
        if (success) {
            emitScriptsUpdated(afterSnapshot);
            return ScriptOperationResponse.success(ACTION_TOGGLE, "script toggled", afterScript, afterSnapshot);
        }

        return ScriptOperationResponse.failure(ACTION_TOGGLE, "script state did not change", afterSnapshot);
    }

    private ScriptOperationResponse handleRefresh() {
        try {
            scriptingService.refresh();
        } catch (RuntimeException exception) {
            Main.LOGGER.error("Failed to refresh scripts from UI bridge.", exception);
            return ScriptOperationResponse.failure(ACTION_REFRESH, exception.getMessage(), buildSnapshot());
        }

        ScriptsSnapshotResponse snapshot = buildSnapshot();
        emitScriptsUpdated(snapshot);
        return ScriptOperationResponse.success(ACTION_REFRESH, "scripts refreshed", null, snapshot);
    }

    private ScriptOperationResponse handleRefreshAndReenable() {
        try {
            scriptingService.refreshAndReenable();
        } catch (RuntimeException exception) {
            Main.LOGGER.error("Failed to refresh and re-enable scripts from UI bridge.", exception);
            return ScriptOperationResponse.failure(ACTION_REFRESH_AND_REENABLE, exception.getMessage(), buildSnapshot());
        }

        ScriptsSnapshotResponse snapshot = buildSnapshot();
        emitScriptsUpdated(snapshot);
        return ScriptOperationResponse.success(ACTION_REFRESH_AND_REENABLE, "scripts refreshed and re-enabled", null, snapshot);
    }

    private ScriptOperationResponse handleDisableAll() {
        int disabledCount;
        try {
            disabledCount = scriptingService.disableAll();
        } catch (RuntimeException exception) {
            Main.LOGGER.error("Failed to disable all scripts from UI bridge.", exception);
            return ScriptOperationResponse.failure(ACTION_DISABLE_ALL, exception.getMessage(), buildSnapshot());
        }

        ScriptsSnapshotResponse snapshot = buildSnapshot();
        emitScriptsUpdated(snapshot);

        String pluralSuffix = disabledCount == 1 ? "" : "s";
        String message = disabledCount == 0
                ? "no running scripts to disable"
                : "disabled " + disabledCount + " running script" + pluralSuffix;

        return ScriptOperationResponse.success(ACTION_DISABLE_ALL, message, null, snapshot);
    }

    private ScriptsSnapshotResponse buildSnapshot() {
        Set<String> runningScriptIds = scriptingService.listRunning().stream()
                .map(RunningScript::getId)
                .collect(Collectors.toSet());

        List<ScriptStateResponse> scripts = scriptingService.listAvailable().stream()
                .map(descriptor -> toScriptState(descriptor, runningScriptIds))
                .sorted(
                        Comparator.comparing(ScriptStateResponse::scriptName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                                .thenComparing(ScriptStateResponse::id, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                )
                .toList();

        int runningCount = (int) scripts.stream()
                .filter(ScriptStateResponse::running)
                .count();

        return new ScriptsSnapshotResponse(scripts, runningCount, scripts.size());
    }

    private ScriptStateResponse toScriptState(ScriptDescriptor descriptor, Set<String> runningScriptIds) {
        return new ScriptStateResponse(
                descriptor.getId(),
                descriptor.scriptName(),
                descriptor.version(),
                descriptor.mainClass(),
                descriptor.path().toString(),
                runningScriptIds.contains(descriptor.getId())
        );
    }

    private ScriptStateResponse findScript(ScriptsSnapshotResponse snapshot, String scriptId) {
        return snapshot.scripts().stream()
                .filter(script -> script.id().equals(scriptId))
                .findFirst()
                .orElse(null);
    }

    private String normalizeScriptId(ScriptIdRequest payload) {
        if (payload == null || payload.scriptId() == null) {
            return null;
        }

        String scriptId = payload.scriptId().trim();
        if (scriptId.isEmpty()) {
            return null;
        }

        return scriptId;
    }

    private void emitScriptsUpdated() {
        emitScriptsUpdated(buildSnapshot());
    }

    private void emitScriptsUpdated(ScriptsSnapshotResponse snapshot) {
        BridgeEmitter.emitJsonOnClientThread(() -> this.bridge, BRIDGE_NAME, EVENT_UPDATED, snapshot);
    }

    public record ScriptIdRequest(String scriptId) {
    }

    public record ScriptsSnapshotResponse(List<ScriptStateResponse> scripts, int runningCount, int totalCount) {
    }

    public record ScriptStateResponse(
            String id,
            String scriptName,
            String version,
            String mainClass,
            String path,
            boolean running
    ) {
    }

    public record ScriptOperationResponse(
            boolean success,
            String action,
            String message,
            ScriptStateResponse script,
            ScriptsSnapshotResponse snapshot
    ) {
        public static ScriptOperationResponse success(
                String action,
                String message,
                ScriptStateResponse script,
                ScriptsSnapshotResponse snapshot
        ) {
            return new ScriptOperationResponse(true, action, message, script, snapshot);
        }

        public static ScriptOperationResponse failure(
                String action,
                String message,
                ScriptsSnapshotResponse snapshot
        ) {
            return new ScriptOperationResponse(false, action, message, null, snapshot);
        }
    }
}

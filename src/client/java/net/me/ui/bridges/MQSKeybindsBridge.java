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
import net.me.keybinds.HostKeyBinding;
import net.me.keybinds.KeyBinding;
import net.me.keybinds.KeybindManager;
import net.me.utils.McUtils;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.bridge.GrapheneBridgeSubscription;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class MQSKeybindsBridge implements AutoCloseable {
    private static final String ACTION_CREATE = "create";
    private static final String ACTION_UPDATE = "update";
    private static final String ACTION_DELETE = "delete";

    private static final String MESSAGE_PAYLOAD_REQUIRED = "payload is required";
    private static final String MESSAGE_NAME_REQUIRED = "name is required";
    private static final String MESSAGE_KEY_CODE_REQUIRED = "keyCode is required";
    private static final String MESSAGE_SCRIPT_ID_REQUIRED = "scriptId is required for script keybinds";
    private static final String MESSAGE_SCRIPT_KEYBIND_NOT_FOUND = "script keybind not found";
    private static final String MESSAGE_HOST_KEYBIND_NOT_FOUND = "host keybind not found";
    private static final String MESSAGE_KEYBIND_NOT_FOUND = "keybind not found";
    private static final String MESSAGE_SCRIPT_KEYBINDS_ARE_SCRIPT_OWNED = "script keybinds are created by scripts";

    private static final String SCOPE_SCRIPT = "script";
    private static final String SCOPE_HOST = "host";

    public static final String CHANNEL_LIST = "mqs:keybinds:list";
    public static final String CHANNEL_CREATE = "mqs:keybinds:create";
    public static final String CHANNEL_UPDATE = "mqs:keybinds:update";
    public static final String CHANNEL_DELETE = "mqs:keybinds:delete";

    public static final String EVENT_UPDATED = "mqs:keybinds:updated";
    public static final String EVENT_TRIGGERED = "mqs:keybinds:triggered";

    private final KeybindManager keybindManager;
    private final List<GrapheneBridgeSubscription> subscriptions;
    private GrapheneBridge bridge;

    public MQSKeybindsBridge(KeybindManager keybindManager) {
        this.keybindManager = Objects.requireNonNull(keybindManager, "keybindManager");
        this.subscriptions = new ArrayList<>();
    }

    public void attach(GrapheneBridge bridge) {
        close();
        this.bridge = Objects.requireNonNull(bridge, "bridge");

        this.subscriptions.add(this.bridge.onReady(this::emitUpdated));
        this.subscriptions.add(this.bridge.onRequestJson(
                CHANNEL_LIST,
                Object.class,
                (ignoredChannel, ignoredPayload) -> CompletableFuture.completedFuture(buildSnapshot())
        ));
        this.subscriptions.add(this.bridge.onRequestJson(
                CHANNEL_CREATE,
                KeybindMutationRequest.class,
                (ignoredChannel, payload) -> CompletableFuture.completedFuture(handleCreate(payload))
        ));
        this.subscriptions.add(this.bridge.onRequestJson(
                CHANNEL_UPDATE,
                KeybindMutationRequest.class,
                (ignoredChannel, payload) -> CompletableFuture.completedFuture(handleUpdate(payload))
        ));
        this.subscriptions.add(this.bridge.onRequestJson(
                CHANNEL_DELETE,
                KeybindMutationRequest.class,
                (ignoredChannel, payload) -> CompletableFuture.completedFuture(handleDelete(payload))
        ));
    }

    @Override
    public void close() {
        for (GrapheneBridgeSubscription subscription : this.subscriptions) {
            try {
                subscription.unsubscribe();
            } catch (RuntimeException exception) {
                Main.LOGGER.debug("Failed to unsubscribe MQS keybinds bridge handler.", exception);
            }
        }

        this.subscriptions.clear();
        this.bridge = null;
    }

    private KeybindMutationResponse handleCreate(KeybindMutationRequest payload) {
        RequestValidationResult validationResult = validateRequest(ACTION_CREATE, payload, true);
        if (validationResult.hasFailure()) {
            return validationResult.failure();
        }

        ValidatedRequest request = validationResult.request();
        if (SCOPE_SCRIPT.equals(request.scope())) {
            return failure(ACTION_CREATE, MESSAGE_SCRIPT_KEYBINDS_ARE_SCRIPT_OWNED);
        }

        boolean repeatable = payload.repeatable() != null && payload.repeatable();
        int debounceMillis = payload.debounceMillis() == null ? 100 : Math.max(0, payload.debounceMillis());
        this.keybindManager.registerHost(
                request.name(),
                () -> emitTriggered(request.name()),
                request.keyCode(),
                repeatable,
                debounceMillis
        );

        KeybindStateResponse created = toHostResponse(this.keybindManager.findHostKeybind(request.name()));
        if (created == null) {
            return failure(ACTION_CREATE, "failed to create host keybind");
        }

        return success(ACTION_CREATE, "host keybind created", created);
    }

    private KeybindMutationResponse handleUpdate(KeybindMutationRequest payload) {
        RequestValidationResult validationResult = validateRequest(ACTION_UPDATE, payload, true);
        if (validationResult.hasFailure()) {
            return validationResult.failure();
        }

        ValidatedRequest request = validationResult.request();
        KeybindStateResponse updated;
        if (SCOPE_SCRIPT.equals(request.scope())) {
            ScriptBindingResolution scriptBindingResolution = resolveScriptBinding(ACTION_UPDATE, request);
            if (scriptBindingResolution.hasFailure()) {
                return scriptBindingResolution.failure();
            }

            KeyBinding scriptBinding = scriptBindingResolution.binding();
            this.keybindManager.rebindKey(scriptBinding, request.keyCode());
            updated = toScriptResponse(scriptBinding);
        } else {
            HostKeyBinding hostBinding = this.keybindManager.findHostKeybind(request.name());
            if (hostBinding == null) {
                return failure(ACTION_UPDATE, MESSAGE_HOST_KEYBIND_NOT_FOUND);
            }

            this.keybindManager.rebindHostKey(hostBinding, request.keyCode());
            updated = toHostResponse(hostBinding);
        }

        return success(ACTION_UPDATE, "keybind updated", updated);
    }

    private KeybindMutationResponse handleDelete(KeybindMutationRequest payload) {
        RequestValidationResult validationResult = validateRequest(ACTION_DELETE, payload, false);
        if (validationResult.hasFailure()) {
            return validationResult.failure();
        }

        ValidatedRequest request = validationResult.request();
        boolean deleted;
        if (SCOPE_SCRIPT.equals(request.scope())) {
            ScriptBindingResolution scriptBindingResolution = resolveScriptBinding(ACTION_DELETE, request);
            if (scriptBindingResolution.hasFailure()) {
                return scriptBindingResolution.failure();
            }

            KeyBinding scriptBinding = scriptBindingResolution.binding();
            deleted = this.keybindManager.unregister(scriptBinding.getOwner(), scriptBinding.getName());
        } else {
            deleted = this.keybindManager.unregisterHost(request.name());
        }

        if (!deleted) {
            return failure(ACTION_DELETE, MESSAGE_KEYBIND_NOT_FOUND);
        }

        return success(ACTION_DELETE, "keybind deleted", null);
    }

    private RequestValidationResult validateRequest(String action, KeybindMutationRequest payload, boolean requireKeyCode) {
        if (payload == null) {
            return RequestValidationResult.failure(failure(action, MESSAGE_PAYLOAD_REQUIRED));
        }

        String name = normalizeValue(payload.name());
        if (name == null) {
            return RequestValidationResult.failure(failure(action, MESSAGE_NAME_REQUIRED));
        }

        Integer keyCode = payload.keyCode();
        if (requireKeyCode && keyCode == null) {
            return RequestValidationResult.failure(failure(action, MESSAGE_KEY_CODE_REQUIRED));
        }

        return RequestValidationResult.success(new ValidatedRequest(
                normalizeScope(payload.scope(), payload.scriptId()),
                normalizeValue(payload.scriptId()),
                name,
                keyCode
        ));
    }

    private ScriptBindingResolution resolveScriptBinding(String action, ValidatedRequest request) {
        if (request.scriptId() == null) {
            return ScriptBindingResolution.failure(failure(action, MESSAGE_SCRIPT_ID_REQUIRED));
        }

        KeyBinding scriptBinding = this.keybindManager.findScriptKeybind(request.scriptId(), request.name());
        if (scriptBinding == null) {
            return ScriptBindingResolution.failure(failure(action, MESSAGE_SCRIPT_KEYBIND_NOT_FOUND));
        }

        return ScriptBindingResolution.success(scriptBinding);
    }

    private KeybindMutationResponse success(String action, String message, KeybindStateResponse keybind) {
        KeybindSnapshotResponse snapshot = buildSnapshot();
        emitUpdated(snapshot);
        return KeybindMutationResponse.success(action, message, keybind, snapshot);
    }

    private KeybindMutationResponse failure(String action, String message) {
        return KeybindMutationResponse.failure(action, message, buildSnapshot());
    }

    private KeybindSnapshotResponse buildSnapshot() {
        List<KeybindStateResponse> scriptKeybinds = this.keybindManager.getScriptKeybinds().stream()
                .map(this::toScriptResponse)
                .sorted(
                        Comparator.comparing(KeybindStateResponse::scriptName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                                .thenComparing(KeybindStateResponse::name, String.CASE_INSENSITIVE_ORDER)
                )
                .toList();

        List<KeybindStateResponse> hostKeybinds = this.keybindManager.getHostKeybinds().stream()
                .map(this::toHostResponse)
                .sorted(Comparator.comparing(KeybindStateResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<KeybindStateResponse> keybinds = new ArrayList<>(scriptKeybinds.size() + hostKeybinds.size());
        keybinds.addAll(scriptKeybinds);
        keybinds.addAll(hostKeybinds);

        return new KeybindSnapshotResponse(keybinds, keybinds.size(), scriptKeybinds.size(), hostKeybinds.size());
    }

    private KeybindStateResponse toScriptResponse(KeyBinding keyBinding) {
        return new KeybindStateResponse(
                SCOPE_SCRIPT,
                keyBinding.getOwner().getId(),
                keyBinding.getOwner().getName(),
                keyBinding.getName(),
                keyBinding.getKey(),
                keyBinding.getKeyName()
        );
    }

    private KeybindStateResponse toHostResponse(HostKeyBinding keyBinding) {
        if (keyBinding == null) {
            return null;
        }

        return new KeybindStateResponse(
                SCOPE_HOST,
                null,
                null,
                keyBinding.getName(),
                keyBinding.getKey(),
                keyBinding.getKeyName()
        );
    }

    private String normalizeScope(String scopeValue, String scriptIdValue) {
        String scope = normalizeValue(scopeValue);
        if (scope == null) {
            return normalizeValue(scriptIdValue) != null ? SCOPE_SCRIPT : SCOPE_HOST;
        }

        String normalized = scope.toLowerCase(Locale.ROOT);
        return SCOPE_SCRIPT.equals(normalized) ? SCOPE_SCRIPT : SCOPE_HOST;
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void emitUpdated() {
        emitUpdated(buildSnapshot());
    }

    private void emitUpdated(KeybindSnapshotResponse snapshot) {
        GrapheneBridge activeBridge = this.bridge;
        if (activeBridge == null) {
            return;
        }

        try {
            activeBridge.emitJson(EVENT_UPDATED, snapshot);
        } catch (RuntimeException exception) {
            Main.LOGGER.debug("Failed to emit keybind update bridge event.", exception);
        }
    }

    private void emitTriggered(String keybindName) {
        McUtils.getMc().execute(() -> {
            GrapheneBridge activeBridge = this.bridge;
            if (activeBridge == null) {
                return;
            }

            HostKeyBinding keybind = this.keybindManager.findHostKeybind(keybindName);
            int keyCode = keybind == null ? -1 : keybind.getKey();
            String keyName = keybind == null ? "Unknown" : keybind.getKeyName();

            try {
                activeBridge.emitJson(EVENT_TRIGGERED, new KeybindTriggeredEvent(keybindName, keyCode, keyName));
            } catch (RuntimeException exception) {
                Main.LOGGER.debug("Failed to emit keybind triggered bridge event.", exception);
            }
        });
    }

    private record ValidatedRequest(String scope, String scriptId, String name, Integer keyCode) {
    }

    private record RequestValidationResult(ValidatedRequest request, KeybindMutationResponse failure) {
        static RequestValidationResult success(ValidatedRequest request) {
            return new RequestValidationResult(request, null);
        }

        static RequestValidationResult failure(KeybindMutationResponse failure) {
            return new RequestValidationResult(null, failure);
        }

        boolean hasFailure() {
            return this.failure != null;
        }
    }

    private record ScriptBindingResolution(KeyBinding binding, KeybindMutationResponse failure) {
        static ScriptBindingResolution success(KeyBinding binding) {
            return new ScriptBindingResolution(binding, null);
        }

        static ScriptBindingResolution failure(KeybindMutationResponse failure) {
            return new ScriptBindingResolution(null, failure);
        }

        boolean hasFailure() {
            return this.failure != null;
        }
    }

    public record KeybindMutationRequest(
            String scope,
            String scriptId,
            String name,
            Integer keyCode,
            Boolean repeatable,
            Integer debounceMillis
    ) {
    }

    public record KeybindSnapshotResponse(
            List<KeybindStateResponse> keybinds,
            int totalCount,
            int scriptCount,
            int hostCount
    ) {
    }

    public record KeybindStateResponse(
            String scope,
            String scriptId,
            String scriptName,
            String name,
            int keyCode,
            String keyName
    ) {
    }

    public record KeybindMutationResponse(
            boolean success,
            String action,
            String message,
            KeybindStateResponse keybind,
            KeybindSnapshotResponse snapshot
    ) {
        public static KeybindMutationResponse success(
                String action,
                String message,
                KeybindStateResponse keybind,
                KeybindSnapshotResponse snapshot
        ) {
            return new KeybindMutationResponse(true, action, message, keybind, snapshot);
        }

        public static KeybindMutationResponse failure(
                String action,
                String message,
                KeybindSnapshotResponse snapshot
        ) {
            return new KeybindMutationResponse(false, action, message, null, snapshot);
        }
    }

    public record KeybindTriggeredEvent(String name, int keyCode, String keyName) {
    }
}

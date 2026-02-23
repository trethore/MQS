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
import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;
import net.me.console.ConsoleMessage;
import net.me.utils.McUtils;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.bridge.GrapheneBridgeSubscription;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class MQSConsoleBridge implements AutoCloseable {
    public static final String CHANNEL_SNAPSHOT = "mqs:console:snapshot";
    public static final String CHANNEL_COMMANDS = "mqs:console:commands";
    public static final String CHANNEL_EXECUTE = "mqs:console:execute";
    public static final String CHANNEL_CLEAR = "mqs:console:clear";

    public static final String EVENT_MESSAGE = "mqs:console:message";
    public static final String EVENT_CLEARED = "mqs:console:cleared";
    public static final String EVENT_SNAPSHOT_UPDATED = "mqs:console:snapshot:updated";

    private final ConsoleManager consoleManager;
    private final List<GrapheneBridgeSubscription> subscriptions;
    private final ConsoleManager.ConsoleListener consoleListener;

    private GrapheneBridge bridge;
    private boolean listening;

    public MQSConsoleBridge(ConsoleManager consoleManager) {
        this.consoleManager = Objects.requireNonNull(consoleManager, "consoleManager");
        this.subscriptions = new ArrayList<>();
        this.consoleListener = new ConsoleManager.ConsoleListener() {
            @Override
            public void onMessageAdded(ConsoleMessage message) {
                emitOnClientThread(EVENT_MESSAGE, toMessageResponse(message));
            }

            @Override
            public void onCleared() {
                emitOnClientThread(EVENT_CLEARED, new ConsoleClearedResponse(true));
            }
        };
    }

    public void attach(GrapheneBridge bridge) {
        close();
        this.bridge = Objects.requireNonNull(bridge, "bridge");

        this.subscriptions.add(this.bridge.onReady(() -> emitOnClientThread(EVENT_SNAPSHOT_UPDATED, buildSnapshot())));
        this.subscriptions.add(this.bridge.onRequestJson(
                CHANNEL_SNAPSHOT,
                Object.class,
                (ignoredChannel, ignoredPayload) -> CompletableFuture.completedFuture(buildSnapshot())
        ));
        this.subscriptions.add(this.bridge.onRequestJson(
                CHANNEL_COMMANDS,
                Object.class,
                (ignoredChannel, ignoredPayload) -> CompletableFuture.completedFuture(buildCommandsResponse())
        ));
        this.subscriptions.add(this.bridge.onRequestJson(
                CHANNEL_EXECUTE,
                ConsoleExecuteRequest.class,
                (ignoredChannel, payload) -> CompletableFuture.completedFuture(handleExecute(payload))
        ));
        this.subscriptions.add(this.bridge.onRequestJson(
                CHANNEL_CLEAR,
                Object.class,
                (ignoredChannel, ignoredPayload) -> CompletableFuture.completedFuture(handleClear())
        ));

        this.consoleManager.addListener(this.consoleListener);
        this.listening = true;
    }

    @Override
    public void close() {
        for (GrapheneBridgeSubscription subscription : this.subscriptions) {
            try {
                subscription.unsubscribe();
            } catch (RuntimeException exception) {
                Main.LOGGER.debug("Failed to unsubscribe MQS console bridge handler.", exception);
            }
        }

        this.subscriptions.clear();
        if (this.listening) {
            this.consoleManager.removeListener(this.consoleListener);
            this.listening = false;
        }

        this.bridge = null;
    }

    private ConsoleExecuteResponse handleExecute(ConsoleExecuteRequest payload) {
        String input = payload == null || payload.input() == null ? "" : payload.input().trim();
        if (input.isEmpty()) {
            return new ConsoleExecuteResponse(false, "payload must include a non-empty input", buildSnapshot());
        }

        try {
            this.consoleManager.executeCommand(input);
        } catch (RuntimeException exception) {
            Main.LOGGER.error("Failed to execute console command from UI bridge.", exception);
            return new ConsoleExecuteResponse(false, exception.getMessage(), buildSnapshot());
        }

        return new ConsoleExecuteResponse(true, "command executed", buildSnapshot());
    }

    private ConsoleClearResponse handleClear() {
        try {
            this.consoleManager.clear();
        } catch (RuntimeException exception) {
            Main.LOGGER.error("Failed to clear console from UI bridge.", exception);
            return new ConsoleClearResponse(false);
        }

        return new ConsoleClearResponse(true);
    }

    private ConsoleSnapshotResponse buildSnapshot() {
        List<ConsoleMessageResponse> messages = this.consoleManager.getMessages().stream()
                .map(this::toMessageResponse)
                .toList();

        List<String> history = List.copyOf(this.consoleManager.getCommandHistory());
        return new ConsoleSnapshotResponse(messages, history, messages.size());
    }

    private ConsoleCommandsResponse buildCommandsResponse() {
        List<ConsoleCommandResponse> commands = this.consoleManager.getCommands().values().stream()
                .sorted(Comparator.comparing(ConsoleCommand::getName, String.CASE_INSENSITIVE_ORDER))
                .map(command -> new ConsoleCommandResponse(command.getName(), command.getDescription(), command.getUsage()))
                .toList();

        return new ConsoleCommandsResponse(commands);
    }

    private ConsoleMessageResponse toMessageResponse(ConsoleMessage message) {
        return new ConsoleMessageResponse(
                message.text(),
                message.type().name(),
                message.timestamp()
        );
    }

    private void emitOnClientThread(String channel, Object payload) {
        McUtils.getMc().execute(() -> {
            GrapheneBridge activeBridge = this.bridge;
            if (activeBridge == null) {
                return;
            }

            try {
                activeBridge.emitJson(channel, payload);
            } catch (RuntimeException exception) {
                Main.LOGGER.debug("Failed to emit console bridge event on channel '{}'.", channel, exception);
            }
        });
    }

    public record ConsoleExecuteRequest(String input) {
    }

    public record ConsoleExecuteResponse(boolean success, String message, ConsoleSnapshotResponse snapshot) {
    }

    public record ConsoleClearResponse(boolean success) {
    }

    public record ConsoleClearedResponse(boolean cleared) {
    }

    public record ConsoleSnapshotResponse(
            List<ConsoleMessageResponse> messages,
            List<String> commandHistory,
            int messageCount
    ) {
    }

    public record ConsoleMessageResponse(String text, String type, String timestamp) {
    }

    public record ConsoleCommandsResponse(List<ConsoleCommandResponse> commands) {
    }

    public record ConsoleCommandResponse(String name, String description, String usage) {
    }
}

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
import net.me.scripting.ScriptManager;
import net.me.scripting.commands.CommandAPIService;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.bridge.GrapheneBridgeSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class MQSCommandsBridge implements AutoCloseable {
    public static final String CHANNEL_LIST = "mqs:commands:list";

    private final ScriptManager scriptManager;
    private final List<GrapheneBridgeSubscription> subscriptions;

    public MQSCommandsBridge(ScriptManager scriptManager) {
        this.scriptManager = Objects.requireNonNull(scriptManager, "scriptManager");
        this.subscriptions = new ArrayList<>();
    }

    public void attach(GrapheneBridge bridge) {
        close();
        GrapheneBridge validatedBridge = Objects.requireNonNull(bridge, "bridge");

        this.subscriptions.add(validatedBridge.onRequestJson(
                CHANNEL_LIST,
                Object.class,
                (ignoredChannel, ignoredPayload) -> CompletableFuture.completedFuture(buildSnapshot())
        ));
    }

    @Override
    public void close() {
        for (GrapheneBridgeSubscription subscription : this.subscriptions) {
            try {
                subscription.unsubscribe();
            } catch (RuntimeException exception) {
                Main.LOGGER.debug("Failed to unsubscribe MQS commands bridge handler.", exception);
            }
        }

        this.subscriptions.clear();
    }

    private CommandsSnapshotResponse buildSnapshot() {
        CommandAPIService commandApiService = this.scriptManager.getCommandApiService();
        if (commandApiService == null) {
            return new CommandsSnapshotResponse(List.of(), 0);
        }

        List<CommandResponse> commands = commandApiService.getManagedCommandsSnapshot().stream()
                .map(command -> new CommandResponse(
                        command.name(),
                        command.ownerScriptId(),
                        command.ownerScriptName(),
                        command.queued()
                ))
                .toList();

        return new CommandsSnapshotResponse(commands, commands.size());
    }

    public record CommandsSnapshotResponse(List<CommandResponse> commands, int totalCount) {
    }

    public record CommandResponse(String name, String scriptId, String scriptName, boolean queued) {
    }
}

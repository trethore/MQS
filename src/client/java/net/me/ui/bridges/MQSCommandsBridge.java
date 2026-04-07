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

import net.me.scripting.ScriptManager;
import net.me.scripting.commands.CommandAPIService;
import net.me.ui.bridges.utils.BridgeRequests;
import net.me.ui.bridges.utils.BridgeSubscriptions;
import tytoo.grapheneui.api.bridge.GrapheneBridge;

import java.util.List;
import java.util.Objects;

public final class MQSCommandsBridge implements AutoCloseable {
    public static final String CHANNEL_LIST = "mqs:commands:list";
    private static final String BRIDGE_NAME = "MQS commands";

    private final ScriptManager scriptManager;
    private final BridgeSubscriptions subscriptions;

    public MQSCommandsBridge(ScriptManager scriptManager) {
        this.scriptManager = Objects.requireNonNull(scriptManager, "scriptManager");
        this.subscriptions = new BridgeSubscriptions(BRIDGE_NAME);
    }

    public void attach(GrapheneBridge bridge) {
        close();
        GrapheneBridge validatedBridge = Objects.requireNonNull(bridge, "bridge");

        this.subscriptions.add(BridgeRequests.onRequestJson(validatedBridge, CHANNEL_LIST, this::buildSnapshot));
    }

    @Override
    public void close() {
        this.subscriptions.close();
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

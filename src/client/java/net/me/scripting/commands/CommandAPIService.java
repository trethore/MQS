/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
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

package net.me.scripting.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.Main;
import net.me.mixin.client.accessors.CommandNodeAccessor;
import net.me.scripting.module.RunningScript;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CommandAPIService {
    private final Map<RunningScript, Map<String, LiteralArgumentBuilder<FabricClientCommandSource>>> scriptCommands = new ConcurrentHashMap<>();
    private final Set<String> allManagedCommandNames = ConcurrentHashMap.newKeySet();
    private final Queue<QueuedCommand> commandQueue = new ConcurrentLinkedQueue<>();

    public void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) -> {
            Main.LOGGER.info("Client command registration event fired. Re-registering script commands.");

            for (Map.Entry<RunningScript, Map<String, LiteralArgumentBuilder<FabricClientCommandSource>>> scriptEntry : scriptCommands.entrySet()) {
                for (LiteralArgumentBuilder<FabricClientCommandSource> builder : scriptEntry.getValue().values()) {
                    dispatcher.register(builder);
                    Main.LOGGER.debug("Re-registered command '{}' for script '{}'", builder.getLiteral(), scriptEntry.getKey().getName());
                }
            }

            if (!commandQueue.isEmpty()) {
                Main.LOGGER.info("Registering {} queued script commands...", commandQueue.size());
                QueuedCommand queuedCommand;
                while ((queuedCommand = commandQueue.poll()) != null) {
                    try {
                        registerInternal(dispatcher, queuedCommand.owner(), queuedCommand.builder());
                    } catch (Exception e) {
                        Main.LOGGER.error("Failed to register queued command '{}' for script '{}': {}",
                                queuedCommand.builder().getLiteral(), queuedCommand.owner().getName(), e.getMessage());
                    }
                }
            }
        });
    }

    public void register(RunningScript owner, CommandBuilder commandBuilder) {
        LiteralArgumentBuilder<FabricClientCommandSource> literalBuilder = commandBuilder.getRootBuilder();
        CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.getActiveDispatcher();

        if (dispatcher == null) {
            Main.LOGGER.warn("Dispatcher not available. Queuing command '{}' for later registration.", literalBuilder.getLiteral());
            commandQueue.add(new QueuedCommand(owner, literalBuilder));
            return;
        }

        registerInternal(dispatcher, owner, literalBuilder);
        pushCommandTree(dispatcher);
    }

    private void registerInternal(CommandDispatcher<FabricClientCommandSource> dispatcher, RunningScript owner, LiteralArgumentBuilder<FabricClientCommandSource> literalBuilder) {
        String name = literalBuilder.getLiteral();
        if (dispatcher.getRoot().getChild(name) != null) {
            Main.LOGGER.warn("Command '{}' already exists. It will be replaced.", name);
            removeNode(name);
        }
        dispatcher.register(literalBuilder);
        scriptCommands.computeIfAbsent(owner, _ -> new ConcurrentHashMap<>()).put(name, literalBuilder);
        allManagedCommandNames.add(name);
    }

    public void unregister(RunningScript owner, String name) {
        commandQueue.removeIf(qc -> qc.owner().equals(owner) && qc.builder().getLiteral().equals(name));

        Map<String, LiteralArgumentBuilder<FabricClientCommandSource>> owned = scriptCommands.get(owner);
        if (owned == null || !owned.containsKey(name)) {
            Main.LOGGER.warn("Script '{}' tried to unregister '{}' which it does not own or wasn’t found.", owner.getName(), name);
            return;
        }

        if (removeNode(name)) {
            owned.remove(name);
            if (owned.isEmpty()) {
                scriptCommands.remove(owner);
            }
            CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.getActiveDispatcher();
            if (dispatcher != null) {
                pushCommandTree(dispatcher);
            }
        }
    }

    public void unregisterAllFor(RunningScript owner) {
        commandQueue.removeIf(qc -> qc.owner().equals(owner));

        Map<String, LiteralArgumentBuilder<FabricClientCommandSource>> owned = scriptCommands.remove(owner);
        if (owned == null) return;

        boolean changed = false;
        for (String name : owned.keySet()) {
            if (removeNode(name)) {
                Main.LOGGER.info("Unregistered '{}' from disabled script '{}'", name, owner.getName());
                changed = true;
            }
        }

        if (changed) {
            CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.getActiveDispatcher();
            if (dispatcher != null) {
                pushCommandTree(dispatcher);
            }
        }
    }

    private boolean removeNode(String name) {
        CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.getActiveDispatcher();
        if (dispatcher == null) return false;

        RootCommandNode<FabricClientCommandSource> root = dispatcher.getRoot();
        @SuppressWarnings("unchecked")
        CommandNodeAccessor<FabricClientCommandSource> accessor = (CommandNodeAccessor<FabricClientCommandSource>) root;

        boolean removed = accessor.getChildrenMap().remove(name) != null;
        if (removed) {
            accessor.getLiteralsMap().remove(name);
        }
        return removed;
    }

    private void pushCommandTree(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) {
            return;
        }

        client.execute(() -> {
            final CommandDispatcher<ClientSuggestionProvider> suggestionDispatcher = client.getConnection().getCommands();
            final RootCommandNode<ClientSuggestionProvider> suggestionRoot = suggestionDispatcher.getRoot();
            @SuppressWarnings("unchecked") final CommandNodeAccessor<ClientSuggestionProvider> rootAccessor = (CommandNodeAccessor<ClientSuggestionProvider>) suggestionRoot;

            final Map<String, CommandNode<ClientSuggestionProvider>> children = rootAccessor.getChildrenMap();
            final Map<String, LiteralCommandNode<ClientSuggestionProvider>> literals = rootAccessor.getLiteralsMap();

            for (String commandName : allManagedCommandNames) {
                children.remove(commandName);
                literals.remove(commandName);
            }

            for (CommandNode<FabricClientCommandSource> node : dispatcher.getRoot().getChildren()) {
                if (allManagedCommandNames.contains(node.getName())) {
                    @SuppressWarnings("unchecked")
                    CommandNode<ClientSuggestionProvider> castedNode = (CommandNode<ClientSuggestionProvider>) (Object) node;
                    suggestionRoot.addChild(castedNode);
                }
            }
            Main.LOGGER.info("Patched client suggestion command tree in-memory.");
        });
    }

    private record QueuedCommand(RunningScript owner, LiteralArgumentBuilder<FabricClientCommandSource> builder) {
    }
}

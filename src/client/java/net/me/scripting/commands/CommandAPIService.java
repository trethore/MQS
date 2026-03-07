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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CommandAPIService {
    private final Map<RunningScript, Map<String, LiteralArgumentBuilder<FabricClientCommandSource>>> scriptCommands = new ConcurrentHashMap<>();
    private final Set<String> allManagedCommandNames = ConcurrentHashMap.newKeySet();
    private final Queue<QueuedCommand> commandQueue = new ConcurrentLinkedQueue<>();

    public void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, ignored) -> {
            Main.LOGGER.info("Client command registration event fired. Re-registering script commands.");
            reRegisterAllCommands(dispatcher);
            processQueuedCommands(dispatcher);
        });
    }

    private void reRegisterAllCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        scriptCommands.forEach((script, commands) ->
                commands.values().forEach(builder -> {
                    dispatcher.register(builder);
                    Main.LOGGER.debug("Re-registered command '{}' for script '{}'", builder.getLiteral(), script.getName());
                })
        );
    }

    private void processQueuedCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        if (commandQueue.isEmpty()) {
            return;
        }

        Main.LOGGER.info("Registering {} queued script commands...", commandQueue.size());

        QueuedCommand queuedCommand;
        while ((queuedCommand = commandQueue.poll()) != null) {
            registerQueuedCommand(dispatcher, queuedCommand);
        }
    }

    private void registerQueuedCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, QueuedCommand queuedCommand) {
        try {
            registerInternal(dispatcher, queuedCommand.owner(), queuedCommand.builder());
        } catch (Exception e) {
            Main.LOGGER.error("Failed to register queued command '{}' for script '{}': {}",
                    queuedCommand.builder().getLiteral(), queuedCommand.owner().getName(), e.getMessage());
        }
    }

    public void register(RunningScript owner, CommandBuilder commandBuilder) {
        LiteralArgumentBuilder<FabricClientCommandSource> literalBuilder = commandBuilder.getRootBuilder();
        CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.getActiveDispatcher();

        if (dispatcher == null) {
            Main.LOGGER.warn("Dispatcher not available. Queuing command '{}' for later registration.", literalBuilder.getLiteral());
            removeOwnershipForName(literalBuilder.getLiteral());
            commandQueue.add(new QueuedCommand(owner, literalBuilder));
            reconcileManagedCommandName(literalBuilder.getLiteral());
            return;
        }

        commandQueue.removeIf(qc -> qc.builder().getLiteral().equals(literalBuilder.getLiteral()));
        registerInternal(dispatcher, owner, literalBuilder);
        pushCommandTree(dispatcher);
    }

    private void registerInternal(CommandDispatcher<FabricClientCommandSource> dispatcher, RunningScript owner, LiteralArgumentBuilder<FabricClientCommandSource> literalBuilder) {
        String name = literalBuilder.getLiteral();
        if (dispatcher.getRoot().getChild(name) != null) {
            Main.LOGGER.warn("Command '{}' already exists. It will be replaced.", name);
            removeNode(name);
        }
        removeOwnershipFromMaps(name);
        dispatcher.register(literalBuilder);
        scriptCommands.computeIfAbsent(owner, ignored -> new ConcurrentHashMap<>()).put(name, literalBuilder);
        reconcileManagedCommandName(name);
    }

    public void unregister(RunningScript owner, String name) {
        commandQueue.removeIf(qc -> qc.owner().equals(owner) && qc.builder().getLiteral().equals(name));

        Map<String, LiteralArgumentBuilder<FabricClientCommandSource>> owned = scriptCommands.get(owner);
        if (owned == null || !owned.containsKey(name)) {
            Main.LOGGER.warn("Script '{}' tried to unregister '{}' which it does not own or wasn’t found.", owner.getName(), name);
            reconcileManagedCommandName(name);
            return;
        }

        removeNode(name);
        owned.remove(name);
        if (owned.isEmpty()) {
            scriptCommands.remove(owner);
        }

        CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.getActiveDispatcher();
        if (dispatcher != null) {
            pushCommandTree(dispatcher);
        }
        reconcileManagedCommandName(name);
    }

    public void unregisterAllFor(RunningScript owner) {
        Set<String> touchedNames = new HashSet<>();
        commandQueue.removeIf(qc -> {
            if (!qc.owner().equals(owner)) {
                return false;
            }
            touchedNames.add(qc.builder().getLiteral());
            return true;
        });

        Map<String, LiteralArgumentBuilder<FabricClientCommandSource>> owned = scriptCommands.remove(owner);
        if (owned != null) {
            touchedNames.addAll(owned.keySet());
        }
        if (touchedNames.isEmpty()) {
            return;
        }

        if (owned != null) {
            for (String name : owned.keySet()) {
                if (removeNode(name)) {
                    Main.LOGGER.info("Unregistered '{}' from disabled script '{}'", name, owner.getName());
                }
            }
        }

        CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.getActiveDispatcher();
        if (dispatcher != null && owned != null && !owned.isEmpty()) {
            pushCommandTree(dispatcher);
        }

        for (String name : touchedNames) {
            reconcileManagedCommandName(name);
        }
    }

    private void removeOwnershipForName(String name) {
        removeOwnershipFromMaps(name);
        commandQueue.removeIf(qc -> qc.builder().getLiteral().equals(name));
        reconcileManagedCommandName(name);
    }

    private void removeOwnershipFromMaps(String name) {
        for (Map.Entry<RunningScript, Map<String, LiteralArgumentBuilder<FabricClientCommandSource>>> entry : scriptCommands.entrySet()) {
            Map<String, LiteralArgumentBuilder<FabricClientCommandSource>> owned = entry.getValue();
            owned.remove(name);
            if (owned.isEmpty()) {
                scriptCommands.remove(entry.getKey(), owned);
            }
        }
    }

    private void reconcileManagedCommandName(String name) {
        if (isNameManaged(name)) {
            allManagedCommandNames.add(name);
        } else {
            allManagedCommandNames.remove(name);
        }
    }

    private boolean isNameManaged(String name) {
        for (Map<String, LiteralArgumentBuilder<FabricClientCommandSource>> owned : scriptCommands.values()) {
            if (owned.containsKey(name)) {
                return true;
            }
        }
        for (QueuedCommand queuedCommand : commandQueue) {
            if (queuedCommand.builder().getLiteral().equals(name)) {
                return true;
            }
        }
        return false;
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

        Set<String> managedNamesSnapshot = Set.copyOf(allManagedCommandNames);

        client.execute(() -> {
            final CommandDispatcher<ClientSuggestionProvider> suggestionDispatcher = client.getConnection().getCommands();
            final RootCommandNode<ClientSuggestionProvider> suggestionRoot = suggestionDispatcher.getRoot();
            @SuppressWarnings("unchecked") final CommandNodeAccessor<ClientSuggestionProvider> rootAccessor = (CommandNodeAccessor<ClientSuggestionProvider>) suggestionRoot;

            final Map<String, CommandNode<ClientSuggestionProvider>> children = rootAccessor.getChildrenMap();
            final Map<String, LiteralCommandNode<ClientSuggestionProvider>> literals = rootAccessor.getLiteralsMap();

            for (String commandName : managedNamesSnapshot) {
                children.remove(commandName);
                literals.remove(commandName);
            }

            for (CommandNode<FabricClientCommandSource> node : dispatcher.getRoot().getChildren()) {
                if (managedNamesSnapshot.contains(node.getName())) {
                    @SuppressWarnings("unchecked")
                    CommandNode<ClientSuggestionProvider> castedNode = (CommandNode<ClientSuggestionProvider>) (Object) node;
                    suggestionRoot.addChild(castedNode);
                }
            }
            Main.LOGGER.info("Patched client suggestion command tree in-memory.");
        });
    }

    public List<ManagedCommandInfo> getManagedCommandsSnapshot() {
        List<ManagedCommandInfo> commands = new ArrayList<>();

        scriptCommands.forEach((owner, ownedCommands) ->
                ownedCommands.keySet().forEach(commandName ->
                        commands.add(new ManagedCommandInfo(commandName, owner.getId(), owner.getName(), false))
                )
        );

        for (QueuedCommand queuedCommand : commandQueue) {
            commands.add(new ManagedCommandInfo(
                    queuedCommand.builder().getLiteral(),
                    queuedCommand.owner().getId(),
                    queuedCommand.owner().getName(),
                    true
            ));
        }

        return commands.stream()
                .sorted(
                        Comparator.comparing(ManagedCommandInfo::name, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(ManagedCommandInfo::ownerScriptName, String.CASE_INSENSITIVE_ORDER)
                )
                .toList();
    }

    public record ManagedCommandInfo(String name, String ownerScriptId, String ownerScriptName, boolean queued) {
    }

    private record QueuedCommand(RunningScript owner, LiteralArgumentBuilder<FabricClientCommandSource> builder) {
    }
}

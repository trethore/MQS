package net.me.scripting.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.Main;
import net.me.mixin.client.accessors.CommandNodeAccessor;
import net.me.scripting.module.RunningScript;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CommandAPIService {
    private static final CommandAPIService INSTANCE = new CommandAPIService();
    private final Map<RunningScript, Set<String>> scriptCommands = new ConcurrentHashMap<>();

    private CommandAPIService() {
    }

    public static CommandAPIService getInstance() {
        return INSTANCE;
    }

    public void register(RunningScript owner, CommandBuilder commandBuilder) {
        LiteralArgumentBuilder<FabricClientCommandSource> literalBuilder = commandBuilder.getRootBuilder();
        String name = literalBuilder.getLiteral();

        CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.getActiveDispatcher();
        if (dispatcher == null) {
            Main.LOGGER.warn("Cannot register command '{}' – dispatcher not available", name);
            return;
        }

        if (dispatcher.getRoot().getChild(name) != null) {
            throw new IllegalStateException("Command '" + name + "' is already registered.");
        }

        dispatcher.register(literalBuilder);
        scriptCommands.computeIfAbsent(owner, k -> new HashSet<>()).add(name);
        pushCommandTree(dispatcher);
    }

    public void unregister(RunningScript owner, String name) {
        Set<String> owned = scriptCommands.get(owner);
        if (owned == null || !owned.contains(name)) {
            Main.LOGGER.warn(
                    "Script '{}' tried to unregister '{}' which it does not own or wasn’t found.",
                    owner.getName(), name
            );
            return;
        }
        if (removeNode(name)) {
            owned.remove(name);
            if (owned.isEmpty()) scriptCommands.remove(owner);
            CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.getActiveDispatcher();
            if (dispatcher != null) pushCommandTree(dispatcher);
        }
    }

    public void unregisterAllFor(RunningScript owner) {
        Set<String> owned = scriptCommands.remove(owner);
        if (owned == null) return;

        boolean changed = false;
        for (String name : owned) {
            if (removeNode(name)) {
                Main.LOGGER.info("Unregistered '{}' from disabled script '{}'", name, owner.getName());
                changed = true;
            }
        }
        if (changed) {
            CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.getActiveDispatcher();
            if (dispatcher != null) pushCommandTree(dispatcher);
        }
    }

    private boolean removeNode(String name) {
        CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.getActiveDispatcher();
        if (dispatcher == null) return false;

        RootCommandNode<FabricClientCommandSource> root = dispatcher.getRoot();

        @SuppressWarnings("unchecked")
        CommandNodeAccessor<FabricClientCommandSource> accessor = (CommandNodeAccessor<FabricClientCommandSource>) root;

        Map<String, CommandNode<FabricClientCommandSource>> children = accessor.getChildrenMap();
        Map<String, LiteralCommandNode<FabricClientCommandSource>> literals = accessor.getLiteralsMap();

        boolean removed = children.remove(name) != null;
        if (removed) {
            literals.remove(name);
        }
        return removed;
    }

    private void pushCommandTree(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        RootCommandNode<FabricClientCommandSource> clientRoot = dispatcher.getRoot();

        @SuppressWarnings("unchecked")
        RootCommandNode<CommandSource> vanillaRoot = (RootCommandNode<CommandSource>) (Object) clientRoot;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != null && client.player != null) {
            client.getNetworkHandler().onCommandTree(new CommandTreeS2CPacket(vanillaRoot));
        }
    }
}
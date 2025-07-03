package net.me.utils;

import net.me.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

import java.util.Optional;

@SuppressWarnings("unused")
public final class McUtils {

    private McUtils() {
    }

    public static Optional<MinecraftClient> getMc() {
        return Optional.ofNullable(MinecraftClient.getInstance());
    }

    public static Optional<ClientPlayerEntity> getPlayer() {
        return getMc().flatMap(mc -> {
            ClientPlayerEntity player = mc.player;
            if (player == null) {
                Main.LOGGER.warn("Cannot send chat: player is null");
                return Optional.empty();
            }
            return Optional.of(player);
        });
    }

    public static Optional<ClientWorld> getWorld() {
        return getMc().flatMap(mc -> {
            ClientWorld world = mc.world;
            if (world == null) {
                Main.LOGGER.warn("Cannot get world: world is null");
                return Optional.empty();
            }
            return Optional.of(world);
        });
    }
}

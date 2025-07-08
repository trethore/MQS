package net.me.utils;

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
        return getMc().map(mc -> mc.player);

    }

    public static Optional<ClientWorld> getWorld() {
        return getMc().map(mc -> mc.world);
    }
}

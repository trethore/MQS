package net.me.event.events.player;

import net.me.event.Event;
import net.me.event.Events;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;

@SuppressWarnings("unused")
public class PlayerRespawnEvent extends Event {
    private final ClientPlayerEntity player;

    public PlayerRespawnEvent(PlayerRespawnS2CPacket packet, ClientPlayerEntity player) {
        this.player = player;
    }

    public ClientPlayerEntity getPlayer() {
        return player;
    }

    @Override
    public Events getType() {
        return Events.PlayerRespawnEvent;
    }
}
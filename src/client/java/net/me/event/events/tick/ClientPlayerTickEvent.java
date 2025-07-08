package net.me.event.events.tick;

import net.me.event.Event;
import net.me.event.Events;
import net.minecraft.client.network.ClientPlayerEntity;


public class ClientPlayerTickEvent extends Event {
    private final ClientPlayerEntity player;

    public ClientPlayerTickEvent(ClientPlayerEntity player) {
        this.player = player;
    }

    public ClientPlayerEntity getPlayer() {
        return this.player;
    }

    @Override
    public Events getType() {
        return Events.ClientPlayerTickEvent;
    }
}
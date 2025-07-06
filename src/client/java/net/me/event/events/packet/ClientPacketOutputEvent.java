package net.me.event.events.packet;

import net.me.event.Event;
import net.me.event.EventManager;
import net.minecraft.network.packet.Packet;

public class ClientPacketOutputEvent extends Event {
    private final Packet<?> packet;

    public ClientPacketOutputEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    @Override
    public EventManager.Events getType() {
        return EventManager.Events.ClientPacketOutputEvent;
    }
}

package net.me.event.events.packet;

import net.me.event.Event;
import net.me.event.EventManager;
import net.minecraft.network.packet.Packet;

public class ClientPacketInputEvent extends Event {
    private final Packet<?> packet;

    public ClientPacketInputEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    @Override
    public EventManager.Events getType() {
        return EventManager.Events.ClientPacketInputEvent;
    }
}

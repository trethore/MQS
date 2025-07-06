package net.me.event.events.packet;

import net.me.event.Events;
import net.me.event.PacketEvent;
import net.minecraft.network.packet.Packet;

public class ClientPacketOutputEvent extends PacketEvent<Packet<?>> {

    public ClientPacketOutputEvent(Packet<?> packet) {
        super(packet);
    }

    @Override
    public Events getType() {
        return Events.ClientPacketOutputEvent;
    }
}
package net.me.event.events.packet;

import net.me.event.Events;
import net.me.event.PacketEvent;
import net.minecraft.network.packet.Packet;

public class ClientPacketInputEvent extends PacketEvent<Packet<?>> {

    public ClientPacketInputEvent(Packet<?> packet) {
        super(packet);
    }

    @Override
    public Events getType() {
        return Events.ClientPacketInputEvent;
    }
}
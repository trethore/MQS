package net.me.event;

import net.minecraft.network.packet.Packet;

public abstract class PacketEvent<T extends Packet<?>> extends Event {
    private final T packet;

    public PacketEvent(T packet) {
        this.packet = packet;
    }

    /**
     * Gets the specific packet associated with this event.
     *
     * @return The packet of type T.
     */
    public T getPacket() {
        return this.packet;
    }
}
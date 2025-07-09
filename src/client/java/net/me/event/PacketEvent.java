package net.me.event;

import net.minecraft.network.packet.Packet;

public abstract class PacketEvent<T extends Packet<?>> extends CancellableEvent {
    private final T packet;

    public PacketEvent(T packet) {
        this.packet = packet;
    }

    public T getPacket() {
        return this.packet;
    }
}
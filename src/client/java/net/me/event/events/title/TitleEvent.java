package net.me.event.events.title;

import net.me.event.Events;
import net.me.event.PacketEvent;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;

public class TitleEvent extends PacketEvent<TitleS2CPacket> {

    public TitleEvent(TitleS2CPacket packet) {
        super(packet);
    }

    @Override
    public Events getType() {
        return Events.TitleEvent;
    }
}
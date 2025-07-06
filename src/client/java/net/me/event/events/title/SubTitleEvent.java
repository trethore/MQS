package net.me.event.events.title;

import net.me.event.Events;
import net.me.event.PacketEvent;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;

public class SubTitleEvent extends PacketEvent<SubtitleS2CPacket> {
    public SubTitleEvent(SubtitleS2CPacket packet) {
        super(packet);
    }

    @Override
    public Events getType() {
        return Events.SubtitleEvent;
    }
}

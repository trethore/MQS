package net.me.event;

import net.me.event.events.MinecraftClientStopEvent;
import net.me.event.events.packet.ClientPacketInputEvent;
import net.me.event.events.packet.ClientPacketOutputEvent;
import net.me.event.events.tick.EndClientTickEvent;
import net.me.event.events.tick.StartClientTickEvent;
import net.me.event.events.title.SubTitleEvent;
import net.me.event.events.title.TitleEvent;

public enum Events {
    StartClientTickEvent(StartClientTickEvent.class),
    EndClientTickEvent(EndClientTickEvent.class),
    MinecraftClientStopEvent(MinecraftClientStopEvent.class),
    ClientPacketOutputEvent(ClientPacketOutputEvent.class),
    ClientPacketInputEvent(ClientPacketInputEvent.class),
    TitleEvent(TitleEvent.class),
    SubtitleEvent(SubTitleEvent.class);

    private final Class<? extends Event> eventClass;

    Events(Class<? extends Event> eventClass) {
        this.eventClass = eventClass;
    }

    public Class<? extends Event> getEventClass() {
        return eventClass;
    }
}
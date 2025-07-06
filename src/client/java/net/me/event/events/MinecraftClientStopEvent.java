package net.me.event.events;

import net.me.event.Event;
import net.me.event.Events;

public class MinecraftClientStopEvent extends Event {
    @Override
    public Events getType() {
        return Events.MinecraftClientStopEvent;
    }
}

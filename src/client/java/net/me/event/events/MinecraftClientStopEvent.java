package net.me.event.events;

import net.me.event.Event;
import net.me.event.EventManager;

public class MinecraftClientStopEvent extends Event {
    @Override
    public EventManager.Events getType() {
        return EventManager.Events.MinecraftClientStopEvent;
    }
}

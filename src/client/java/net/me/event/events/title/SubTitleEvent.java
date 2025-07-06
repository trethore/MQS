package net.me.event.events.title;

import net.me.event.Event;
import net.me.event.EventManager;

public class SubTitleEvent extends Event {
    @Override
    public EventManager.Events getType() {
        return EventManager.Events.SubtitleEvent;
    }
}

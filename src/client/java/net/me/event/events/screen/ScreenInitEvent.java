package net.me.event.events.screen;

import net.me.event.Events;
import net.minecraft.client.gui.screen.Screen;

public class ScreenInitEvent extends ScreenEvent {
    public ScreenInitEvent(Screen screen) {
        super(screen);
    }

    @Override
    public Events getType() {
        return Events.ScreenInitEvent;
    }
}
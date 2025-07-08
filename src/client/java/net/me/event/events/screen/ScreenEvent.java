package net.me.event.events.screen;

import net.me.event.Event;
import net.minecraft.client.gui.screen.Screen;

public abstract class ScreenEvent extends Event {
    private final Screen screen;

    public ScreenEvent(Screen screen) {
        this.screen = screen;
    }

    public Screen getScreen() {
        return screen;
    }
}
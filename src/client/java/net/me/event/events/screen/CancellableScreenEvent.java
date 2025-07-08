package net.me.event.events.screen;

import net.me.event.CancellableEvent;
import net.minecraft.client.gui.screen.Screen;

public abstract class CancellableScreenEvent extends CancellableEvent {
    private final Screen screen;

    public CancellableScreenEvent(Screen screen) {
        this.screen = screen;
    }

    public Screen getScreen() {
        return screen;
    }
}
package net.me.event.events.screen;

import net.me.event.Events;
import net.minecraft.client.gui.screen.TitleScreen;

public class TitleScreenInitEvent extends ScreenInitEvent {

    public TitleScreenInitEvent(TitleScreen screen) {
        super(screen);
    }

    @Override
    public TitleScreen getScreen() {
        return (TitleScreen) super.getScreen();
    }

    @Override
    public Events getType() {
        return Events.TitleScreenInitEvent;
    }
}

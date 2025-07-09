package net.me.event.events.screen;

import net.me.event.Events;
import net.minecraft.client.gui.screen.DeathScreen;

public class DeathScreenInitEvent extends ScreenInitEvent {

    public DeathScreenInitEvent(DeathScreen screen) {
        super(screen);
    }

    @Override
    public DeathScreen getScreen() {
        return (DeathScreen) super.getScreen();
    }

    @Override
    public Events getType() {
        return Events.DeathScreenInitEvent;
    }
}

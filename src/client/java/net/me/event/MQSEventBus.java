package net.me.event;

import net.me.Main;

public class MQSEventBus {

    private static EventManager eventManagerInstance;

    private MQSEventBus() {
    }

    public static void setManager(EventManager manager) {
        eventManagerInstance = manager;
    }

    public static void post(Event event) {
        if (eventManagerInstance != null) {
            eventManagerInstance.post(event);
        } else {
            Main.LOGGER.warn("GlobalEventBus.post called before EventManager was set!");
        }
    }
}

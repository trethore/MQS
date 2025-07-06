package net.me.event.events.tick;

import net.me.event.Event;
import net.me.event.EventManager;
import net.minecraft.client.MinecraftClient;

public class StartClientTickEvent extends Event {
    private final MinecraftClient client;

    public StartClientTickEvent(MinecraftClient client) {
        this.client = client;
    }

    public MinecraftClient getClient() {
        return this.client;
    }

    public EventManager.Events getType() {
        return EventManager.Events.StartClientTickEvent;
    }
}

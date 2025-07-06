package net.me.event.events.tick;

import net.me.event.Event;
import net.me.event.EventManager;
import net.minecraft.client.MinecraftClient;

public class EndClientTickEvent extends Event {
    private final MinecraftClient client;

    public EndClientTickEvent(MinecraftClient client) {
        this.client = client;
    }

    public MinecraftClient getClient() {
        return this.client;
    }
    public EventManager.Events getType() {
        return EventManager.Events.StartClientTickEvent;
    }
}
package net.me.event.events.world;

import net.me.event.Event;
import net.me.event.Events;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class WorldChangeEvent extends Event {
    private final ClientWorld world;

    public WorldChangeEvent(@Nullable ClientWorld world) {
        this.world = world;
    }

    @Nullable
    public ClientWorld getWorld() {
        return world;
    }

    @Override
    public Events getType() {
        return Events.WorldChangeEvent;
    }
}
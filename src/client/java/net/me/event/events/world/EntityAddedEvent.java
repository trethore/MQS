package net.me.event.events.world;

import net.me.event.Event;
import net.me.event.Events;
import net.minecraft.entity.Entity;


public class EntityAddedEvent extends Event {
    private final Entity entity;

    public EntityAddedEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public Events getType() {
        return Events.EntityAddedEvent;
    }
}
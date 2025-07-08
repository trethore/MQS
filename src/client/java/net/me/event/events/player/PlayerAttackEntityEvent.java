package net.me.event.events.player;

import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

@SuppressWarnings("unused")
public class PlayerAttackEntityEvent extends CancellableEvent {
    private final PlayerEntity attacker;
    private Entity target;

    public PlayerAttackEntityEvent(PlayerEntity attacker, Entity target) {
        this.attacker = attacker;
        this.target = target;
    }

    public PlayerEntity getAttacker() {
        return attacker;
    }

    public Entity getTarget() {
        return target;
    }

    public void setTarget(Entity target) {
        this.target = target;
    }

    @Override
    public Events getType() {
        return Events.PlayerAttackEntityEvent;
    }
}
package net.me.event.events.player;

import net.me.event.Event;
import net.me.event.Events;
import net.minecraft.entity.damage.DamageSource;

@SuppressWarnings("unused")
public class PlayerDamageEvent extends Event {
    private final DamageSource source;
    private final float amount;

    public PlayerDamageEvent(DamageSource source, float amount) {
        this.source = source;
        this.amount = amount;
    }

    public DamageSource getSource() {
        return source;
    }

    public float getAmount() {
        return amount;
    }

    @Override
    public Events getType() {
        return Events.PlayerDamageEvent;
    }
}
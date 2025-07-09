package net.me.event.events.player;

import net.me.event.Event;
import net.me.event.Events;
import net.minecraft.entity.damage.DamageSource;

@SuppressWarnings("unused")
public class PlayerDamageEvent extends Event {
    private DamageSource source;
    private float amount;

    public PlayerDamageEvent(DamageSource source, float amount) {
        this.source = source;
        this.amount = amount;
    }

    public DamageSource getSource() {
        return source;
    }

    public void setSource(DamageSource source) {
        this.source = source;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    @Override
    public Events getType() {
        return Events.PlayerDamageEvent;
    }
}
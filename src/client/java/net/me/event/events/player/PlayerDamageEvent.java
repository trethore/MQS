package net.me.event.events.player;

import net.me.event.Event;
import net.me.event.Events;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;

@SuppressWarnings("unused")
public class PlayerDamageEvent extends Event {
    private final ServerWorld wold;
    private DamageSource source;
    private float amount;

    public PlayerDamageEvent(ServerWorld wold, DamageSource source, float amount) {
        this.wold = wold;
        this.source = source;
        this.amount = amount;
    }

    public ServerWorld getWorld() {
        return wold;
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
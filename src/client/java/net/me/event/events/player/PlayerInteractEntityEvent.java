package net.me.event.events.player;

import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

@SuppressWarnings("unused")
public class PlayerInteractEntityEvent extends CancellableEvent {
    private final PlayerEntity player;
    private final Entity target;
    private final Hand hand;

    public PlayerInteractEntityEvent(PlayerEntity player, Entity target, Hand hand) {
        this.player = player;
        this.target = target;
        this.hand = hand;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public Entity getTarget() {
        return target;
    }

    public Hand getHand() {
        return hand;
    }

    @Override
    public Events getType() {
        return Events.PlayerInteractEntityEvent;
    }
}
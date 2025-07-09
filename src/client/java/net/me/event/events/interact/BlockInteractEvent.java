package net.me.event.events.interact;

import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

@SuppressWarnings("unused")
public class BlockInteractEvent extends CancellableEvent {
    private final PlayerEntity player;
    private final Hand hand;
    private final BlockHitResult hitResult;

    public BlockInteractEvent(PlayerEntity player, Hand hand, BlockHitResult hitResult) {
        this.player = player;
        this.hand = hand;
        this.hitResult = hitResult;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public Hand getHand() {
        return hand;
    }

    public BlockHitResult getHitResult() {
        return hitResult;
    }

    @Override
    public Events getType() {
        return Events.BlockInteractEvent;
    }
}
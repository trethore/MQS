package net.me.event.events.interact;

import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

@SuppressWarnings("unused")
public class ItemUseEvent extends CancellableEvent {
    private final PlayerEntity player;
    private Hand hand;
    private ItemStack itemStack;

    public ItemUseEvent(PlayerEntity player, Hand hand) {
        this.player = player;
        this.hand = hand;
        this.itemStack = player.getStackInHand(hand);
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public Hand getHand() {
        return hand;
    }

    public void setHand(Hand hand) {
        this.hand = hand;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public Events getType() {
        return Events.ItemUseEvent;
    }
}
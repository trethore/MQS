package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.interact.BlockInteractEvent;
import net.me.event.events.interact.ItemUseEvent;
import net.me.event.events.player.PlayerAttackEntityEvent;
import net.me.event.events.player.PlayerInteractEntityEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ClientPlayerInteractionManager.class)
public class MQSClientPlayerInteractionManagerMixin {

    @Unique
    private final ThreadLocal<BlockInteractEvent> currentBlockInteractEvent = new ThreadLocal<>();
    @Unique
    private final ThreadLocal<ItemUseEvent> currentItemUseEvent = new ThreadLocal<>();
    @Unique
    private final ThreadLocal<PlayerAttackEntityEvent> currentAttackEvent = new ThreadLocal<>();
    @Unique
    private final ThreadLocal<PlayerInteractEntityEvent> currentInteractEntityEvent = new ThreadLocal<>();


    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        BlockInteractEvent event = new BlockInteractEvent(player, hand, hitResult);
        Main.getInstance().getEventManager().post(event);

        if (event.isCancelled()) {
            cir.setReturnValue(ActionResult.FAIL);
        } else {
            currentBlockInteractEvent.set(event);
        }
    }

    @ModifyArgs(method = "interactBlock", at = @At("HEAD"))
    private void modifyInteractBlockArgs(Args args) {
        BlockInteractEvent event = currentBlockInteractEvent.get();
        currentBlockInteractEvent.remove();
        if (event != null) {
            args.setAll(args.get(0), event.getHand(), event.getHitResult());
        }
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void onInteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemUseEvent event = new ItemUseEvent(player, hand);
        Main.getInstance().getEventManager().post(event);

        if (event.isCancelled()) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @ModifyArgs(method = "interactItem", at = @At("HEAD"))
    private void modifyInteractItemArgs(Args args) {
        ItemUseEvent event = currentItemUseEvent.get();
        currentItemUseEvent.remove();
        if (event != null) {
            args.setAll(args.get(0), event.getHand());
        }
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        PlayerAttackEntityEvent event = new PlayerAttackEntityEvent(player, target);
        Main.getInstance().getEventManager().post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @ModifyArgs(method = "attackEntity", at = @At("HEAD"))
    private void modifyAttackEntityArgs(Args args) {
        PlayerAttackEntityEvent event = currentAttackEvent.get();
        currentAttackEvent.remove();
        if (event != null) {
            args.setAll(args.get(0), event.getTarget());
        }
    }

    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void onInteractEntity(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(player, entity, hand);
        Main.getInstance().getEventManager().post(event);

        if (event.isCancelled()) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @ModifyArgs(method = "interactEntity", at = @At("HEAD"))
    private void modifyInteractEntityArgs(Args args) {
        PlayerInteractEntityEvent event = currentInteractEntityEvent.get();
        currentInteractEntityEvent.remove();
        if (event != null) {
            args.setAll(args.get(0), event.getTarget(), event.getHand());
        }
    }
}

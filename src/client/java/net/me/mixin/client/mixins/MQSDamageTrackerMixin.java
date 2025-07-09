package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.player.PlayerDamageEvent;
import net.minecraft.entity.damage.DamageTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(DamageTracker.class)
public class MQSDamageTrackerMixin {

    @ModifyArgs(method = "onDamage", at = @At("HEAD"))
    private void onOnDamage(Args args) {
        PlayerDamageEvent event = new PlayerDamageEvent(args.get(0), args.get(1));
        Main.getInstance().getEventManager().post(event);
        args.setAll(event.getSource(), event.getAmount());

    }
}

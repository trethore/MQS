package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.player.PlayerDamageEvent;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DamageTracker.class)
public class MQSDamageTrackerMixin {

    @Inject(method = "onDamage", at = @At("HEAD"))
    private void onOnDamage(DamageSource damageSource, float damage, CallbackInfo ci) {
        Main.getInstance().getEventManager().post(new PlayerDamageEvent(damageSource, damage));
    }
}

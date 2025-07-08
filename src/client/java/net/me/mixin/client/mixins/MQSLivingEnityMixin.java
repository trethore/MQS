package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.player.PlayerDamageEvent;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LivingEntity.class)
public class MQSLivingEnityMixin {

    @ModifyArgs(method = "damage", at = @At("HEAD"))
    private void onDamage(Args args) {
        PlayerDamageEvent event = new PlayerDamageEvent(args.get(0), args.get(1), args.get(2));
        Main.getInstance().getEventManager().post(event);
        args.setAll(args.get(0), event.getSource(), event.getAmount());
    }
}

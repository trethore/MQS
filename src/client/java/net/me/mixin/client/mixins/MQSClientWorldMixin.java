package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.world.EntityAddedEvent;
import net.me.event.events.world.EntityRemovedEvent;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class MQSClientWorldMixin {

    @Shadow
    public abstract Entity getEntityById(int id);

    @Inject(method = "addEntity", at = @At("TAIL"))
    private void onAddEntity(Entity entity, CallbackInfo info) {
        Main.getInstance().getEventManager().post(new EntityAddedEvent(entity));
    }

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void onRemoveEntity(int entityId, Entity.RemovalReason removalReason, CallbackInfo info) {
        Entity entity = this.getEntityById(entityId);
        Main.getInstance().getEventManager().post(new EntityRemovedEvent(entity));
    }
}
package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.render.EntityRenderEvent;
import net.me.event.events.render.NameTagRenderEvent;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EntityRenderer.class)
public class MQSEntityRendererMixin<S extends EntityRenderState> {

    @Unique
    private final ThreadLocal<NameTagRenderEvent<S>> currentNameTagEvent = new ThreadLocal<>();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderPre(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        EntityRenderEvent.Pre<S> event = new EntityRenderEvent.Pre<>(state, matrices, vertexConsumers, light);
        Main.getInstance().getEventManager().post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderPost(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        EntityRenderEvent.Post<S> event = new EntityRenderEvent.Post<>(state, matrices, vertexConsumers, light);
        Main.getInstance().getEventManager().post(event);
    }

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void onRenderLabelHead(S state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        NameTagRenderEvent<S> event = new NameTagRenderEvent<>(state, text, matrices, vertexConsumers, light);
        Main.getInstance().getEventManager().post(event);
        if (event.isCancelled()) {
            ci.cancel();
        } else {
            currentNameTagEvent.set(event);
        }
    }

    @ModifyArgs(method = "renderLabelIfPresent", at = @At(value = "HEAD"))
    private void onRenderLabelModifyText(Args args) {
        NameTagRenderEvent<S> event = currentNameTagEvent.get();
        currentNameTagEvent.remove();
        if (event == null) {
            return;
        }
        args.setAll(event.getEntityState(), event.getText(), event.getMatrices(), event.getVertexConsumers(), event.getLight());
    }
}
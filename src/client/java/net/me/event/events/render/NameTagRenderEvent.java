package net.me.event.events.render;

import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

@SuppressWarnings("unused")
public class NameTagRenderEvent<S extends EntityRenderState> extends CancellableEvent {
    private final S entityState;
    private final MatrixStack matrices;
    private final VertexConsumerProvider vertexConsumers;
    private final int light;
    private final Text text;

    public NameTagRenderEvent(S entityState, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        this.entityState = entityState;
        this.matrices = matrices;
        this.vertexConsumers = vertexConsumers;
        this.light = light;
        this.text = text;
    }

    public S getEntityState() {
        return entityState;
    }

    public Text getText() {
        return text;
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public VertexConsumerProvider getVertexConsumers() {
        return vertexConsumers;
    }

    public int getLight() {
        return light;
    }

    @Override
    public Events getType() {
        return Events.NameTagRenderEvent;
    }
}
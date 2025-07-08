package net.me.event.events.render;

import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

@SuppressWarnings("unused")
public class NameTagRenderEvent<S extends EntityRenderState> extends CancellableEvent {
    private S entityState;
    private MatrixStack matrices;
    private VertexConsumerProvider vertexConsumers;
    private int light;
    private Text text;

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

    public void setEntityState(S entityState) {
        this.entityState = entityState;
    }

    public Text getText() {
        return text;
    }

    public void setText(Text text) {
        this.text = text;
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public void setMatrices(MatrixStack matrices) {
        this.matrices = matrices;
    }

    public VertexConsumerProvider getVertexConsumers() {
        return vertexConsumers;
    }

    public void setVertexConsumers(VertexConsumerProvider vertexConsumers) {
        this.vertexConsumers = vertexConsumers;
    }

    public int getLight() {
        return light;
    }

    public void setLight(int light) {
        this.light = light;
    }

    @Override
    public Events getType() {
        return Events.NameTagRenderEvent;
    }
}
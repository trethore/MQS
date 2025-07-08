package net.me.event.events.render;

import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

@SuppressWarnings("unused")
public abstract class EntityRenderEvent<T extends EntityRenderState> extends CancellableEvent {
    private final T entity;
    private final MatrixStack matrices;
    private final VertexConsumerProvider vertexConsumers;
    private final int light;

    public EntityRenderEvent(T entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        this.entity = entity;
        this.matrices = matrices;
        this.vertexConsumers = vertexConsumers;
        this.light = light;
    }

    public T getEntity() {
        return entity;
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

    public static class Pre<T extends EntityRenderState> extends EntityRenderEvent<T> {

        public Pre(T entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
            super(entity, matrices, vertexConsumers, light);
        }

        @Override
        public Events getType() {
            return Events.EntityRenderEventPre;
        }
    }

    public static class Post<T extends EntityRenderState> extends EntityRenderEvent<T> {

        public Post(T entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
            super(entity, matrices, vertexConsumers, light);
        }

        @Override
        public Events getType() {
            return Events.EntityRenderEventPost;
        }
    }
}
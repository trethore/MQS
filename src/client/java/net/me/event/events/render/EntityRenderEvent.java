/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.me.event.events.render;

import lombok.Getter;
import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

@Getter
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
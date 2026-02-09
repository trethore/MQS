/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Réthoré
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

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

@Getter
@SuppressWarnings("unused")
public abstract class EntityRenderEvent<T extends EntityRenderState> extends CancellableEvent {
    private final T entity;
    private final PoseStack matrices;
    private final SubmitNodeCollector vertexConsumers;
    private final int light;

    protected EntityRenderEvent(T entity, PoseStack matrices, SubmitNodeCollector vertexConsumers, int light) {
        this.entity = entity;
        this.matrices = matrices;
        this.vertexConsumers = vertexConsumers;
        this.light = light;
    }

    public static class Pre<T extends EntityRenderState> extends EntityRenderEvent<T> {

        public Pre(T entity, PoseStack matrices, SubmitNodeCollector vertexConsumers, int light) {
            super(entity, matrices, vertexConsumers, light);
        }

        @Override
        public Events getType() {
            return Events.ENTITY_RENDER_EVENT_PRE;
        }
    }

    public static class Post<T extends EntityRenderState> extends EntityRenderEvent<T> {

        public Post(T entity, PoseStack matrices, SubmitNodeCollector vertexConsumers, int light) {
            super(entity, matrices, vertexConsumers, light);
        }

        @Override
        public Events getType() {
            return Events.ENTITY_RENDER_EVENT_POST;
        }
    }
}

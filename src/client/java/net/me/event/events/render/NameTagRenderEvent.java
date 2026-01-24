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

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;

@Getter
@SuppressWarnings("unused")
public class NameTagRenderEvent<S extends EntityRenderState> extends CancellableEvent {
    private final S entityState;
    private final PoseStack matrices;
    private final SubmitNodeCollector vertexConsumers;
    private final int light;
    private final Component text;

    public NameTagRenderEvent(S entityState, Component text, PoseStack matrices, SubmitNodeCollector vertexConsumers, int light) {
        this.entityState = entityState;
        this.matrices = matrices;
        this.vertexConsumers = vertexConsumers;
        this.light = light;
        this.text = text;
    }

    @Override
    public Events getType() {
        return Events.NAME_TAG_RENDER_EVENT;
    }
}

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

import net.me.event.Event;
import net.me.event.Events;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;

@SuppressWarnings("unused")
public class WorldRenderEvent extends Event {
    private final DrawContext drawContext;
    private final RenderTickCounter tickCounter;
    private final Camera camera;

    public WorldRenderEvent(DrawContext drawContext, RenderTickCounter tickCounter, Camera camera) {
        this.drawContext = drawContext;
        this.tickCounter = tickCounter;
        this.camera = camera;
    }

    public DrawContext getDrawContext() {
        return drawContext;
    }

    public MatrixStack getMatrices() {
        return drawContext.getMatrices();
    }

    public float getPartialTicks() {
        return tickCounter.getTickDelta(true);
    }

    public Camera getCamera() {
        return camera;
    }

    @Override
    public Events getType() {
        return Events.WorldRenderEvent;
    }
}
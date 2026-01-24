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
import net.me.event.Event;
import net.me.event.Events;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

@Getter
@SuppressWarnings("unused")
public class HudRenderEvent extends Event {
    private final GuiGraphics drawContext;
    private final DeltaTracker tickCounter;

    public HudRenderEvent(GuiGraphics drawContext, DeltaTracker tickCounter) {
        this.drawContext = drawContext;
        this.tickCounter = tickCounter;
    }

    public float getPartialTicks() {
        return tickCounter.getGameTimeDeltaPartialTick(true);
    }

    @Override
    public Events getType() {
        return Events.HUD_RENDER_EVENT;
    }
}

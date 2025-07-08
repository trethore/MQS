package net.me.event.events.render;

import net.me.event.Event;
import net.me.event.Events;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

@SuppressWarnings("unused")
public class HudRenderEvent extends Event {
    private final DrawContext drawContext;
    private final RenderTickCounter tickCounter;

    public HudRenderEvent(DrawContext drawContext, RenderTickCounter tickCounter) {
        this.drawContext = drawContext;
        this.tickCounter = tickCounter;
    }

    public DrawContext getDrawContext() {
        return drawContext;
    }

    public float getPartialTicks() {
        return tickCounter.getTickDelta(true);
    }

    public RenderTickCounter getTickCounter() {
        return tickCounter;
    }

    @Override
    public Events getType() {
        return Events.HudRenderEvent;
    }
}
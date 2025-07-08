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
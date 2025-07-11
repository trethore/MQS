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

package net.me.screen.component.components;

import net.me.utils.GUIColors;
import net.me.utils.Render2DUtils;
import net.me.utils.TextRenderUtils;
import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MQSToast {
    private static final List<MQSToast> toasts = new CopyOnWriteArrayList<>();
    private static final int TOAST_WIDTH = 180;
    private static final int TOAST_HEIGHT = 36;
    private static final int PADDING = 5;
    private static final float ANIMATION_SPEED = 0.15f;

    private final String title;
    private final String description;
    private final long timeToLive;
    private final Corner corner;
    private final long creationTime;

    private float x, y;
    private boolean isInitialized = false;
    private boolean isLeaving = false;

    private MQSToast(String title, String description, long timeToLive, Corner corner) {
        this.title = title;
        this.description = description;
        this.timeToLive = timeToLive;
        this.corner = corner;
        this.creationTime = System.currentTimeMillis();
    }

    public static void show(String title, String description, long timeToLive, Corner corner) {
        toasts.add(new MQSToast(title, description, timeToLive, corner));
    }

    public static void updateAll() {
        if (toasts.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        for (MQSToast toast : toasts) {
            if (!toast.isLeaving && (now - toast.creationTime) > toast.timeToLive) {
                toast.isLeaving = true;
            }
        }
    }

    public static void renderAll(DrawContext context) {
        if (toasts.isEmpty()) {
            return;
        }

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        for (Corner c : Corner.values()) {
            float cornerY = 0;
            if (c == Corner.TOP_LEFT || c == Corner.TOP_RIGHT) {
                cornerY = PADDING;
            } else if (c == Corner.BOTTOM_LEFT || c == Corner.BOTTOM_RIGHT) {
                cornerY = screenHeight - TOAST_HEIGHT - PADDING;
            }

            List<MQSToast> cornerToasts = toasts.stream().filter(t -> t.corner == c).toList();
            for (int i = 0; i < cornerToasts.size(); i++) {
                MQSToast toast = cornerToasts.get(i);
                float targetY;
                if (c == Corner.TOP_LEFT || c == Corner.TOP_RIGHT) {
                    targetY = cornerY + (i * (TOAST_HEIGHT + PADDING));
                } else {
                    targetY = cornerY - (i * (TOAST_HEIGHT + PADDING));
                }
                toast.render(context, screenWidth, targetY);
            }
        }
    }

    private void render(DrawContext context, int screenWidth, float targetY) {
        if (!isInitialized) {
            this.x = corner.getStartX(screenWidth);
            this.y = targetY;
            this.isInitialized = true;
        }

        float targetX = isLeaving ? corner.getLeaveX(screenWidth) : corner.getAnimateInX(screenWidth);
        this.x += (targetX - this.x) * ANIMATION_SPEED;
        this.y += (targetY - this.y) * ANIMATION_SPEED;

        if (isLeaving && Math.abs(this.x - targetX) < 1) {
            toasts.remove(this);
            return;
        }

        Render2DUtils.drawRoundedRect(context, x, y, TOAST_WIDTH, TOAST_HEIGHT, 5, 10, GUIColors.DARK_L1.getRGB());
        Render2DUtils.drawRoundedOutline(context, x, y, TOAST_WIDTH, TOAST_HEIGHT, 5, 1, 10, GUIColors.DARK_L3.getRGB());

        TextRenderUtils.drawCustomText(context, title, x + PADDING, y + 7, GUIColors.TEXT.getRGB(), true, 1f);
        TextRenderUtils.drawCustomText(context, description, x + PADDING, y + 20, GUIColors.TEXT_DISABLED.getRGB(), true, 0.8f);

        if (!isLeaving) {
            float progress = 1.0f - (float) (System.currentTimeMillis() - creationTime) / timeToLive;
            progress = Math.max(0, progress);
            float progressWidth = (TOAST_WIDTH - 10) * progress;

            Render2DUtils.drawRoundedRect(context, x + 5, y + TOAST_HEIGHT - 6, TOAST_WIDTH - 10, 2, 3, 5, GUIColors.DARK_L4.getRGB());
            Render2DUtils.drawRoundedRect(context, x + 5, y + TOAST_HEIGHT - 6, progressWidth, 2, 3, 5, GUIColors.SECONDARY.getRGB());
        }
    }


    public enum Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        public int getStartX(int screenWidth) {
            return switch (this) {
                case TOP_LEFT, BOTTOM_LEFT -> -TOAST_WIDTH;
                case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth;
            };
        }

        public int getAnimateInX(int screenWidth) {
            return switch (this) {
                case TOP_LEFT, BOTTOM_LEFT -> PADDING;
                case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - TOAST_WIDTH - PADDING;
            };
        }

        public int getLeaveX(int screenWidth) {
            return switch (this) {
                case TOP_LEFT, BOTTOM_LEFT -> -TOAST_WIDTH - PADDING;
                case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth + PADDING;
            };
        }
    }
}

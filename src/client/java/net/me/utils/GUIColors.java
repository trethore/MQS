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

package net.me.utils;

import java.awt.*;

@SuppressWarnings("unused")
public enum GUIColors {
    // basics
    BLACK(new Color(0, 0, 0, 255)),
    WHITE(new Color(255, 255, 255, 255)),
    // text
    TEXT(new Color(250, 250, 250, 255)),
    TEXT_DISABLED(new Color(151, 151, 151, 240)),
    // default
    SUCCESS(new Color(33, 199, 33, 255)),
    WARN(new Color(255, 204, 0, 255)),
    ERROR(new Color(204, 51, 0, 255)),
    // gradients
    DARK_L1(new Color(23, 23, 23, 255)),
    DARK_L2(new Color(33, 33, 33, 255)),
    DARK_L3(new Color(48, 48, 48, 255)),
    DARK_L4(new Color(69, 69, 69, 255)),
    // ui color
    PRIMARY(new Color(96, 255, 255, 255)),
    SECONDARY(new Color(255, 96, 96, 255));

    private final Color color;

    GUIColors(Color color) {
        this.color = color;
    }

    public Color darker(Level level) {
        return darker(level.getPercentage());
    }

    public Color lighter(Level level) {
        return lighter(level.getPercentage());
    }

    public Color darker(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100.");
        }
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hsb[2] = Math.max(0f, hsb[2] - (percentage / 100f));
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return new Color(r, g, b, color.getAlpha());
    }

    public Color lighter(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100.");
        }
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hsb[2] = Math.min(1f, hsb[2] + (percentage / 100f));
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return new Color(r, g, b, color.getAlpha());
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public int getRGB() {
        return this.color.getRGB();
    }

    public Color getColor() {
        return color;
    }

    public int getRed() {
        return color.getRed();
    }

    public int getGreen() {
        return color.getGreen();
    }

    public int getBlue() {
        return color.getBlue();
    }

    public int getAlpha() {
        return color.getAlpha();
    }

    public enum Level {
        LOW(10),
        MEDIUM(20),
        HIGH(30);

        private final int percentage;

        Level(int percentage) {
            this.percentage = percentage;
        }

        public int getPercentage() {
            return percentage;
        }
    }
}

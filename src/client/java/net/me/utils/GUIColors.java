package net.me.utils;

import java.awt.*;

public enum GUIColors {
    BLACK(0, 0, 0, 255),
    DARK_L1(23, 23, 23, 255),
    DARK_L2(33, 33, 33, 255),
    DARK_L3(48, 48, 48, 255),
    DARK_L4(69, 69, 69, 255),
    BG_DARK(51, 51, 51, 255),
    TEXT_GREY_DISABLED(151, 151, 151, 255),
    WHITE(255, 255, 255, 255),
    GREEN(0, 255, 0, 255),
    SUCCESS(33, 199, 33, 255),
    RED(255, 0, 0, 255),
    ERROR(173, 14, 14, 255);

    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;

    GUIColors(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;

    }

    public int getRGBA() {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public Color darker(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100.");
        }
        double factor = 1.0 - (percentage / 100.0);

        int newRed = (int) (this.red * factor);
        int newGreen = (int) (this.green * factor);
        int newBlue = (int) (this.blue * factor);

        return new Color(clamp(newRed), clamp(newGreen), clamp(newBlue), this.alpha);
    }

    public Color lighter(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100.");
        }
        double factor = percentage / 100.0;

        int newRed = (int) (this.red + (255 - this.red) * factor);
        int newGreen = (int) (this.green + (255 - this.green) * factor);
        int newBlue = (int) (this.blue + (255 - this.blue) * factor);

        return new Color(clamp(newRed), clamp(newGreen), clamp(newBlue), this.alpha);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public Color getColor() {
        return new Color(red, green, blue, alpha);
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public int getAlpha() {
        return alpha;
    }
}

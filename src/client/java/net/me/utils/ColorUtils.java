package net.me.utils;

import java.awt.*;

@SuppressWarnings("unused")
public final class ColorUtils {
    private ColorUtils() {}

    public static int getRainbowColor(long speed, float saturation, float brightness) {
        return getRainbowColor(0, speed, saturation, brightness);
    }

    public static int getRainbowColor(long offset, long speed, float saturation, float brightness) {
        float hue = ((System.currentTimeMillis() + offset) % speed) / (float) speed;
        return Color.HSBtoRGB(hue, saturation, brightness);
    }
}

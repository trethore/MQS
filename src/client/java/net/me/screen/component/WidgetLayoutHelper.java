package net.me.screen.component;

import net.minecraft.client.gui.widget.ClickableWidget;

public final class WidgetLayoutHelper {
    public static int layoutVertically(int x, int startY, int spacing, IResizableWidget... widgets) {
        int currentY = startY;
        for (IResizableWidget widget : widgets) {
            widget.setPos(x, currentY);
            if (widget instanceof ClickableWidget clickable) {
                currentY += clickable.getHeight() + spacing;
            } else {
                currentY += 20 + spacing;
            }
        }
        return currentY;
    }
}
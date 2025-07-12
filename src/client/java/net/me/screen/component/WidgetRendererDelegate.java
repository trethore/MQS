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

package net.me.screen.component;

import net.me.screen.theme.GUIColors;
import net.me.utils.Render2DUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;

public class WidgetRendererDelegate {
    public static void renderMQSBackground(DrawContext context, ClickableWidget widget, int defaultBgColor, int hoveredBgColor) {
        int bgColor = (widget.isHovered() || widget.isFocused()) && widget.active ? hoveredBgColor : defaultBgColor;
        Render2DUtils.drawRoundedRect(context, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 3, 10, bgColor);

        if (widget.isFocused()) {
            Render2DUtils.drawRoundedOutline(context, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 3, 1, 5, GUIColors.DARK_L4.getRGB());
        }
    }
}
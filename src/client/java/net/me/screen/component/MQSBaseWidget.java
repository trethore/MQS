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
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;

@SuppressWarnings("unused")
public abstract class MQSBaseWidget extends PressableWidget implements IResizableWidget {

    protected int nonHoveredBackgroundColor = GUIColors.DARK_L2.getRGB();
    protected int hoveredBackgroundColor = GUIColors.DARK_L3.getRGB();

    public MQSBaseWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    protected abstract void renderContent(DrawContext context, int mouseX, int mouseY, float delta);

    @Override
    public final void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }
        renderBackground(context);

        renderContent(context, mouseX, mouseY, delta);
    }

    protected void renderBackground(DrawContext context) {
        int bgColor = this.isHovered() && this.active ? this.hoveredBackgroundColor : this.nonHoveredBackgroundColor;
        Render2DUtils.drawRoundedRect(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 3, 10, bgColor);

        if (this.isFocused()) {
            Render2DUtils.drawRoundedOutline(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 3, 1, 5, GUIColors.DARK_L4.getRGB());
        }
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        // No sound by default
    }

    @Override
    public void setPos(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public void setSize(int width, int height) {
        this.setWidth(width);
        this.setHeight(height);
    }

    public void setBackgroundColors(int nonHovered, int hovered) {
        this.nonHoveredBackgroundColor = nonHovered;
        this.hoveredBackgroundColor = hovered;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
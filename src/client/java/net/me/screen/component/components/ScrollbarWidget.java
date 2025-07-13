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

import net.me.screen.component.IResizableWidget;
import net.me.screen.theme.GUIColors;
import net.me.utils.Render2DUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;

public class ScrollbarWidget extends ClickableWidget implements IResizableWidget {

    private final Consumer<Double> onScroll;
    private double scrollY;
    private double maxScroll;
    private int contentHeight;
    private int viewHeight;
    private boolean isDragging = false;

    public ScrollbarWidget(int x, int y, int width, int height, Consumer<Double> onScroll) {
        super(x, y, width, height, Text.empty());
        this.onScroll = onScroll;
    }

    public void update(int contentHeight, int viewHeight, double currentScrollY) {
        this.contentHeight = contentHeight;
        this.viewHeight = viewHeight;
        this.maxScroll = Math.max(0, this.contentHeight - this.viewHeight);
        setScrollY(currentScrollY);
    }

    public void setScrollY(double scrollY) {
        this.scrollY = MathHelper.clamp(scrollY, 0, this.maxScroll);
    }

    private int getThumbHeight() {
        if (contentHeight <= viewHeight) {
            return this.getHeight();
        }
        return (int) Math.max(10, (float) this.viewHeight / this.contentHeight * this.getHeight());
    }

    private int getThumbY() {
        if (contentHeight <= viewHeight) {
            return this.getY();
        }
        double scrollPercentage = this.scrollY / this.maxScroll;
        return this.getY() + (int) (scrollPercentage * (this.getHeight() - getThumbHeight()));
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }
        Render2DUtils.drawRoundedRect(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 3, 10, GUIColors.DARK_L2.getRGB());

        if (contentHeight > viewHeight) {
            int thumbHeight = getThumbHeight();
            int thumbY = getThumbY();
            int thumbColor = (this.isHovered() || isDragging) ? GUIColors.DARK_L4.getRGB() : GUIColors.DARK_L3.getRGB();
            Render2DUtils.drawRoundedRect(context, this.getX(), thumbY, this.getWidth(), thumbHeight, 3, 10, thumbColor);
        }
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (this.isDragging) {
            double scrollableHeight = this.getHeight() - getThumbHeight();
            if (scrollableHeight > 0) {
                double clickOffset = mouseY - (this.getY() + getThumbHeight() / 2.0);
                double scrollPercentage = MathHelper.clamp(clickOffset / scrollableHeight, 0.0, 1.0);
                onScroll.accept(scrollPercentage * this.maxScroll);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY)) {
            this.isDragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isDragging) {
            this.isDragging = false;
            return true;
        }
        return false;
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

    @Override
    public void playDownSound(SoundManager soundManager) {
        // No sound
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
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
import net.me.screen.theme.UIConstants;
import net.me.utils.Render2DUtils;
import net.me.utils.TextRenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;

public abstract class AbstractToggleEntryWidget extends PressableWidget implements IResizableWidget {

    public AbstractToggleEntryWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.empty());
    }

    protected abstract boolean isToggled();

    protected abstract String getPrimaryText();

    protected abstract String getSecondaryText();

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }

        WidgetRendererDelegate.renderMQSBackground(context, this, GUIColors.DARK_L2.getRGB(), GUIColors.DARK_L3.getRGB());

        TextRenderUtils.drawCustomText(context, getPrimaryText(), this.getX() + UIConstants.PADDING_S, this.getY() + UIConstants.PADDING_S, GUIColors.TEXT.getRGB(), true, UIConstants.TEXT_SCALE);
        if (getSecondaryText() != null && !getSecondaryText().isEmpty()) {
            TextRenderUtils.drawCustomText(context, getSecondaryText(), this.getX() + UIConstants.PADDING_S, this.getY() + 17, GUIColors.TEXT.darker(25).getRGB(), true, UIConstants.SUBTITLE_SCALE);
        }

        renderToggle(context);
    }

    private void renderToggle(DrawContext context) {
        int toggleBgColor = this.isHovered() ? GUIColors.DARK_L3.lighter(10).getRGB() : GUIColors.DARK_L3.getRGB();
        int toggleBgX = this.getX() + this.getWidth() - UIConstants.TOGGLE_BG_SIZE - UIConstants.PADDING_S;
        int toggleBgY = this.getY() + (this.getHeight() - UIConstants.TOGGLE_BG_SIZE) / 2;
        Render2DUtils.drawRoundedRect(context, toggleBgX, toggleBgY, UIConstants.TOGGLE_BG_SIZE, UIConstants.TOGGLE_BG_SIZE, 2, 5, toggleBgColor);

        int stateColor = isToggled() ? GUIColors.SUCCESS.getRGB() : GUIColors.ERROR.getRGB();
        int indicatorX = toggleBgX + (UIConstants.TOGGLE_BG_SIZE - UIConstants.TOGGLE_INDICATOR_SIZE) / 2;
        int indicatorY = toggleBgY + (UIConstants.TOGGLE_BG_SIZE - UIConstants.TOGGLE_INDICATOR_SIZE) / 2;
        Render2DUtils.drawRoundedRect(context, indicatorX, indicatorY, UIConstants.TOGGLE_INDICATOR_SIZE, UIConstants.TOGGLE_INDICATOR_SIZE, 2, 10, stateColor);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        // No sound
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
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
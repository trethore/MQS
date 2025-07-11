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

import net.me.keybinds.KeyBinding;
import net.me.screen.component.IResizableWidget;
import net.me.utils.GUIColors;
import net.me.utils.Render2DUtils;
import net.me.utils.TextRenderUtils;
import net.me.utils.UIConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public class KeybindEntryWidget extends ClickableWidget implements IResizableWidget {

    private final KeyBinding keyBinding;
    private final MQSButtonWidget rebindButton;
    private boolean isListening = false;

    protected KeybindEntryWidget(int x, int y, int width, int height, KeyBinding keyBinding, Consumer<KeyBinding> onRebindClick) {
        super(x, y, width, height, Text.literal(""));
        this.keyBinding = keyBinding;

        this.rebindButton = MQSButtonWidget.builder(keyBinding.getKeyName(), (btn) -> {
                    this.setListening(true);
                    onRebindClick.accept(this.keyBinding);
                })
                .size(UIConstants.BUTTON_WIDTH_MEDIUM, UIConstants.BUTTON_HEIGHT)
                .backgroundColors(GUIColors.DARK_L3.getRGB(), GUIColors.DARK_L4.getRGB())
                .build();

        this.updateButtonBounds();
    }

    public static Builder builder(KeyBinding keyBinding, Consumer<KeyBinding> onRebindClick) {
        return new Builder(keyBinding, onRebindClick);
    }

    private void updateButtonBounds() {
        int buttonX = this.getX() + this.getWidth() - UIConstants.BUTTON_WIDTH_MEDIUM - UIConstants.PADDING_S;
        int buttonY = this.getY() + (this.getHeight() - UIConstants.BUTTON_HEIGHT) / 2;
        this.rebindButton.setPos(buttonX, buttonY);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }


    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int color = GUIColors.DARK_L2.getRGB();
        Render2DUtils.drawRoundedRect(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 3, 10, color);
        TextRenderUtils.drawCustomText(
                context,
                keyBinding.getName(),
                this.getX() + 5,
                this.getY() + (this.getHeight() - 8) / 2f,
                GUIColors.TEXT.getRGB(),
                true,
                1f
        );

        if (isListening) {
            rebindButton.setMessage(Text.literal("> " + Formatting.YELLOW + Formatting.UNDERLINE + "..." + Formatting.RESET + " <"));
        } else {
            rebindButton.setMessage(Text.literal(keyBinding.getKeyName()));
        }

        rebindButton.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return rebindButton.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return rebindButton.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) || rebindButton.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void setPos(int x, int y) {
        this.setX(x);
        this.setY(y);
        updateButtonBounds();
    }

    @Override
    public void setSize(int width, int height) {
        this.setWidth(width);
        this.setHeight(height);
        updateButtonBounds();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        updateButtonBounds();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        updateButtonBounds();
    }

    public void setListening(boolean listening) {
        this.isListening = listening;
    }

    public KeyBinding getKeyBinding() {
        return keyBinding;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
        rebindButton.appendNarrations(builder);
    }

    public static class Builder {
        private final KeyBinding keyBinding;
        private final Consumer<KeyBinding> onRebindClick;
        private int x = 0;
        private int y = 0;
        private int width = 220;
        private int height = 24;

        public Builder(KeyBinding keyBinding, Consumer<KeyBinding> onRebindClick) {
            this.keyBinding = keyBinding;
            this.onRebindClick = onRebindClick;
        }

        public Builder dimensions(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public KeybindEntryWidget build() {
            return new KeybindEntryWidget(this.x, this.y, this.width, this.height, this.keyBinding, this.onRebindClick);
        }
    }
}
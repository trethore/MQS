package net.me.screen.component.components;

import net.me.screen.component.IResizableWidget;
import net.me.utils.GUIColors;
import net.me.utils.McUtils;
import net.me.utils.Render2DUtils;
import net.me.utils.TextRendererUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;

@SuppressWarnings("unused")
public class MQSButtonWidget extends ButtonWidget implements IResizableWidget {
    protected int nonHoveredBackgroundColor;
    protected int hoveredBackgroundColor;
    protected int inactiveTextColor;
    protected int activeTextColor;

    protected MQSButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress,
                              int nonHoveredBackgroundColor, int hoveredBackgroundColor,
                              int inactiveTextColor, int activeTextColor) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.nonHoveredBackgroundColor = nonHoveredBackgroundColor;
        this.hoveredBackgroundColor = hoveredBackgroundColor;
        this.inactiveTextColor = inactiveTextColor;
        this.activeTextColor = activeTextColor;
    }

    public static Builder builder(String message, PressAction onPress) {
        return new Builder(message, onPress);
    }

    public void setBackgroundColors(int nonHoveredColor, int hoveredColor) {
        this.nonHoveredBackgroundColor = nonHoveredColor;
        this.hoveredBackgroundColor = hoveredColor;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    public void setTextColors(int inactiveColor, int activeColor) {
        this.inactiveTextColor = inactiveColor;
        this.activeTextColor = activeColor;
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
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        McUtils.getMc().ifPresent(mc -> {
            TextRenderer textRenderer = TextRendererUtils.getCustomTextRenderer();

            boolean isHovered = this.isHovered() && this.active;
            int bgColor = isHovered ? this.hoveredBackgroundColor : this.nonHoveredBackgroundColor;
            Render2DUtils.drawRoundedRect(context, this.getX(), this.getY(), this.width, this.height, 3, 5, bgColor);

            int textColor = this.active ? this.activeTextColor : this.inactiveTextColor;
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    this.getMessage(),
                    this.getX() + this.width / 2,
                    this.getY() + (this.height - 8) / 2,
                    textColor
            );
        });
    }

    public static class Builder {
        private final String message;
        private final PressAction onPress;
        private int x = 0;
        private int y = 0;
        private int width = 200;
        private int height = 20;

        private int nonHoveredBackgroundColor = GUIColors.DARK_L2.getRGB();
        private int hoveredBackgroundColor = GUIColors.DARK_L3.getRGB();
        private int inactiveTextColor = GUIColors.TEXT_DISABLED.getRGB();
        private int activeTextColor = GUIColors.TEXT.getRGB();

        public Builder(String message, PressAction onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder backgroundColors(int nonHovered, int hovered) {
            this.nonHoveredBackgroundColor = nonHovered;
            this.hoveredBackgroundColor = hovered;
            return this;
        }

        public Builder textColors(int inactive, int active) {
            this.inactiveTextColor = inactive;
            this.activeTextColor = active;
            return this;
        }

        public Builder dimensions(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public MQSButtonWidget build() {
            return new MQSButtonWidget(
                    this.x, this.y, this.width, this.height, Text.literal(this.message), this.onPress,
                    this.nonHoveredBackgroundColor, this.hoveredBackgroundColor,
                    this.inactiveTextColor, this.activeTextColor
            );
        }
    }
}
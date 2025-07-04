package net.me.screen.component.components;

import net.me.utils.GUIColors;
import net.me.utils.Render2DUtils;
import net.me.utils.TextRendererUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.Color;

@SuppressWarnings("unused")
public class MQSImageButtonWidget extends MQSButtonWidget {
    protected final Identifier image;
    protected int imagePadding = 4;
    protected int imageTextGap = 5;
    protected Color imageColor = Color.WHITE;

    protected MQSImageButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, Identifier image,
                                   int nonHoveredBackgroundColor, int hoveredBackgroundColor,
                                   int inactiveTextColor, int activeTextColor) {
        super(x, y, width, height, message, onPress, nonHoveredBackgroundColor, hoveredBackgroundColor, inactiveTextColor, activeTextColor);
        this.image = image;
    }

    public static Builder builder(Identifier image, String message, PressAction onPress) {
        return new Builder(image, message, onPress);
    }

    public void setImageColor(Color color) {
        this.imageColor = color;
    }

    public void setImagePadding(int padding) {
        this.imagePadding = padding;
    }

    public void setImageTextGap(int gap) {
        this.imageTextGap = gap;
    }


    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean isHovered = this.isHovered() && this.active;
        int bgColor = isHovered ? this.hoveredBackgroundColor : this.nonHoveredBackgroundColor;
        Render2DUtils.drawRoundedRect(context, this.getX(), this.getY(), this.width, this.height, 3, 5, bgColor);

        TextRenderer textRenderer = TextRendererUtils.getCustomTextRenderer();

        int imageSize = this.getHeight() - this.imagePadding * 2;
        int imageX = this.getX() + this.imagePadding;
        int imageY = this.getY() + this.imagePadding;

        Render2DUtils.drawImage(image, imageX, imageY, imageX + imageSize, imageY + imageSize, 0, false, this.imageColor);

        int textColor = this.active ? this.activeTextColor : this.inactiveTextColor;
        int textX = imageX + imageSize + this.imageTextGap;
        int textY = this.getY() + (this.height - 8) / 2;

        context.drawTextWithShadow(
                textRenderer,
                this.getMessage(),
                textX,
                textY,
                textColor
        );
    }

    public static class Builder {
        private final String message;
        private final PressAction onPress;
        private Identifier image;
        private int x = 0;
        private int y = 0;
        private int width = 200;
        private int height = 20;

        private int nonHoveredBackgroundColor = GUIColors.DARK_L2.getRGB();
        private int hoveredBackgroundColor = GUIColors.DARK_L3.getRGB();
        private int inactiveTextColor = GUIColors.TEXT_DISABLED.getRGB();
        private int activeTextColor = GUIColors.TEXT.getRGB();

        public Builder(Identifier image, String message, PressAction onPress) {
            this.image = image;
            this.message = message;
            this.onPress = onPress;
        }

        public Builder image(Identifier image) {
            this.image = image;
            return this;
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

        public MQSImageButtonWidget build() {
            if (image == null) {
                throw new IllegalStateException("Image must be set for MQSImageButtonWidget");
            }
            return new MQSImageButtonWidget(
                    this.x, this.y, this.width, this.height, Text.literal(this.message), this.onPress, this.image,
                    this.nonHoveredBackgroundColor, this.hoveredBackgroundColor,
                    this.inactiveTextColor, this.activeTextColor
            );
        }
    }
}
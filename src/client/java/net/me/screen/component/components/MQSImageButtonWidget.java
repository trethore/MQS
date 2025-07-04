package net.me.screen.component.components;

import net.me.utils.GUIColors;
import net.me.utils.Render2DUtils;
import net.me.utils.TextRendererUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;

@SuppressWarnings("unused")
public class MQSImageButtonWidget extends MQSButtonWidget {
    protected Identifier image;
    protected int imagePadding = 4;
    protected int imageTextGap = 4;
    protected Color imageColor = Color.WHITE;
    protected int imageWidth;
    protected int imageHeight;

    protected MQSImageButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, Identifier image,
                                   int imageWidth, int imageHeight,
                                   int nonHoveredBackgroundColor, int hoveredBackgroundColor,
                                   int inactiveTextColor, int activeTextColor) {
        super(x, y, width, height, message, onPress, nonHoveredBackgroundColor, hoveredBackgroundColor, inactiveTextColor, activeTextColor);
        this.image = image;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public static Builder builder(Identifier image, String message, PressAction onPress) {
        return new Builder(image, message, onPress);
    }

    public static Builder builder(Identifier image, PressAction onPress) {
        return new Builder(image, "", onPress);
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

    public void setImage(Identifier image) {
        this.image = image;
    }

    public void setImageSize(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
    }


    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean isHovered = this.isHovered() && this.active;
        int bgColor = isHovered ? this.hoveredBackgroundColor : this.nonHoveredBackgroundColor;
        Render2DUtils.drawRoundedRect(context, this.getX(), this.getY(), this.width, this.height, 3, 5, bgColor);

        TextRenderer textRenderer = TextRendererUtils.getCustomTextRenderer();
        Text message = this.getMessage();
        boolean hasText = !message.getString().isEmpty();

        int finalImageWidth = this.imageWidth;
        int finalImageHeight = this.imageHeight;

        if (finalImageWidth <= 0 || finalImageHeight <= 0) {
            int calculatedSize = this.getHeight() - this.imagePadding * 2;
            finalImageWidth = calculatedSize;
            finalImageHeight = calculatedSize;
        }

        int textWidth = hasText ? textRenderer.getWidth(message) : 0;
        int gap = hasText ? this.imageTextGap : 0;
        int totalContentWidth = finalImageWidth + gap + textWidth;

        int imageX = this.getX() + (this.getWidth() - totalContentWidth) / 2;
        int imageY = this.getY() + (this.getHeight() - finalImageHeight) / 2;

        if (this.image == null) {
            finalImageWidth = Math.round(finalImageWidth / 2f) - this.imagePadding / 2;
        } else {
            Render2DUtils.drawImage(image, imageX, imageY, imageX + finalImageWidth, imageY + finalImageHeight, 0, false, this.imageColor);
        }
        if (hasText) {
            int textColor = this.active ? this.activeTextColor : this.inactiveTextColor;
            int textX = imageX + finalImageWidth + gap;
            int textY = this.getY() + (this.height - 8) / 2;
            context.drawTextWithShadow(
                    textRenderer,
                    message,
                    textX,
                    textY,
                    textColor
            );
        }
    }

    public static class Builder {
        private final String message;
        private final PressAction onPress;
        private Identifier image;
        private int x = 0;
        private int y = 0;
        private int width = 200;
        private int height = 20;
        private int imageWidth = 0;
        private int imageHeight = 0;

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

        public Builder imageSize(int width, int height) {
            this.imageWidth = width;
            this.imageHeight = height;
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
                    this.imageWidth, this.imageHeight,
                    this.nonHoveredBackgroundColor, this.hoveredBackgroundColor,
                    this.inactiveTextColor, this.activeTextColor
            );
        }
    }
}
package net.me.screen.component.components;

import net.me.utils.GUIColors;
import net.me.utils.Render2DUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class DarkButtonWidget extends ButtonWidget {

    public DarkButtonWidget(int x, int y, int width, int height, String message, PressAction onPress) {
        super(x, y, width, height, Text.literal(message), onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    public static Builder builder(String message, PressAction onPress) {
        return new Builder(message, onPress);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        TextRenderer textRenderer = minecraftClient.textRenderer;

        boolean isHovered = this.isHovered() && this.active;
        int color = isHovered ? GUIColors.DARK_L3.getRGBA() : GUIColors.DARK_L2.getRGBA();
        Render2DUtils.drawRoundedRect(context, this.getX(), this.getY(), this.width, this.height, 3, 5, color);

        int textColor = this.active ? GUIColors.WHITE.getRGBA() : GUIColors.TEXT_GREY_DISABLED.getRGBA();
        context.drawCenteredTextWithShadow(
                textRenderer,
                this.getMessage(),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2,
                textColor
        );
    }

    public static class Builder extends ButtonWidget.Builder {
        private final String message;
        private final PressAction onPress;
        private int x;
        private int y;
        private int width = 200;
        private int height = 20;

        public Builder(String message, PressAction onPress) {
            super(Text.literal(message), onPress);
            this.message = message;
            this.onPress = onPress;
        }

        public Builder dimensions(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public DarkButtonWidget build() {
            return new DarkButtonWidget(this.x, this.y, this.width, this.height, this.message, this.onPress);
        }
    }
}
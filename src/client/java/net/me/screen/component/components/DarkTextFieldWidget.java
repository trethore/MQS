package net.me.screen.component.components;

import net.me.utils.GUIColors;
import net.me.utils.Render2DUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class DarkTextFieldWidget extends TextFieldWidget {

    protected DarkTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, String placeholder) {
        super(textRenderer, x, y, width, height, Text.literal(placeholder));
    }

    public static Builder builder(TextRenderer textRenderer) {
        return new Builder(textRenderer);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        this.setDrawsBackground(false);
        context.getMatrices().translate(5,6,0);
        super.renderWidget(context, mouseX, mouseY, delta);
        context.getMatrices().translate(-5,-6,0);
    }

    private void renderBackground(DrawContext context) {
        int color = this.isHovered() ? GUIColors.DARK_L3.getRGBA() : GUIColors.DARK_L2.getRGBA();
        color = this.isFocused() ? GUIColors.DARK_L4.getRGBA() : color;
        Render2DUtils.drawRoundedRect(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(),3,10, color);
    }

    public void clearText() {
        this.setText("");
    }

    public static class Builder {
        private TextRenderer textRenderer;
        private int x;
        private int y;
        private int width = 200;
        private int height = 20;
        private String placeholder;

        public Builder(TextRenderer textRenderer) {
            this.textRenderer = textRenderer;
        }

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder dimensions(int x, int y,int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }
        public Builder placeholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public DarkTextFieldWidget build() {
            return new DarkTextFieldWidget(this.textRenderer, this.x, this.y, this.width, this.height, placeholder);
        }
    }
}

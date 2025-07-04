package net.me.screen.component.components;

import net.me.keybinds.KeyBinding;
import net.me.screen.component.IResizableWidget;
import net.me.utils.GUIColors;
import net.me.utils.TextRenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public class KeybindEntryWidget extends ClickableWidget implements IResizableWidget {

    private static final int REBIND_BUTTON_WIDTH = 90;
    private static final int REBIND_BUTTON_HEIGHT = 20;
    private static final int REBIND_BUTTON_MARGIN_RIGHT = 5;
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
                .size(REBIND_BUTTON_WIDTH, REBIND_BUTTON_HEIGHT)
                .build();

        this.updateButtonBounds();
    }

    public static Builder builder(KeyBinding keyBinding, Consumer<KeyBinding> onRebindClick) {
        return new Builder(keyBinding, onRebindClick);
    }

    private void updateButtonBounds() {
        int buttonX = this.getX() + this.getWidth() - REBIND_BUTTON_WIDTH - REBIND_BUTTON_MARGIN_RIGHT;
        int buttonY = this.getY() + (this.getHeight() - REBIND_BUTTON_HEIGHT) / 2;
        this.rebindButton.setPos(buttonX, buttonY);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
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
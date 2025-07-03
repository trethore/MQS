// New File: java/net/me/screen/component/components/KeybindEntryWidget.java
package net.me.screen.component.components;

import net.me.keybinds.KeyBinding;
import net.me.utils.GUIColors;
import net.me.utils.Render2DUtils;
import net.me.utils.TextRenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public class KeybindEntryWidget extends ClickableWidget {
    private final KeyBinding keyBinding;
    private final DarkButtonWidget rebindButton;
    private boolean isListening = false;

    public KeybindEntryWidget(int x, int y, int width, int height, KeyBinding keyBinding, Consumer<KeyBinding> onRebindClick) {
        super(x, y, width, height, Text.literal(keyBinding.getName()));
        this.keyBinding = keyBinding;

        this.rebindButton = DarkButtonWidget.builder(keyBinding.getKeyName(), (btn) -> {
            isListening = true;
            onRebindClick.accept(keyBinding);
        }).dimensions(this.getX() + this.getWidth() - 100, this.getY() + (this.getHeight() - 20) / 2, 90, 20).build();
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderUtils.drawText(context, keyBinding.getName(), this.getX() + 5, this.getY() + (this.getHeight() - 8) / 2f, GUIColors.WHITE.getRGBA(), true, 1f);

        if (isListening) {
            rebindButton.setMessage(Text.literal("> " + Formatting.UNDERLINE + keyBinding.getKeyName() + Formatting.RESET + " <"));
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
    public void setX(int x) {
        super.setX(x);
        this.rebindButton.setX(this.getX() + this.getWidth() - 100);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.rebindButton.setY(this.getY() + (this.getHeight() - 20) / 2);
    }

    public void setListening(boolean listening) {
        this.isListening = listening;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    public KeyBinding getKeyBinding() {
        return keyBinding;
    }
}
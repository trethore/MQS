package net.me.screen.component.components;

import net.me.screen.component.IResizableWidget;
import net.me.utils.GUIColors;
import net.me.utils.Render2DUtils;
import net.me.utils.TextRenderUtils;
import net.me.utils.UIConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class BooleanSettingEntryWidget extends PressableWidget implements IResizableWidget {

    private final String name;
    private final String description;
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    protected BooleanSettingEntryWidget(int x, int y, int width, int height, String name, String description, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        super(x, y, width, height, Text.literal(name));
        this.name = name;
        this.description = description;
        this.getter = getter;
        this.setter = setter;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    public void onPress() {
        if (this.setter != null) {
            this.setter.accept(!this.getter.get());
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }

        boolean isHovered = this.active && this.isMouseOver(mouseX, mouseY);
        int bgColor = isHovered ? GUIColors.DARK_L3.getRGB() : GUIColors.DARK_L2.getRGB();
        Render2DUtils.drawRoundedRect(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 3, 10, bgColor);

        TextRenderUtils.drawCustomText(context, this.name, this.getX() + UIConstants.PADDING_S, this.getY() + UIConstants.PADDING_S, GUIColors.TEXT.getRGB(), true, UIConstants.TEXT_SCALE);
        if (this.description != null && !this.description.isEmpty()) {
            TextRenderUtils.drawCustomText(context, this.description, this.getX() + UIConstants.PADDING_S, this.getY() + UIConstants.PADDING_S + 12, GUIColors.TEXT_DISABLED.getRGB(), true, UIConstants.TEXT_SCALE);
        }
        renderToggle(context, isHovered);
    }

    private void renderToggle(DrawContext context, boolean isHovered) {
        int toggleBgColor = isHovered ? GUIColors.DARK_L3.lighter(10).getRGB() : GUIColors.DARK_L3.getRGB();
        int toggleBgX = this.getX() + this.getWidth() - UIConstants.TOGGLE_BG_SIZE - UIConstants.PADDING_S;
        int toggleBgY = this.getY() + (this.getHeight() - UIConstants.TOGGLE_BG_SIZE) / 2;
        Render2DUtils.drawRoundedRect(context, toggleBgX, toggleBgY, UIConstants.TOGGLE_BG_SIZE, UIConstants.TOGGLE_BG_SIZE, 2, 5, toggleBgColor);

        int stateColor = this.getter.get()
                ? GUIColors.SUCCESS.getRGB()
                : GUIColors.ERROR.getRGB();
        int indicatorX = toggleBgX + (UIConstants.TOGGLE_BG_SIZE - UIConstants.TOGGLE_INDICATOR_SIZE) / 2;
        int indicatorY = toggleBgY + (UIConstants.TOGGLE_BG_SIZE - UIConstants.TOGGLE_INDICATOR_SIZE) / 2;
        Render2DUtils.drawRoundedRect(context, indicatorX, indicatorY, UIConstants.TOGGLE_INDICATOR_SIZE, UIConstants.TOGGLE_INDICATOR_SIZE, 2, 10, stateColor);
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

    public static class Builder {
        private String name = "";
        private String description = "";
        private Supplier<Boolean> getter = () -> false;
        private Consumer<Boolean> setter = (b) -> {
        };
        private int x = 0;
        private int y = 0;
        private int width = 200;
        private int height = 40;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder getter(Supplier<Boolean> getter) {
            this.getter = getter;
            return this;
        }

        public Builder setter(Consumer<Boolean> setter) {
            this.setter = setter;
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

        public BooleanSettingEntryWidget build() {
            return new BooleanSettingEntryWidget(this.x, this.y, this.width, this.height, this.name, this.description, this.getter, this.setter);
        }
    }
}
package net.me.screen.component.components;


import net.me.scripting.ScriptManager;
import net.me.scripting.module.ScriptDescriptor;
import net.me.utils.GUIColors;
import net.me.utils.Render2DUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

public class ScriptDescriptorToggleWidget extends PressableWidget {
    private final ScriptDescriptor descriptor;

    public ScriptDescriptorToggleWidget(int x, int y, int width, int height, ScriptDescriptor descriptor) {
        super(x, y, width, height, Text.literal(""));
        this.descriptor = descriptor;
    }

    public static Builder builder(ScriptDescriptor descriptor) {
        return new Builder(descriptor);
    }

    @Override
    public void onPress() {
        ScriptManager sm = ScriptManager.getInstance();
        if (sm.isRunning(descriptor.getId())) {
            sm.disableScript(descriptor.getId());
        } else {
            sm.enableScript(descriptor.getId());
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean isHovered = this.active && this.isMouseOver(mouseX, mouseY);
        int color = isHovered ? GUIColors.DARK_L3.getRGBA() : GUIColors.DARK_L2.getRGBA();
        Render2DUtils.drawRoundedRect(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 3, 10, color);
        Render2DUtils.drawText(context, this.descriptor.moduleName(), this.getX() + 5, this.getY() + 5, GUIColors.WHITE.getRGBA(), true, 1);
        Render2DUtils.drawText(context, this.descriptor.path().getFileName().toString(), this.getX() + 5, this.getY() + 16, GUIColors.WHITE.darker(25).getRGB(), true, 0.8f);
        renderToggleState(context, isHovered);
    }

    private void renderToggleState(DrawContext context, boolean isHovered) {
        int color = isHovered ? GUIColors.DARK_L3.lighter(10).getRGB() : GUIColors.DARK_L3.getRGBA();

        Render2DUtils.drawRoundedRect(context, this.getX() + this.getWidth() - 25, this.getY() + 5, 20, 20, 2, 5, color);
        int stateColor;
        if (ScriptManager.getInstance().isRunning(descriptor.getId())) {
            stateColor = GUIColors.SUCCESS.getRGBA();
        } else {
            stateColor = GUIColors.ERROR.getRGBA();
        }
        Render2DUtils.drawRoundedRect(context, this.getX() + this.getWidth() - 23, this.getY() + 7, 16, 16, 2, 10, stateColor);

    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    public ScriptDescriptor getDescriptor() {
        return descriptor;
    }

    public static class Builder {
        private final ScriptDescriptor descriptor;
        private int x;
        private int y;
        private int width = 200;
        private int height = 30;

        public Builder(ScriptDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        public ScriptDescriptorToggleWidget.Builder dimensions(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public ScriptDescriptorToggleWidget.Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public ScriptDescriptorToggleWidget build() {
            return new ScriptDescriptorToggleWidget(this.x, this.y, this.width, this.height, descriptor);
        }
    }

}

package net.me.screen.component.components;

import net.me.scripting.ScriptingService;
import net.me.scripting.module.ScriptDescriptor;
import net.me.utils.GUIColors;
import net.me.utils.Render2DUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class ScriptDescriptorToggleWidget extends PressableWidget {
    private final ScriptingService scriptingService;
    private ScriptDescriptor descriptor;

    protected ScriptDescriptorToggleWidget(int x, int y, int width, int height, @Nullable ScriptDescriptor descriptor, ScriptingService scriptingService) {
        super(x, y, width, height, Text.literal(""));
        this.scriptingService = scriptingService;
        this.update(descriptor);
    }

    public static Builder builder(@Nullable ScriptDescriptor descriptor, ScriptingService scriptingService) {
        return new Builder(descriptor, scriptingService);
    }

    public void update(@Nullable ScriptDescriptor descriptor) {
        this.descriptor = descriptor;
        this.visible = (descriptor != null);
        this.active = (descriptor != null);
    }

    @Override
    public void onPress() {
        if (descriptor == null) return;

        if (scriptingService.isRunning(descriptor.getId())) {
            scriptingService.disable(descriptor.getId());
        } else {
            scriptingService.enable(descriptor.getId());
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.descriptor == null) return;

        boolean isHovered = this.active && this.isMouseOver(mouseX, mouseY);
        int color = isHovered ? GUIColors.DARK_L3.getRGBA() : GUIColors.DARK_L2.getRGBA();
        Render2DUtils.drawRoundedRect(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 3, 10, color);
        String title = this.descriptor.moduleName() + " v" + this.descriptor.version();
        Render2DUtils.drawText(context, title, this.getX() + 5, this.getY() + 5, GUIColors.WHITE.getRGBA(), true, 1);
        Render2DUtils.drawText(context, this.descriptor.path().getFileName().toString(), this.getX() + 5, this.getY() + 16, GUIColors.WHITE.darker(25).getRGB(), true, 0.8f);
        renderToggleState(context, isHovered);
    }

    private void renderToggleState(DrawContext context, boolean isHovered) {
        if (this.descriptor == null) return;
        int color = isHovered ? GUIColors.DARK_L3.lighter(10).getRGB() : GUIColors.DARK_L3.getRGBA();

        Render2DUtils.drawRoundedRect(context, this.getX() + this.getWidth() - 25, this.getY() + 5, 20, 20, 2, 5, color);
        int stateColor;
        if (scriptingService.isRunning(descriptor.getId())) {
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

    @Nullable
    public ScriptDescriptor getDescriptor() {
        return descriptor;
    }

    public static class Builder {
        private final ScriptDescriptor descriptor;
        private final ScriptingService scriptingService;
        private int x;
        private int y;
        private int width = 200;
        private int height = 30;

        public Builder(@Nullable ScriptDescriptor descriptor, ScriptingService scriptingService) {
            this.descriptor = descriptor;
            this.scriptingService = scriptingService;
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
            return new ScriptDescriptorToggleWidget(this.x, this.y, this.width, this.height, descriptor, scriptingService);
        }
    }

}
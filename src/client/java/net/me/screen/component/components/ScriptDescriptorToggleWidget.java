package net.me.screen.component.components;

import net.me.config.GlobalConfigManager;
import net.me.screen.component.IResizableWidget;
import net.me.scripting.ScriptingService;
import net.me.scripting.module.ScriptDescriptor;
import net.me.utils.GUIColors;
import net.me.utils.Render2DUtils;
import net.me.utils.TextRenderUtils;
import net.me.utils.UIConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ScriptDescriptorToggleWidget extends PressableWidget implements IResizableWidget {
    private final ScriptingService scriptingService;
    private final GlobalConfigManager globalConfigManager;
    private ScriptDescriptor descriptor;

    protected ScriptDescriptorToggleWidget(int x, int y, int width, int height, @Nullable ScriptDescriptor descriptor, ScriptingService scriptingService, GlobalConfigManager globalConfigManager) {
        super(x, y, width, height, Text.empty());
        this.scriptingService = scriptingService;
        this.globalConfigManager = globalConfigManager;
        this.update(descriptor);
    }

    public static Builder builder(ScriptingService scriptingService, GlobalConfigManager globalConfigManager) {
        return new Builder(scriptingService, globalConfigManager);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    public void update(@Nullable ScriptDescriptor descriptor) {
        this.descriptor = descriptor;
        this.visible = (descriptor != null);
        this.active = (descriptor != null);
    }

    @Override
    public void onPress() {
        if (descriptor == null) return;
        boolean isRunning = scriptingService.isRunning(descriptor.getId());
        if (isRunning) {
            scriptingService.disable(descriptor.getId());
        } else {
            scriptingService.enable(descriptor.getId());
        }
        if (this.globalConfigManager.isEnableDisableToastEnabled()) {
            String state = (isRunning ? Formatting.RED + "disabled" : Formatting.GREEN + "enabled") + Formatting.RESET;
            MQSToast.show(descriptor.moduleName() + " " + state, "This script has been successfully " + (isRunning ? "disabled" : "enabled"), 1500, MQSToast.Corner.TOP_LEFT);
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.descriptor == null || !this.visible) {
            return;
        }

        boolean isHovered = this.active && this.isMouseOver(mouseX, mouseY);
        int bgColor = isHovered ? GUIColors.DARK_L3.getRGB() : GUIColors.DARK_L2.getRGB();
        Render2DUtils.drawRoundedRect(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 3, 10, bgColor);

        String title = this.descriptor.moduleName() + " v" + this.descriptor.version();
        TextRenderUtils.drawCustomText(context, title, this.getX() + UIConstants.PADDING_S, this.getY() + UIConstants.PADDING_S, GUIColors.TEXT.getRGB(), true, UIConstants.TEXT_SCALE);
        TextRenderUtils.drawCustomText(context, this.descriptor.path().getFileName().toString(), this.getX() + UIConstants.PADDING_S, this.getY() + 16, GUIColors.TEXT.darker(25).getRGB(), true, UIConstants.SUBTITLE_SCALE);
        renderToggle(context, isHovered);
    }

    private void renderToggle(DrawContext context, boolean isHovered) {
        int toggleBgColor = isHovered ? GUIColors.DARK_L3.lighter(10).getRGB() : GUIColors.DARK_L3.getRGB();
        int toggleBgX = this.getX() + this.getWidth() - UIConstants.TOGGLE_BG_SIZE - UIConstants.PADDING_S;
        int toggleBgY = this.getY() + (this.getHeight() - UIConstants.TOGGLE_BG_SIZE) / 2;
        Render2DUtils.drawRoundedRect(context, toggleBgX, toggleBgY, UIConstants.TOGGLE_BG_SIZE, UIConstants.TOGGLE_BG_SIZE, 2, 5, toggleBgColor);

        int stateColor = scriptingService.isRunning(descriptor.getId())
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

    @Nullable
    public ScriptDescriptor getDescriptor() {
        return descriptor;
    }

    public static class Builder {
        private final ScriptingService scriptingService;
        private final GlobalConfigManager globalConfigManager;

        private ScriptDescriptor descriptor = null;
        private int x = 0;
        private int y = 0;
        private int width = 200;
        private int height = 30;

        public Builder(ScriptingService scriptingService, GlobalConfigManager globalConfigManager) {
            this.scriptingService = scriptingService;
            this.globalConfigManager = globalConfigManager;
        }

        public Builder descriptor(ScriptDescriptor descriptor) {
            this.descriptor = descriptor;
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

        public ScriptDescriptorToggleWidget build() {
            return new ScriptDescriptorToggleWidget(this.x, this.y, this.width, this.height, this.descriptor, this.scriptingService, this.globalConfigManager);
        }
    }
}
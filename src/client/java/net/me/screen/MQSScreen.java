package net.me.screen;

import net.me.utils.Position;
import net.me.utils.Render2DUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public abstract class MQSScreen extends Screen {
    private final String title;
    private int windowWidth;
    private int windowHeight;
    private MQSScreen parent;

    protected MQSScreen(String title, int windowWidth, int windowHeight) {
        this(title, windowWidth, windowHeight, null);
    }

    protected MQSScreen(String title, int windowWidth, int windowHeight, @Nullable MQSScreen parent) {
        super(Text.literal("MQS: " + title));
        this.title = title;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.parent = parent;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        int startX = getMiddlePoint().x() - windowWidth / 2;
        int startY = getMiddlePoint().y() - windowHeight / 2;

        Render2DUtils.drawRect(context, 0, 0, this.width, this.height, 0x11333333);
        this.applyBlur();
        Render2DUtils.drawRoundedRect(context, startX, startY, this.windowWidth, this.windowHeight, 10, 5, 0xFF181719);
        Render2DUtils.drawRoundedOutline(context, startX, startY, this.windowWidth, this.windowHeight, 10, 2, 5, 0xFF212121);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawTitle(context);
    }

    @Override
    public void close() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (this.parent != null) {
            mc.setScreen(this.parent);
        } else {
            super.close();
        }
    }


    private void drawTitle(DrawContext context) {
        int x = getMiddlePoint().x();
        int y = getMiddlePoint().y() - this.windowHeight / 2 + 22;
        int color = Render2DUtils.getRainbowColor(2000, 0.8f, 1.0f);
        Render2DUtils.drawCenteredText(context, this.getStringTitle(), x, y, color, true, 1.5f);
    }

    public void open() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.send(() -> mc.setScreen(this));
        if (MinecraftClient.getInstance().currentScreen instanceof MQSScreen) {
            this.parent = (MQSScreen) MinecraftClient.getInstance().currentScreen;
        }
    }

    protected Position getMiddlePoint() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        return new Position(centerX, centerY);
    }

    public void setWindowDimensions(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
    }

    public int getWindowWidth() {
        return this.windowWidth;
    }

    public int getWindowHeight() {
        return this.windowHeight;
    }

    public String getStringTitle() {
        return this.title;
    }


}

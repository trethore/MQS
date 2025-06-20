package net.me.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public abstract class MQSScreen extends Screen {
    protected MQSScreen(String title) {
        super(Text.literal("MQS: " + title));
    }

    public void open() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.send(() -> mc.setScreen(this));
    }

    protected void init() {
        super.init();
        initHeader();
        initBody();
        initFooter();
    }

    protected void initHeader() {}

    protected void initBody() {}

    protected void initFooter() {
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, (button) -> this.close())
                .dimensions(this.width / 2 - 100, this.height - 27, 200, 20).build());
    }
}

package net.me.screen.screens;

import net.me.Main;
import net.me.scripting.Script;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AllScriptsScreen extends Screen {
    public AllScriptsScreen() {
        super(Text.literal(Main.MOD_ID + ":all_scripts"));
    }

    @Override
    protected void init() {
        super.init();
        ButtonWidget widget = ButtonWidget.builder(Text.literal("Test JS"), button -> {
            Script jsScript = new Script("test", Main.MOD_DIR.resolve("scripts").resolve("test4.js"));
            jsScript.run();
        }).dimensions(this.width / 2 - 100, this.height / 4, 200, 20).build();
        this.addDrawableChild(widget);
    }
}

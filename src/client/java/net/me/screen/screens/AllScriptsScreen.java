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
        int x = this.width / 2 - 100;
        ButtonWidget scriptButton1 = ButtonWidget.builder(Text.literal("Test1 JS"), button -> {
            Script jsScript = new Script("test", Main.MOD_DIR.resolve("scripts").resolve("test.js"));
            jsScript.run();
        }).dimensions(x, this.height / 4, 200, 20).build();
        ButtonWidget scriptButton2 = ButtonWidget.builder(Text.literal("Test2 JS"), button -> {
            Script jsScript = new Script("test", Main.MOD_DIR.resolve("scripts").resolve("test2.js"));
            jsScript.run();
        }).dimensions(x, this.height / 4 + 40, 200, 20).build();
        ButtonWidget scriptButton3 = ButtonWidget.builder(Text.literal("Test3 JS"), button -> {
            Script jsScript = new Script("test", Main.MOD_DIR.resolve("scripts").resolve("test3.js"));
            jsScript.run();
        }).dimensions(x, this.height / 4 + 2 * 40, 200, 20).build();

        this.addDrawableChild(scriptButton3);
        this.addDrawableChild(scriptButton2);
        this.addDrawableChild(scriptButton1);
    }
}

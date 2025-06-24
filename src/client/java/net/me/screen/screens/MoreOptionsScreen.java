package net.me.screen.screens;

import net.me.Main;
import net.me.screen.MQSScreen;
import net.me.screen.component.components.DarkButtonWidget;
import net.minecraft.util.Util;

import java.nio.file.Path;

public class MoreOptionsScreen extends MQSScreen {

    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 5;

    public MoreOptionsScreen(MQSScreen parent) {
        super("More Options", 200, 150, parent);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = getMiddlePoint().x();
        int startY = getMiddlePoint().y() - 35;

        addDrawableChild(DarkButtonWidget.builder("Open Configs Folder", button -> openFolder("configs")).dimensions(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        startY += BUTTON_HEIGHT + BUTTON_SPACING;
        addDrawableChild(DarkButtonWidget.builder("Open Scripts Folder", button -> openFolder("scripts")).dimensions(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        startY += BUTTON_HEIGHT + BUTTON_SPACING;
        addDrawableChild(DarkButtonWidget.builder("Commands", button -> {
            System.out.println("Registered commands screen here");
            // Later: new CommandsScreen(this).open();
        }).dimensions(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        startY += BUTTON_HEIGHT + BUTTON_SPACING;
        addDrawableChild(DarkButtonWidget.builder("Keybinds", button -> {
            System.out.println("Keybinds screen here");
            // Later: new KeybindsScreen(this).open();
        }).dimensions(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());


    }

    private void openFolder(String folder) {
        try {
            Path configsDir = Main.MOD_DIR.resolve(folder);
            if (!java.nio.file.Files.exists(configsDir)) {
                java.nio.file.Files.createDirectories(configsDir);
            }
            Util.getOperatingSystem().open(configsDir.toFile());
        } catch (Exception e) {
            Main.LOGGER.error("Could not open configs folder", e);
        }
    }
}
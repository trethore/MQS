package net.me.screen.screens;

import net.me.screen.MQSScreen;
import net.me.screen.component.components.DarkButtonWidget;
import net.me.screen.component.components.ScriptDescriptorToggleWidget;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.module.ScriptDescriptor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AllScriptsScreen extends MQSScreen {

    private final List<ScriptDescriptor> allScripts;
    private int currentPage = 0;
    private int totalPages = 0;
    private static final int ITEMS_PER_PAGE = 4;

    public AllScriptsScreen() {
        super("My QOL Scripts", 300, 280);
        this.allScripts = new ArrayList<>(ScriptManager.getInstance().getAvailableScripts());
    }

    @Override
    public void init() {
        this.clearChildren();
        super.init();
        this.totalPages = (int) Math.ceil((double) this.allScripts.size() / ITEMS_PER_PAGE);
        if (this.totalPages == 0) {
            this.totalPages = 1;
        }

        int listStartX = this.getMiddlePoint().getX() - 100;
        int listStartY = this.getMiddlePoint().getY() - 70;
        int rowHeight = 35;

        int startIndex = this.currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, this.allScripts.size());

        for (int i = startIndex; i < endIndex; i++) {
            ScriptDescriptor descriptor = this.allScripts.get(i);

            int itemIndexOnPage = i - startIndex;
            int currentY = listStartY + (itemIndexOnPage * rowHeight);

            ScriptDescriptorToggleWidget toggleWidget = ScriptDescriptorToggleWidget.builder(descriptor)
                    .position(listStartX, currentY)
                    .build();
            this.addDrawableChild(toggleWidget);
        }

        int navY = this.getMiddlePoint().getY() + 75;

        DarkButtonWidget prevButton = DarkButtonWidget.builder("Previous Page", button -> {
            this.currentPage--;
            this.init();
        }).dimensions(this.getMiddlePoint().getX() - 100, navY, 80, 20).build();
        prevButton.active = this.currentPage > 0;

        DarkButtonWidget nextButton = DarkButtonWidget.builder("Next Page", button -> {
            this.currentPage++;
            this.init();
        }).dimensions(this.getMiddlePoint().getX() + 20, navY, 80, 20).build();
        nextButton.active = this.currentPage < this.totalPages - 1;

        DarkButtonWidget refreshButton = DarkButtonWidget.builder("Refresh", button -> {
            ScriptManager.getInstance().refreshAndReenable();
            this.currentPage = 0;
            this.init();
        }).dimensions(this.getMiddlePoint().getX() - 100, navY + 25, 60, 20).build();

        DarkButtonWidget consoleButton = DarkButtonWidget.builder("See Console", button -> {
            // future implementation for console
        }).dimensions(this.getMiddlePoint().getX() - 35, navY + 25, 70, 20).build();

        DarkButtonWidget offButton = DarkButtonWidget.builder("All" + Formatting.RED + " Off" + Formatting.RESET, button -> {
            ScriptManager sm = ScriptManager.getInstance();
            List<String> runningScriptIds = sm.getRunningScripts().stream()
                    .map(RunningScript::getId)
                    .toList();

            runningScriptIds.forEach(sm::disableScript);

            this.init();
        }).dimensions(this.getMiddlePoint().getX() + 40, navY + 25, 60, 20).build();

        this.addDrawableChild(nextButton);
        this.addDrawableChild(prevButton);
        this.addDrawableChild(refreshButton);
        this.addDrawableChild(consoleButton);
        this.addDrawableChild(offButton);
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawPageNumber(context);
    }

    private void drawPageNumber(DrawContext context) {
        int navY = this.getMiddlePoint().getY() + 75;
        int navCenterX = this.getMiddlePoint().getX();
        String pageText = (currentPage + 1) + " / " + totalPages;
        context.drawCenteredTextWithShadow(this.textRenderer, pageText, navCenterX, navY + 6, Color.WHITE.getRGB());
    }


}
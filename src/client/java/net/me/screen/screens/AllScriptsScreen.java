package net.me.screen.screens;

import net.me.screen.MQSScreen;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.ScriptDescriptor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class AllScriptsScreen extends MQSScreen {

    private final List<ScriptDescriptor> allScripts;
    private int currentPage = 0;
    private int totalPages = 0;
    private static final int ITEMS_PER_PAGE = 6;


    public AllScriptsScreen() {
        super("My QOL Scripts List");
        this.allScripts = new ArrayList<>(ScriptManager.getInstance().getAvailableScripts());
    }

    @Override
    public void initBody() {
        this.totalPages = (int) Math.ceil((double) this.allScripts.size() / ITEMS_PER_PAGE);
        if (this.totalPages == 0) {
            this.totalPages = 1;
        }

        int listStartX = this.width / 2 - 155;
        int listStartY = this.height / 4;
        int scriptEntryWidth = 300;
        int rowHeight = 25;

        int startIndex = this.currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, this.allScripts.size());

        for (int i = startIndex; i < endIndex; i++) {
            ScriptDescriptor descriptor = this.allScripts.get(i);
            boolean isRunning = ScriptManager.getInstance().isRunning(descriptor.getId());

            int itemIndexOnPage = i - startIndex;
            int currentY = listStartY + (itemIndexOnPage * rowHeight);

            Text scriptInfoText = Text.literal(descriptor.moduleName() + " (from " + descriptor.path().getFileName() + ")");
            this.addDrawableChild(new TextWidget(listStartX, currentY, scriptEntryWidth, 20, scriptInfoText, this.textRenderer));

            String status = (isRunning ? Formatting.GREEN + "" + Formatting.BOLD + "ON" : Formatting.RED + "" + Formatting.BOLD + "OFF") + Formatting.RESET;
            Text buttonText = Text.literal(status);
            ButtonWidget actionButton = ButtonWidget.builder(buttonText, button -> {
                if (isRunning) {
                    ScriptManager.getInstance().disableScript(descriptor.getId());
                } else {
                    ScriptManager.getInstance().enableScript(descriptor.getId());
                }
                if (this.client != null) {
                    this.client.setScreen(new AllScriptsScreen());
                }
            }).dimensions(listStartX + scriptEntryWidth + 5, currentY, 60, 20).build();
            this.addDrawableChild(actionButton);
        }

        int navY = listStartY + (ITEMS_PER_PAGE * rowHeight) + 10;
        int navCenterX = this.width / 2;

        ButtonWidget prevButton = ButtonWidget.builder(Text.literal("<-"), button -> {
            this.currentPage--;
            this.init();
        }).dimensions(navCenterX - 55, navY, 20, 20).build();
        prevButton.active = this.currentPage > 0;
        this.addDrawableChild(prevButton);

        ButtonWidget nextButton = ButtonWidget.builder(Text.literal("->"), button -> {
            this.currentPage++;
            this.init();
        }).dimensions(navCenterX + 35, navY, 20, 20).build();
        nextButton.active = this.currentPage < this.totalPages - 1;
        this.addDrawableChild(nextButton);
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawTitle(context);
        drawPageNumber(context);
    }

    private void drawTitle(DrawContext context) {
        float hue = (System.currentTimeMillis()) % 5000;
        int color = Color.HSBtoRGB(hue / 5000, 0.9f, 1f);
        context.getMatrices().push();
        context.getMatrices().scale(1.5F, 1.5F, 1.5F);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("QOL Scripts List"), Math.round((float) this.width / 2 / 1.5F) , Math.round((float)(this.height * 0.05 / 1.5f)), color);
        context.getMatrices().pop();
    }

    private void drawPageNumber(DrawContext context) {
        int listStartY = this.height / 4;
        int rowHeight = 25;
        int navY = listStartY + (ITEMS_PER_PAGE * rowHeight) + 10;
        int navCenterX = this.width / 2;

        String pageText = "Page " + (currentPage + 1) + " / " + totalPages;
        context.drawCenteredTextWithShadow(this.textRenderer, pageText, navCenterX, navY + 6, Color.WHITE.getRGB());
    }


}
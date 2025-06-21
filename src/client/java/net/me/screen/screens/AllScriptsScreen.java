package net.me.screen.screens;

import net.me.screen.MQSScreen;
import net.me.screen.component.components.DarkButtonWidget;
import net.me.screen.component.components.DarkTextFieldWidget;
import net.me.screen.component.components.ScriptDescriptorToggleWidget;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.module.ScriptDescriptor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AllScriptsScreen extends MQSScreen {

    private final List<ScriptDescriptor> allScripts;
    private List<ScriptDescriptor> filteredScripts;
    private int currentPage = 0;
    private int totalPages = 0;
    private static final int ITEMS_PER_PAGE = 4;
    private DarkTextFieldWidget searchTextField;

    public AllScriptsScreen() {
        super("My QOL Scripts", 300, 280);
        this.allScripts = new ArrayList<>(ScriptManager.getInstance().getAvailableScripts());
        this.allScripts.sort(Comparator.comparing(ScriptDescriptor::moduleName, String.CASE_INSENSITIVE_ORDER));
        this.filteredScripts = new ArrayList<>(this.allScripts);
    }

    @Override
    public void init() {
        this.clearChildren();
        super.init();
        addSearch();
        addScriptsList();
        int navY = this.getMiddlePoint().getY() + 75;
        addPagesButtons(navY);
        addOthersButtons(navY);
    }

    private void addSearch() {
        if (this.searchTextField == null) {
            this.searchTextField = DarkTextFieldWidget.builder(this.textRenderer).dimensions( this.getMiddlePoint().getX() - 100, this.getMiddlePoint().getY() - 100, 170, 20).placeholder("Search scripts...").build();
            this.searchTextField.setChangedListener(text -> {
                this.filteredScripts = new ArrayList<>(this.allScripts);
                filteredScripts.removeIf(script -> !script.moduleName().toLowerCase().contains(text.toLowerCase()));
                if (!text.isEmpty()) {
                    currentPage = 0;
                }
                this.init();
            });
        }

        this.addSelectableChild(this.searchTextField);

        DarkButtonWidget clearTextFieldButton = DarkButtonWidget.builder("❌", button -> this.searchTextField.clearText()).dimensions(this.getMiddlePoint().getX() + 80, this.getMiddlePoint().getY() - 100, 20, 20).build();
        this.addDrawableChild(clearTextFieldButton);

    }

    private void addScriptsList() {
        this.totalPages = (int) Math.ceil((double) this.filteredScripts.size() / ITEMS_PER_PAGE);
        if (this.totalPages == 0) {
            this.totalPages = 1;
        }

        int listStartX = this.getMiddlePoint().getX() - 100;
        int listStartY = this.getMiddlePoint().getY() - 70;
        int rowHeight = 35;

        int startIndex = this.currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, this.filteredScripts.size());

        for (int i = startIndex; i < endIndex; i++) {
            ScriptDescriptor descriptor = this.filteredScripts.get(i);

            int itemIndexOnPage = i - startIndex;
            int currentY = listStartY + (itemIndexOnPage * rowHeight);

            ScriptDescriptorToggleWidget toggleWidget = ScriptDescriptorToggleWidget.builder(descriptor)
                    .position(listStartX, currentY)
                    .build();
            this.addDrawableChild(toggleWidget);
        }
    }

    private void addPagesButtons(int navY) {
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
        this.addDrawableChild(nextButton);
        this.addDrawableChild(prevButton);
    }

    private void addOthersButtons(int navY) {
        DarkButtonWidget refreshButton = DarkButtonWidget.builder("Refresh", button -> {
            ScriptManager.getInstance().refreshAndReenable();
            this.currentPage = 0;
            this.init();
        }).dimensions(this.getMiddlePoint().getX() - 100, navY + 25, 60, 20).build();

        DarkButtonWidget consoleButton = DarkButtonWidget.builder("Console", button -> {
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

        this.addDrawableChild(refreshButton);
        this.addDrawableChild(consoleButton);
        this.addDrawableChild(offButton);
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.searchTextField.render(context, mouseX, mouseY, delta);
        drawPageNumber(context);
    }

    private void drawPageNumber(DrawContext context) {
        int navY = this.getMiddlePoint().getY() + 75;
        int navCenterX = this.getMiddlePoint().getX();
        String pageText = (currentPage + 1) + " / " + totalPages;
        context.drawCenteredTextWithShadow(this.textRenderer, pageText, navCenterX, navY + 6, Color.WHITE.getRGB());
    }


}
package net.me.screen.screens;

import net.me.screen.MQSScreen;
import net.me.screen.component.components.DarkButtonWidget;
import net.me.screen.component.components.DarkTextFieldWidget;
import net.me.screen.component.components.ScriptDescriptorToggleWidget;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.module.ScriptDescriptor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AllScriptsScreen extends MQSScreen {

    private static final int ITEMS_PER_PAGE = 4;
    private static final int SCRIPT_ROW_HEIGHT = 35;
    private static final int PADDING = 100;
    private static final int SEARCH_BAR_WIDTH = 170;
    private static final int SEARCH_BAR_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;

    private final List<ScriptDescriptor> allScripts;
    private List<ScriptDescriptor> filteredScripts;
    private int currentPage = 0;
    private int totalPages = 1;

    private DarkTextFieldWidget searchTextField;
    private DarkButtonWidget prevButton;
    private DarkButtonWidget nextButton;

    private final List<ClickableWidget> scriptEntryWidgets = new ArrayList<>();

    public AllScriptsScreen() {
        super("My QOL Scripts", 300, 280);
        this.allScripts = new ArrayList<>(ScriptManager.getInstance().getAvailableScripts());
        this.allScripts.sort(Comparator.comparing(ScriptDescriptor::moduleName, String.CASE_INSENSITIVE_ORDER));
        this.filteredScripts = new ArrayList<>(this.allScripts);
    }

    @Override
    public void init() {
        super.init();

        addSearch();
        addNavigationAndActionButtons();

        updateScriptList();
    }

    private void addSearch() {
        if (this.searchTextField == null) {
            int searchX = this.getMiddlePoint().getX() - PADDING;
            int searchY = this.getMiddlePoint().getY() - PADDING;

            this.searchTextField = DarkTextFieldWidget.builder(this.textRenderer)
                    .dimensions(searchX, searchY, SEARCH_BAR_WIDTH, SEARCH_BAR_HEIGHT)
                    .placeholder("Search...")
                    .build();

            this.searchTextField.setChangedListener(this::onSearchTextChanged);

            DarkButtonWidget clearTextFieldButton = DarkButtonWidget.builder("❌", button -> this.searchTextField.clearText())
                    .dimensions(searchX + SEARCH_BAR_WIDTH + 10, searchY, SEARCH_BAR_HEIGHT, SEARCH_BAR_HEIGHT)
                    .build();

            this.addSelectableChild(this.searchTextField);
            this.addDrawableChild(clearTextFieldButton);
        }
    }

    private void onSearchTextChanged(String text) {
        String searchText = text.toLowerCase();
        this.filteredScripts = this.allScripts.stream()
                .filter(script -> script.moduleName().toLowerCase().contains(searchText))
                .collect(Collectors.toList());

        this.currentPage = 0;

        updateScriptList();
    }

    private void updateScriptList() {
        this.scriptEntryWidgets.forEach(this::remove);
        this.scriptEntryWidgets.clear();

        this.totalPages = (int) Math.ceil((double) this.filteredScripts.size() / ITEMS_PER_PAGE);
        if (this.totalPages == 0) {
            this.totalPages = 1;
        }
        this.currentPage = Math.max(0, Math.min(this.currentPage, this.totalPages - 1));

        int listStartX = this.getMiddlePoint().getX() - PADDING;
        int listStartY = this.getMiddlePoint().getY() - 70;

        int startIndex = this.currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, this.filteredScripts.size());

        for (int i = startIndex; i < endIndex; i++) {
            ScriptDescriptor descriptor = this.filteredScripts.get(i);
            int itemIndexOnPage = i - startIndex;
            int currentY = listStartY + (itemIndexOnPage * SCRIPT_ROW_HEIGHT);

            ScriptDescriptorToggleWidget toggleWidget = ScriptDescriptorToggleWidget.builder(descriptor)
                    .position(listStartX, currentY)
                    .build();

            this.addDrawableChild(toggleWidget);
            this.scriptEntryWidgets.add(toggleWidget);
        }

        updateNavigationButtons();
    }

    private void addNavigationAndActionButtons() {
        int navY = this.getMiddlePoint().getY() + 75;
        int navX = this.getMiddlePoint().getX();

        this.prevButton = DarkButtonWidget.builder("Previous", button -> {
            if (this.currentPage > 0) {
                this.currentPage--;
                updateScriptList();
            }
        }).dimensions(navX - PADDING, navY, 80, BUTTON_HEIGHT).build();

        this.nextButton = DarkButtonWidget.builder("Next", button -> {
            if (this.currentPage < this.totalPages - 1) {
                this.currentPage++;
                updateScriptList();
            }
        }).dimensions(navX + 20, navY, 80, BUTTON_HEIGHT).build();

        this.addDrawableChild(this.prevButton);
        this.addDrawableChild(this.nextButton);

        int actionY = navY + 25;

        DarkButtonWidget refreshButton = DarkButtonWidget.builder("Refresh", button -> refreshScripts())
                .dimensions(navX - PADDING, actionY, 60, BUTTON_HEIGHT).build();

        DarkButtonWidget consoleButton = DarkButtonWidget.builder("Console", button -> new ConsoleScreen().open()).dimensions(navX - 35, actionY, 70, BUTTON_HEIGHT).build();

        DarkButtonWidget offButton = DarkButtonWidget.builder("All" + Formatting.RED + " Off", button -> disableAllScripts())
                .dimensions(navX + 40, actionY, 60, BUTTON_HEIGHT).build();

        this.addDrawableChild(refreshButton);
        this.addDrawableChild(consoleButton);
        this.addDrawableChild(offButton);
    }

    private void updateNavigationButtons() {
        if(this.prevButton != null) this.prevButton.active = this.currentPage > 0;
        if(this.nextButton != null) this.nextButton.active = this.currentPage < this.totalPages - 1;
    }

    private void refreshScripts() {
        ScriptManager.getInstance().refreshAndReenable();
        this.allScripts.clear();
        this.allScripts.addAll(ScriptManager.getInstance().getAvailableScripts());
        this.allScripts.sort(Comparator.comparing(ScriptDescriptor::moduleName, String.CASE_INSENSITIVE_ORDER));

        onSearchTextChanged(this.searchTextField.getText());
    }

    private void disableAllScripts() {
        ScriptManager sm = ScriptManager.getInstance();

        List<String> runningScriptIds = sm.getRunningScripts().stream()
                .map(RunningScript::getId)
                .toList();

        runningScriptIds.forEach(sm::disableScript);

        updateScriptList();
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
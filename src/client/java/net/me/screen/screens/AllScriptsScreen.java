package net.me.screen.screens;

import net.me.Main;
import net.me.console.ConsoleManager;
import net.me.screen.MQSScreen;
import net.me.screen.component.WidgetLayoutHelper;
import net.me.screen.component.components.MQSButtonWidget;
import net.me.screen.component.components.MQSImageButtonWidget;
import net.me.screen.component.components.MQSTextFieldWidget;
import net.me.screen.component.components.ScriptDescriptorToggleWidget;
import net.me.scripting.ScriptingService;
import net.me.scripting.module.ScriptDescriptor;
import net.me.utils.GUIColors;
import net.me.utils.TextRenderUtils;
import net.me.utils.UIConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AllScriptsScreen extends MQSScreen {

    private static final int ITEMS_PER_PAGE = 4;
    private static final int SCRIPT_ROW_HEIGHT = 35;
    private static final int WINDOW_HORIZONTAL_MARGIN = 100;
    private static final int SEARCH_BAR_WIDTH = 175;

    private final ScriptingService scriptingService;
    private final ConsoleManager consoleManager;

    private final List<ScriptDescriptor> allScripts;
    private final List<ScriptDescriptorToggleWidget> scriptEntryWidgets = new ArrayList<>();
    private List<ScriptDescriptor> filteredScripts;
    private int currentPage = 0;
    private int totalPages = 1;
    private MQSTextFieldWidget searchTextField;
    private MQSButtonWidget prevButton;
    private MQSButtonWidget nextButton;
    private MQSImageButtonWidget refreshButton;
    private boolean isRefreshing = false;
    private long refreshFinishTime = -1L;


    public AllScriptsScreen(ScriptingService scriptingService, ConsoleManager consoleManager) {
        super("My QOL Scripts", 260, 280);
        this.scriptingService = scriptingService;
        this.consoleManager = consoleManager;
        this.allScripts = new ArrayList<>(scriptingService.listAvailable());
        this.allScripts.sort(Comparator.comparing(ScriptDescriptor::moduleName, String.CASE_INSENSITIVE_ORDER));
        this.filteredScripts = new ArrayList<>(this.allScripts);
    }

    @Override
    public void init() {
        this.scriptEntryWidgets.clear();
        super.init();

        addSearch();
        addNavigationAndActionButtons();
        addScriptListWidgets();

        updateScriptList();
    }

    private void addScriptListWidgets() {
        int listStartX = this.getMiddlePoint().x() - WINDOW_HORIZONTAL_MARGIN;
        int listStartY = this.getMiddlePoint().y() - 70;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            ScriptDescriptorToggleWidget toggleWidget = ScriptDescriptorToggleWidget.builder(scriptingService)
                    .size(200, SCRIPT_ROW_HEIGHT - 5)
                    .build();
            toggleWidget.visible = false;
            this.addDrawableChild(toggleWidget);
            this.scriptEntryWidgets.add(toggleWidget);
        }

        WidgetLayoutHelper.layoutVertically(
                listStartX,
                listStartY,
                5,
                this.scriptEntryWidgets.toArray(new ScriptDescriptorToggleWidget[0])
        );
    }

    private void addSearch() {
        int searchX = this.getMiddlePoint().x() - WINDOW_HORIZONTAL_MARGIN;
        int searchY = this.getMiddlePoint().y() - WINDOW_HORIZONTAL_MARGIN;

        this.searchTextField = MQSTextFieldWidget.builder()
                .dimensions(searchX, searchY, SEARCH_BAR_WIDTH, UIConstants.BUTTON_HEIGHT)
                .placeholder("Search...")
                .build();

        this.searchTextField.setChangedListener(this::onSearchTextChanged);

        this.addSelectableChild(this.searchTextField);
        MQSButtonWidget clearTextFieldButton = MQSImageButtonWidget.builder(Identifier.of(Main.MOD_ID, "icons/close.png"), button -> this.searchTextField.clearText())
                .dimensions(searchX + SEARCH_BAR_WIDTH + 5, searchY, UIConstants.BUTTON_HEIGHT, UIConstants.BUTTON_HEIGHT)
                .build();
        this.addDrawableChild(clearTextFieldButton);
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
        this.totalPages = (int) Math.ceil((double) this.filteredScripts.size() / ITEMS_PER_PAGE);
        if (this.totalPages == 0) {
            this.totalPages = 1;
        }
        this.currentPage = Math.max(0, Math.min(this.currentPage, this.totalPages - 1));

        int startIndex = this.currentPage * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int scriptIndex = startIndex + i;
            ScriptDescriptorToggleWidget widget = this.scriptEntryWidgets.get(i);

            if (scriptIndex < this.filteredScripts.size()) {
                ScriptDescriptor descriptor = this.filteredScripts.get(scriptIndex);
                widget.update(descriptor);
            } else {
                widget.update(null);
            }
        }

        updateNavigationButtons();
    }

    private void addNavigationAndActionButtons() {
        int navY = this.getMiddlePoint().y() + 75;
        int navX = this.getMiddlePoint().x();

        this.prevButton = MQSButtonWidget.builder("Previous", button -> {
            if (this.currentPage > 0) {
                this.currentPage--;
                updateScriptList();
            }
        }).dimensions(navX - WINDOW_HORIZONTAL_MARGIN, navY, 80, UIConstants.BUTTON_HEIGHT).build();

        this.nextButton = MQSButtonWidget.builder("Next", button -> {
            if (this.currentPage < this.totalPages - 1) {
                this.currentPage++;
                updateScriptList();
            }
        }).dimensions(navX + 20, navY, 80, UIConstants.BUTTON_HEIGHT).build();

        this.addDrawableChild(this.prevButton);
        this.addDrawableChild(this.nextButton);

        int actionY = navY + 25;

        this.refreshButton = MQSImageButtonWidget.builder(Identifier.of(Main.MOD_ID, "icons/refresh-ccw.png"), "Refresh", button -> refreshScripts())
                .dimensions(navX - WINDOW_HORIZONTAL_MARGIN, actionY, 65, UIConstants.BUTTON_HEIGHT).build();

        MQSButtonWidget consoleButton = MQSImageButtonWidget.builder(Identifier.of(Main.MOD_ID, "icons/square-terminal.png"), "Console", button -> new ConsoleScreen(this, consoleManager).open())
                .dimensions(navX - 30, actionY, 65, UIConstants.BUTTON_HEIGHT).build();

        MQSButtonWidget offButton = MQSButtonWidget.builder(MessageFormat.format("All{0} Off", Formatting.RED), button -> disableAllScripts())
                .dimensions(navX + 40, actionY, 40, UIConstants.BUTTON_HEIGHT).build();

        MQSButtonWidget moreButton = MQSImageButtonWidget.builder(Identifier.of(Main.MOD_ID, "icons/ellipsis-vertical.png"), button -> new MoreOptionsScreen(this).open())
                .dimensions(navX + 85, actionY, 15, UIConstants.BUTTON_HEIGHT)
                .build();

        this.addDrawableChild(this.refreshButton);
        this.addDrawableChild(consoleButton);
        this.addDrawableChild(offButton);
        this.addDrawableChild(moreButton);
    }

    private void updateNavigationButtons() {
        if (this.prevButton != null) this.prevButton.active = this.currentPage > 0;
        if (this.nextButton != null) this.nextButton.active = this.currentPage < this.totalPages - 1;
    }

    private void refreshScripts() {
        if (isRefreshing) {
            return;
        }
        isRefreshing = true;
        if (this.refreshButton != null) {
            this.refreshButton.active = false;
            this.refreshButton.setMessage(Text.literal("Refreshing..."));
            this.allScripts.clear();
            this.filteredScripts.clear();
        }

        assert this.client != null;
        this.client.send(() -> {
            scriptingService.refreshAndReenable();

            this.allScripts.clear();
            this.allScripts.addAll(scriptingService.listAvailable());
            this.allScripts.sort(Comparator.comparing(ScriptDescriptor::moduleName, String.CASE_INSENSITIVE_ORDER));

            onSearchTextChanged(this.searchTextField.getText());

            isRefreshing = false;
            if (this.refreshButton != null) {
                this.refreshButton.active = true;
                this.refreshButton.setImage(null);
                this.refreshButton.setMessage(Text.literal("Refreshed!"));
                this.refreshFinishTime = System.currentTimeMillis();
            }
        });
    }

    private void disableAllScripts() {
        scriptingService.disableAll();
        updateScriptList();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (refreshFinishTime != -1L && System.currentTimeMillis() - refreshFinishTime > 1000L) {
            if (this.refreshButton != null) {
                this.refreshButton.setImage(Identifier.of(Main.MOD_ID, "icons/refresh-ccw.png"));
                this.refreshButton.active = true;
                this.refreshButton.setMessage(Text.literal("Refresh"));
            }
            refreshFinishTime = -1L;
        }

        super.render(context, mouseX, mouseY, delta);
        this.searchTextField.render(context, mouseX, mouseY, delta);

        if (filteredScripts.isEmpty()) {
            TextRenderUtils.drawCustomCenteredText(context, "No modules found.",
                    this.getMiddlePoint().x(),
                    this.getMiddlePoint().y() - 10,
                    GUIColors.TEXT_DISABLED.getRGB(), true, 1.1f);
        }
        drawPageNumber(context);

    }

    private void drawPageNumber(DrawContext context) {
        int navY = this.getMiddlePoint().y() + 86;
        int navCenterX = this.getMiddlePoint().x();
        String pageText = (currentPage + 1) + " / " + totalPages;
        TextRenderUtils.drawCustomCenteredText(context, pageText, navCenterX, navY, Color.WHITE.getRGB(), true, 1.0f);
    }
}
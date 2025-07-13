/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.me.screen.screens;


import net.me.Main;
import net.me.category.Category;
import net.me.category.CategoryManager;
import net.me.config.GlobalConfigManager;
import net.me.console.ConsoleManager;
import net.me.screen.MQSScreen;
import net.me.screen.component.components.*;
import net.me.screen.screens.viewmodel.AllScriptsViewModel;
import net.me.screen.theme.GUIColors;
import net.me.screen.theme.UIConstants;
import net.me.scripting.ScriptingService;
import net.me.scripting.module.ScriptDescriptor;
import net.me.utils.AssetIdentifiers;
import net.me.utils.Render2DUtils;
import net.me.utils.TextRenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AllScriptsScreen extends MQSScreen {

    private static final int SIDEBAR_WIDTH = 100;
    private static final int SEPARATOR_WIDTH = 2;
    private static final int PADDING = 8;
    private static final int SCRIPT_ROW_HEIGHT = 35;
    private static final int CONTENT_TOP_OFFSET = 43;
    private static final int LIST_TOP_OFFSET = 70;
    private static final int LIST_HEIGHT_REDUCTION = 87;
    private static final int BUTTON_SPACING = 4;
    private static final int CATEGORY_BUTTON_SPACING = 5;
    private static final int SEARCH_REFRESH_SPACING = 9;
    private static final int REFRESH_COOLDOWN_MS = 1500;
    private static final double SCROLL_SPEED_MULTIPLIER = 0.5;

    private final AllScriptsViewModel viewModel;
    private final ConsoleManager consoleManager;
    private final GlobalConfigManager globalConfigManager;
    private final ScriptingService scriptingService;
    private final CategoryManager categoryManager;

    private final List<ScriptDescriptorToggleWidget> scriptEntryWidgets = new ArrayList<>();
    private final List<MQSButtonWidget> scrollableCategoryButtons = new ArrayList<>();
    private MQSButtonWidget allScriptsButton;
    private MQSTextFieldWidget searchTextField;
    private MQSImageButtonWidget refreshButton;
    private ScrollbarWidget scriptListScrollbar;
    private ScrollbarWidget categoryScrollbar;
    private double scriptListScrollY = 0;
    private double categoryScrollY = 0;
    private int totalScriptContentHeight = 0;
    private int totalCategoryContentHeight = 0;
    private long refreshFinishTime = -1L;

    private int windowStartX, windowStartY, mainContentX, listStartY, listRenderHeight, listWidth;
    private int categoryListStartY, categoryListRenderHeight, separatorY;

    public AllScriptsScreen(ScriptingService scriptingService, ConsoleManager consoleManager, GlobalConfigManager globalConfigManager) {
        super("My QOL Scripts", 384, 216);
        this.scriptingService = scriptingService;
        this.consoleManager = consoleManager;
        this.globalConfigManager = globalConfigManager;
        this.categoryManager = Main.getInstance().getCategoryManager();
        this.viewModel = new AllScriptsViewModel(scriptingService, Main.getInstance().getConfigManager());
    }


    @Override
    protected void init() {
        super.init();
        clearChildren();
        scriptEntryWidgets.clear();
        scrollableCategoryButtons.clear();
        this.categoryScrollbar = null;

        calculateLayoutValues();
        rebuildUI();
    }

    private void rebuildUI() {
        rebuildSidebar();
        createHeaderWidgets();
        rebuildScriptListWidgets();
        updateCategoryButtonStyles();
    }

    private void calculateLayoutValues() {
        windowStartX = getMiddlePoint().x() - getWindowWidth() / 2;
        windowStartY = getMiddlePoint().y() - getWindowHeight() / 2;
        mainContentX = windowStartX + SIDEBAR_WIDTH + PADDING;
        listStartY = windowStartY + LIST_TOP_OFFSET;
        listRenderHeight = getWindowHeight() - LIST_HEIGHT_REDUCTION;
        listWidth = getWindowWidth() - SIDEBAR_WIDTH - (PADDING * 3) - UIConstants.SCROLLBAR_WIDTH;
    }

    private void rebuildSidebar() {
        setupStaticSidebarElements();
        rebuildCategoryList();
    }

    private void setupStaticSidebarElements() {
        int contentTopY = windowStartY + CONTENT_TOP_OFFSET;
        int allScriptButtonWidth = SIDEBAR_WIDTH - (PADDING * 5);
        int buttonX = windowStartX + PADDING;

        this.allScriptsButton = MQSButtonWidget.mqsBuilder("All Scripts", btn -> {
                    viewModel.setSelectedCategory(AllScriptsViewModel.ALL_SCRIPTS_ID);
                    rebuildScriptListWidgets();
                })
                .dimensions(buttonX, contentTopY, allScriptButtonWidth, UIConstants.BUTTON_HEIGHT)
                .build();
        addDrawableChild(allScriptsButton);

        MQSImageButtonWidget manageButton = MQSImageButtonWidget.mqsBuilder(AssetIdentifiers.ICON_EDIT, btn -> new CategoryManagementScreen(this).open())
                .dimensions(buttonX + allScriptButtonWidth + 5, contentTopY, 20, UIConstants.BUTTON_HEIGHT)
                .build();
        addDrawableChild(manageButton);

        this.separatorY = contentTopY + UIConstants.BUTTON_HEIGHT + PADDING;
    }

    private void rebuildCategoryList() {
        scrollableCategoryButtons.clear();
        this.categoryListStartY = separatorY + SEPARATOR_WIDTH + PADDING;
        this.categoryListRenderHeight = windowStartY + getWindowHeight() - categoryListStartY - PADDING * 2;

        List<Category> sortedCategories = new ArrayList<>(categoryManager.getAllCategories());
        sortedCategories.sort(Comparator.comparing(Category::name, String.CASE_INSENSITIVE_ORDER));

        if (sortedCategories.isEmpty()) {
            this.totalCategoryContentHeight = 0;
            return;
        }

        totalCategoryContentHeight = sortedCategories.size() * (UIConstants.BUTTON_HEIGHT + CATEGORY_BUTTON_SPACING);
        int buttonX = windowStartX + PADDING;
        int buttonWidth = SIDEBAR_WIDTH - (PADDING * 3);

        for (Category category : sortedCategories) {
            MQSButtonWidget button = MQSButtonWidget.mqsBuilder(category.name(), btn -> {
                        viewModel.setSelectedCategory(category);
                        rebuildScriptListWidgets();
                    })
                    .dimensions(buttonX, 0, buttonWidth, UIConstants.BUTTON_HEIGHT)
                    .build();
            scrollableCategoryButtons.add(button);
            addSelectableChild(button);
        }

        int scrollbarX = windowStartX + SIDEBAR_WIDTH - PADDING - UIConstants.SCROLLBAR_WIDTH;
        categoryScrollbar = new ScrollbarWidget(scrollbarX, categoryListStartY, UIConstants.SCROLLBAR_WIDTH, categoryListRenderHeight, newScroll -> categoryScrollY = newScroll);
        addDrawableChild(categoryScrollbar);
    }

    private void createHeaderWidgets() {
        int headerY = windowStartY + CONTENT_TOP_OFFSET;
        createSearchField(headerY);
        createActionButtons(headerY);
    }

    private void createSearchField(int headerY) {
        searchTextField = MQSTextFieldWidget.builder()
                .position(mainContentX, headerY)
                .size(155, UIConstants.BUTTON_HEIGHT)
                .placeholder("Search..")
                .text(viewModel.getSearchText())
                .build();

        searchTextField.setChangedListener(newText -> {
            viewModel.onSearchTextChanged(newText);
            rebuildScriptListWidgets();
        });

        addSelectableChild(searchTextField);
    }

    private void createActionButtons(int headerY) {
        int buttonX = searchTextField.getX() + searchTextField.getWidth() + SEARCH_REFRESH_SPACING;
        int buttonSize = UIConstants.BUTTON_HEIGHT;

        ActionButtonConfig[] buttons = {
                new ActionButtonConfig(AssetIdentifiers.ICON_REFRESH, this::refreshScripts),
                new ActionButtonConfig(AssetIdentifiers.ICON_TERMINAL, () -> new ConsoleScreen(this, consoleManager).open(), GUIColors.PRIMARY.getColor()),
                new ActionButtonConfig(AssetIdentifiers.ICON_STOP, viewModel::disableAllScripts, GUIColors.ERROR.getColor()),
                new ActionButtonConfig(AssetIdentifiers.ICON_MORE_OPTIONS, () -> new MoreOptionsScreen(this).open())
        };

        for (int i = 0; i < buttons.length; i++) {
            ActionButtonConfig config = buttons[i];
            int x = buttonX + i * (buttonSize + BUTTON_SPACING);

            MQSImageButtonWidget.Builder builder = MQSImageButtonWidget.mqsBuilder(config.icon, btn -> config.action.run())
                    .dimensions(x, headerY, buttonSize, buttonSize);

            if (config.color() != null) builder.imageColor(config.color());

            MQSImageButtonWidget button = builder.build();
            if (i == 0) refreshButton = button;

            addDrawableChild(button);
        }
    }

    private void rebuildScriptListWidgets() {
        scriptEntryWidgets.forEach(this::remove);
        scriptEntryWidgets.clear();

        if (scriptListScrollbar != null) remove(scriptListScrollbar);

        List<ScriptDescriptor> scripts = viewModel.getFilteredScripts();
        totalScriptContentHeight = scripts.size() * SCRIPT_ROW_HEIGHT;

        for (ScriptDescriptor descriptor : scripts) {
            ScriptDescriptorToggleWidget toggleWidget = new ScriptDescriptorToggleWidget.Builder(scriptingService, globalConfigManager)
                    .descriptor(descriptor)
                    .size(listWidth, SCRIPT_ROW_HEIGHT - 5)
                    .build();
            scriptEntryWidgets.add(toggleWidget);
            addSelectableChild(toggleWidget);
        }

        createScriptListScrollbar();
        updateCategoryButtonStyles();
    }

    private void createScriptListScrollbar() {
        int scrollbarX = mainContentX + listWidth + PADDING;
        scriptListScrollbar = new ScrollbarWidget(scrollbarX, listStartY, UIConstants.SCROLLBAR_WIDTH, listRenderHeight, newScroll -> scriptListScrollY = newScroll);
        addDrawableChild(scriptListScrollbar);
    }

    private void updateCategoryButtonStyles() {
        Object selected = viewModel.getSelectedCategory();
        Style primaryColorStyle = Style.EMPTY.withColor(GUIColors.PRIMARY.getRGB());

        boolean allScriptsSelected = selected.equals(AllScriptsViewModel.ALL_SCRIPTS_ID);
        allScriptsButton.setMessage(Text.literal("All Scripts").fillStyle(allScriptsSelected ? primaryColorStyle : Style.EMPTY));

        List<Category> sortedCategories = new ArrayList<>(categoryManager.getAllCategories());
        sortedCategories.sort(Comparator.comparing(Category::name, String.CASE_INSENSITIVE_ORDER));

        for (int i = 0; i < scrollableCategoryButtons.size(); i++) {
            if (i >= sortedCategories.size()) break;
            MQSButtonWidget button = scrollableCategoryButtons.get(i);
            Category category = sortedCategories.get(i);
            boolean isSelected = category.equals(selected);
            button.setMessage(Text.literal(category.name()).fillStyle(isSelected ? primaryColorStyle : Style.EMPTY));
        }
    }

    private void refreshScripts() {
        if (viewModel.isRefreshing) return;

        refreshButton.setImageColor(GUIColors.SUCCESS.getColor());
        viewModel.isRefreshing = true;
        refreshButton.active = false;

        if (client != null) {
            client.send(() -> {
                try {
                    viewModel.refreshAndReenableScripts();
                    rebuildUI();
                    refreshFinishTime = System.currentTimeMillis();
                } finally {
                    viewModel.isRefreshing = false;
                }
            });
        }
    }

    public void forceRefresh() {
        refreshScripts();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        renderSidebar(context, mouseX, mouseY, delta);
        renderScriptList(context, mouseX, mouseY, delta);
        searchTextField.render(context, mouseX, mouseY, delta);
        updateRefreshButtonState();
    }

    private void renderSidebar(DrawContext context, int mouseX, int mouseY, float delta) {
        int separatorX = windowStartX + SIDEBAR_WIDTH;
        int contentTopY = windowStartY + CONTENT_TOP_OFFSET;
        Render2DUtils.drawRoundedRect(context, separatorX, contentTopY - 3, SEPARATOR_WIDTH, getWindowHeight() - 52, 3, 5, GUIColors.DARK_L3.getRGB());
        Render2DUtils.drawRoundedRect(context, windowStartX + PADDING - 2, separatorY, SIDEBAR_WIDTH - (PADDING * 2), SEPARATOR_WIDTH, 3, 5, GUIColors.DARK_L3.getRGB());

        if (scrollableCategoryButtons.isEmpty()) {
            TextRenderUtils.drawCustomCenteredText(context, "No categories", windowStartX + SIDEBAR_WIDTH / 2f, categoryListStartY + categoryListRenderHeight / 2f - 4, GUIColors.TEXT_DISABLED.getRGB(), true, UIConstants.TEXT_SCALE);
            return;
        }

        if (categoryScrollbar != null) {
            categoryScrollbar.update(totalCategoryContentHeight, categoryListRenderHeight, categoryScrollY);
        }

        context.enableScissor(windowStartX, categoryListStartY, windowStartX + SIDEBAR_WIDTH, categoryListStartY + categoryListRenderHeight);
        int currentY = (int) (categoryListStartY - categoryScrollY);
        for (MQSButtonWidget button : scrollableCategoryButtons) {
            button.setY(currentY);
            boolean isVisible = isWidgetVisible(button, categoryListStartY, categoryListRenderHeight);
            button.visible = isVisible;
            if (isVisible) {
                button.render(context, mouseX, mouseY, delta);
            }
            currentY += UIConstants.BUTTON_HEIGHT + CATEGORY_BUTTON_SPACING;
        }
        context.disableScissor();
    }

    private void renderScriptList(DrawContext context, int mouseX, int mouseY, float delta) {
        scriptListScrollbar.update(totalScriptContentHeight, listRenderHeight, scriptListScrollY);

        context.enableScissor(mainContentX, listStartY, mainContentX + listWidth + UIConstants.SCROLLBAR_WIDTH + PADDING, listStartY + listRenderHeight);
        int currentY = (int) (listStartY - scriptListScrollY);
        for (ScriptDescriptorToggleWidget widget : scriptEntryWidgets) {
            widget.setPos(mainContentX, currentY);
            boolean isVisible = isWidgetVisible(widget, listStartY, listRenderHeight);
            widget.visible = isVisible;
            widget.setClickable(isMouseInClickableArea(mouseX, mouseY));
            if (isVisible) {
                widget.render(context, mouseX, mouseY, delta);
            }
            currentY += SCRIPT_ROW_HEIGHT;
        }
        context.disableScissor();

        if (viewModel.hasNoFilteredScripts()) {
            renderNoModulesMessage(context);
        }
    }
    private boolean isMouseInClickableArea(double mouseX, double mouseY) {
        return mouseX >= mainContentX && mouseX <= mainContentX + listWidth &&
                mouseY >= listStartY && mouseY <= listStartY + listRenderHeight;
    }

    private void renderNoModulesMessage(DrawContext context) {
        String message = "No modules found.";
        if (viewModel.getSelectedCategory() instanceof Category cat) {
            message = "No modules in '" + cat.name() + "'";
        }
        TextRenderUtils.drawCustomCenteredText(context, message, mainContentX + listWidth / 2f, listStartY + listRenderHeight / 2f - 10, GUIColors.TEXT_DISABLED.getRGB(), true, UIConstants.TEXT_SCALE);
    }

    private void updateRefreshButtonState() {
        if (refreshFinishTime != -1L && System.currentTimeMillis() - refreshFinishTime > REFRESH_COOLDOWN_MS) {
            refreshButton.active = true;
            refreshButton.setImageColor(Color.WHITE);
            refreshFinishTime = -1L;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isMouseInSidebarScrollableArea(mouseX, mouseY)) {
            handleCategoryScrolling(verticalAmount);
            return true;
        }
        if (isMouseInScriptListArea(mouseX, mouseY)) {
            handleScriptListScrolling(verticalAmount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void handleCategoryScrolling(double verticalAmount) {
        if (categoryScrollbar == null) return;
        double maxScroll = Math.max(0, totalCategoryContentHeight - categoryListRenderHeight);
        categoryScrollY -= verticalAmount * (UIConstants.BUTTON_HEIGHT + CATEGORY_BUTTON_SPACING) * SCROLL_SPEED_MULTIPLIER;
        categoryScrollY = Math.max(0, Math.min(categoryScrollY, maxScroll));
    }

    private void handleScriptListScrolling(double verticalAmount) {
        double maxScroll = Math.max(0, totalScriptContentHeight - listRenderHeight);
        scriptListScrollY -= verticalAmount * SCRIPT_ROW_HEIGHT * SCROLL_SPEED_MULTIPLIER;
        scriptListScrollY = Math.max(0, Math.min(scriptListScrollY, maxScroll));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (categoryScrollbar != null && categoryScrollbar.isMouseOver(mouseX, mouseY)) {
            return categoryScrollbar.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        if (scriptListScrollbar.isMouseOver(mouseX, mouseY)) {
            return scriptListScrollbar.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (categoryScrollbar != null && categoryScrollbar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (scriptListScrollbar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isWidgetVisible(ClickableWidget widget, int listTopY, int listHeight) {
        return widget.getY() + widget.getHeight() > listTopY && widget.getY() < listTopY + listHeight;
    }

    private boolean isMouseInScriptListArea(double mouseX, double mouseY) {
        return mouseX >= mainContentX && mouseX <= mainContentX + listWidth + 20 &&
                mouseY >= listStartY && mouseY <= listStartY + listRenderHeight;
    }

    private boolean isMouseInSidebarScrollableArea(double mouseX, double mouseY) {
        return mouseX >= windowStartX && mouseX < windowStartX + SIDEBAR_WIDTH &&
                mouseY >= categoryListStartY && mouseY < categoryListStartY + categoryListRenderHeight;
    }

    private record ActionButtonConfig(Identifier icon, Runnable action, Color color) {
        ActionButtonConfig(Identifier icon, Runnable action) {
            this(icon, action, null);
        }
    }
}
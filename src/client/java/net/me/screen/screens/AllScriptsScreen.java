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

// java/net/me/screen/screens/AllScriptsScreen.java
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
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class AllScriptsScreen extends MQSScreen {

    private static final int SIDEBAR_WIDTH = 100;
    private static final int SEPARATOR_WIDTH = 2;
    private static final int PADDING = 8;
    private static final int SCRIPT_ROW_HEIGHT = 35;
    private static final int HEADER_HEIGHT = 20;
    private static final int CONTENT_TOP_OFFSET = 45;
    private static final int LIST_TOP_OFFSET = 70;
    private static final int LIST_HEIGHT_REDUCTION = 87;
    private static final int BUTTON_SPACING = 4;
    private static final int CATEGORY_BUTTON_SPACING = 5;
    private static final int SEARCH_REFRESH_SPACING = 9;
    private static final int REFRESH_COOLDOWN_MS = 1500;
    private static final double SCROLL_SPEED_MULTIPLIER = 0.5;
    private static final float CATEGORY_TEXT_SCALE = 1.1f;

    private final AllScriptsViewModel viewModel;
    private final ConsoleManager consoleManager;
    private final GlobalConfigManager globalConfigManager;
    private final ScriptingService scriptingService;

    private final List<ScriptDescriptorToggleWidget> scriptEntryWidgets = new ArrayList<>();
    private MQSTextFieldWidget searchTextField;
    private MQSImageButtonWidget refreshButton;
    private ScrollbarWidget scrollbar;

    private double scrollY = 0;
    private int totalContentHeight = 0;
    private long refreshFinishTime = -1L;

    private int windowStartX;
    private int windowStartY;
    private int mainContentX;
    private int listStartY;
    private int listRenderHeight;
    private int listWidth;

    public AllScriptsScreen(ScriptingService scriptingService, ConsoleManager consoleManager, GlobalConfigManager globalConfigManager) {
        super("My QOL Scripts", 384, 216);
        this.scriptingService = scriptingService;
        this.consoleManager = consoleManager;
        this.globalConfigManager = globalConfigManager;
        this.viewModel = new AllScriptsViewModel(scriptingService);
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();
        scriptEntryWidgets.clear();

        calculateLayoutValues();
        createSidebarWidgets();
        createHeaderWidgets();
        rebuildScriptListWidgets();
    }

    private void calculateLayoutValues() {
        windowStartX = getMiddlePoint().x() - getWindowWidth() / 2;
        windowStartY = getMiddlePoint().y() - getWindowHeight() / 2;
        mainContentX = windowStartX + SIDEBAR_WIDTH + PADDING;
        listStartY = windowStartY + LIST_TOP_OFFSET;
        listRenderHeight = getWindowHeight() - LIST_HEIGHT_REDUCTION;
        listWidth = getWindowWidth() - SIDEBAR_WIDTH - (PADDING * 3) - UIConstants.SCROLLBAR_WIDTH;
    }

    private void createSidebarWidgets() {
        int contentTopY = windowStartY + CONTENT_TOP_OFFSET;
        int buttonWidth = SIDEBAR_WIDTH - (PADDING * 2);
        int buttonX = windowStartX + PADDING;

        MQSButtonWidget category1Button = createCategoryButton("Category 1", buttonX, contentTopY + 25, buttonWidth);
        MQSButtonWidget category2Button = createCategoryButton("Category 2",
                buttonX, category1Button.getY() + category1Button.getHeight() + CATEGORY_BUTTON_SPACING, buttonWidth);

        MQSButtonWidget newCategoryButton = createCategoryButton("New",
                buttonX, windowStartY + getWindowHeight() - PADDING - UIConstants.BUTTON_HEIGHT, buttonWidth);

        addDrawableChild(category1Button);
        addDrawableChild(category2Button);
        addDrawableChild(newCategoryButton);
    }

    private MQSButtonWidget createCategoryButton(String text, int x, int y, int width) {
        MQSButtonWidget.Builder builder = MQSButtonWidget.mqsBuilder(text, btn -> {/* TODO: Implement category selection */})
                .dimensions(x, y, width, UIConstants.BUTTON_HEIGHT);

        if ("Category 1".equals(text)) {
            builder.backgroundColors(GUIColors.DARK_L3.getRGB(), GUIColors.DARK_L4.getRGB());
        }

        return builder.build();
    }

    private void createHeaderWidgets() {
        int headerY = windowStartY + CONTENT_TOP_OFFSET - 2;

        createSearchField(headerY);
        createActionButtons(headerY);
    }


    private void createSearchField(int headerY) {
        searchTextField = MQSTextFieldWidget.builder()
                .position(mainContentX, headerY)
                .size(155, HEADER_HEIGHT)
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
        int buttonSize = HEADER_HEIGHT;

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

            if (config.color() != null) {
                builder.imageColor(config.color());
            }

            MQSImageButtonWidget button = builder.build();

            if (i == 0) {
                refreshButton = button;
            }

            addDrawableChild(button);
        }
    }

    private void rebuildScriptListWidgets() {
        scriptEntryWidgets.forEach(this::remove);
        scriptEntryWidgets.clear();

        if (scrollbar != null) {
            remove(scrollbar);
        }

        List<ScriptDescriptor> scripts = viewModel.getFilteredScripts();
        totalContentHeight = scripts.size() * SCRIPT_ROW_HEIGHT;

        for (ScriptDescriptor descriptor : scripts) {
            ScriptDescriptorToggleWidget toggleWidget = new ScriptDescriptorToggleWidget.Builder(scriptingService, globalConfigManager)
                    .descriptor(descriptor)
                    .size(listWidth, SCRIPT_ROW_HEIGHT - 5)
                    .build();

            scriptEntryWidgets.add(toggleWidget);
            addSelectableChild(toggleWidget);
        }

        createScrollbar();
    }

    private void createScrollbar() {
        int scrollbarX = mainContentX + listWidth + PADDING;
        scrollbar = new ScrollbarWidget(scrollbarX, listStartY, UIConstants.SCROLLBAR_WIDTH,
                listRenderHeight, newScroll -> scrollY = newScroll);
        addDrawableChild(scrollbar);
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
                    rebuildScriptListWidgets();
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

        renderSidebar(context);
        renderScriptList(context, mouseX, mouseY, delta);
        renderSearchField(context, mouseX, mouseY, delta);

        updateRefreshButtonState();
    }


    private void renderSidebar(DrawContext context) {
        int contentTopY = windowStartY + CONTENT_TOP_OFFSET;
        int separatorX = windowStartX + SIDEBAR_WIDTH;

        Render2DUtils.drawRoundedRect(context, separatorX, contentTopY - 5, SEPARATOR_WIDTH,
                getWindowHeight() - 52, 3, 5, GUIColors.DARK_L3.getRGB());

        TextRenderUtils.drawCustomText(context, "Categories", windowStartX + PADDING, contentTopY,
                GUIColors.TEXT.getRGB(), true, CATEGORY_TEXT_SCALE);
    }

    private void renderScriptList(DrawContext context, int mouseX, int mouseY, float delta) {
        scrollbar.update(totalContentHeight, listRenderHeight, scrollY);

        context.enableScissor(mainContentX, listStartY,
                mainContentX + listWidth + UIConstants.SCROLLBAR_WIDTH + PADDING,
                listStartY + listRenderHeight);

        int currentY = (int) (listStartY - scrollY);
        for (ScriptDescriptorToggleWidget widget : scriptEntryWidgets) {
            widget.setPos(mainContentX, currentY);

            if (isWidgetVisible(widget, listStartY, listRenderHeight)) {
                widget.render(context, mouseX, mouseY, delta);
            }

            currentY += SCRIPT_ROW_HEIGHT;
        }

        context.disableScissor();

        if (viewModel.hasNoFilteredScripts()) {
            renderNoModulesMessage(context);
        }
    }

    private boolean isWidgetVisible(ScriptDescriptorToggleWidget widget, int listStartY, int listRenderHeight) {
        return widget.getY() + widget.getHeight() > listStartY &&
                widget.getY() < listStartY + listRenderHeight;
    }

    private void renderNoModulesMessage(DrawContext context) {
        TextRenderUtils.drawCustomCenteredText(context, "No modules found.",
                mainContentX + (float) listWidth / 2,
                listStartY + (float) listRenderHeight / 2 - 10,
                GUIColors.TEXT_DISABLED.getRGB(), true, UIConstants.TEXT_SCALE);
    }

    private void renderSearchField(DrawContext context, int mouseX, int mouseY, float delta) {
        searchTextField.render(context, mouseX, mouseY, delta);
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
        if (isMouseInClickableArea(mouseX, mouseY)) {
            handleScrolling(verticalAmount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void handleScrolling(double verticalAmount) {
        double maxScroll = Math.max(0, totalContentHeight - listRenderHeight);
        scrollY -= verticalAmount * SCRIPT_ROW_HEIGHT * SCROLL_SPEED_MULTIPLIER;
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isMouseInClickableArea(mouseX, mouseY) || scrollbar.isMouseOver(mouseX, mouseY)) {
            return scrollbar.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollbar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        updateScriptWidgetClickability(mouseX, mouseY);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void updateScriptWidgetClickability(double mouseX, double mouseY) {
        boolean isInClickableArea = isMouseInClickableArea(mouseX, mouseY);

        for (ScriptDescriptorToggleWidget widget : scriptEntryWidgets) {
            widget.setClickable(isInClickableArea);
        }
    }

    private boolean isMouseInClickableArea(double mouseX, double mouseY) {
        return mouseX >= mainContentX && mouseX <= mainContentX + listWidth + 20 &&
                mouseY >= listStartY && mouseY <= listStartY + listRenderHeight;
    }

    private record ActionButtonConfig(Identifier icon, Runnable action, Color color) {
        public ActionButtonConfig(Identifier icon, Runnable action) {
            this(icon, action, null);
        }
    }
}
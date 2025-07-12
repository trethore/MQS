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
import net.me.screen.component.WidgetLayoutHelper;
import net.me.screen.component.components.MQSButtonWidget;
import net.me.screen.component.components.MQSImageButtonWidget;
import net.me.screen.component.components.MQSTextFieldWidget;
import net.me.screen.component.components.ScriptDescriptorToggleWidget;
import net.me.screen.screens.viewmodel.AllScriptsViewModel;
import net.me.screen.theme.GUIColors;
import net.me.screen.theme.UIConstants;
import net.me.scripting.ScriptingService;
import net.me.scripting.module.ScriptDescriptor;
import net.me.utils.AssetIdentifiers;
import net.me.utils.TextRenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AllScriptsScreen extends MQSScreen {

    private static final int WINDOW_HORIZONTAL_MARGIN = 100;
    private static final int SEARCH_BAR_WIDTH = 175;
    private static final int SCRIPT_ROW_HEIGHT = 35;
    private static final int ITEMS_PER_PAGE = 4;

    private final AllScriptsViewModel viewModel;
    private final ConsoleManager consoleManager;
    private final GlobalConfigManager globalConfigManager;
    private final ScriptingService scriptingService;

    private final List<ScriptDescriptorToggleWidget> scriptEntryWidgets = new ArrayList<>();
    private MQSTextFieldWidget searchTextField;
    private MQSButtonWidget prevButton;
    private MQSButtonWidget nextButton;
    private MQSImageButtonWidget refreshButton;
    private long refreshFinishTime = -1L;

    public AllScriptsScreen(ScriptingService scriptingService, ConsoleManager consoleManager, GlobalConfigManager globalConfigManager) {
        super("My QOL Scripts", 260, 280);
        this.scriptingService = scriptingService;
        this.consoleManager = consoleManager;
        this.globalConfigManager = globalConfigManager;
        this.viewModel = new AllScriptsViewModel(scriptingService);
    }

    @Override
    public void init() {
        super.init();
        this.scriptEntryWidgets.clear();

        createSearchWidgets();
        createScriptListWidgets();
        createPagingWidgets();
        createActionWidgets();
    }

    private void createSearchWidgets() {
        int searchX = this.getMiddlePoint().x() - WINDOW_HORIZONTAL_MARGIN;
        int searchY = this.getMiddlePoint().y() - WINDOW_HORIZONTAL_MARGIN;

        this.searchTextField = new MQSTextFieldWidget.Builder()
                .dimensions(searchX, searchY, SEARCH_BAR_WIDTH, UIConstants.BUTTON_HEIGHT)
                .placeholder("Search...")
                .text(viewModel.getSearchText())
                .build();

        this.searchTextField.setChangedListener(viewModel::onSearchTextChanged);

        this.addSelectableChild(this.searchTextField);
        MQSButtonWidget clearTextFieldButton = MQSImageButtonWidget.mqsBuilder(
                AssetIdentifiers.ICON_CLOSE,
                button -> this.searchTextField.setText("")
        ).dimensions(searchX + SEARCH_BAR_WIDTH + 5, searchY, UIConstants.BUTTON_HEIGHT, UIConstants.BUTTON_HEIGHT).build();
        this.addDrawableChild(clearTextFieldButton);
    }

    private void createScriptListWidgets() {
        int listStartX = this.getMiddlePoint().x() - WINDOW_HORIZONTAL_MARGIN;
        int listStartY = this.getMiddlePoint().y() - 70;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            ScriptDescriptorToggleWidget toggleWidget = new ScriptDescriptorToggleWidget.Builder(scriptingService, globalConfigManager)
                    .size(200, SCRIPT_ROW_HEIGHT - 5)
                    .build();
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

    private void createPagingWidgets() {
        int navY = this.getMiddlePoint().y() + 75;
        int navX = this.getMiddlePoint().x();

        this.prevButton = MQSButtonWidget.mqsBuilder("Previous", button -> viewModel.previousPage())
                .dimensions(navX - WINDOW_HORIZONTAL_MARGIN, navY, 80, UIConstants.BUTTON_HEIGHT).build();

        this.nextButton = MQSButtonWidget.mqsBuilder("Next", button -> viewModel.nextPage())
                .dimensions(navX + 20, navY, 80, UIConstants.BUTTON_HEIGHT).build();

        this.addDrawableChild(this.prevButton);
        this.addDrawableChild(this.nextButton);
    }

    private void createActionWidgets() {
        int actionY = this.getMiddlePoint().y() + 75 + 25;
        int navX = this.getMiddlePoint().x();

        this.refreshButton = MQSImageButtonWidget.mqsBuilder(AssetIdentifiers.ICON_REFRESH, "Refresh", button -> refreshScripts())
                .dimensions(navX - WINDOW_HORIZONTAL_MARGIN, actionY, 65, UIConstants.BUTTON_HEIGHT).build();

        MQSButtonWidget consoleButton = MQSImageButtonWidget.mqsBuilder(AssetIdentifiers.ICON_TERMINAL, "Console", button -> new ConsoleScreen(this, consoleManager).open())
                .dimensions(navX - 30, actionY, 65, UIConstants.BUTTON_HEIGHT).build();

        Text offButtonText = Text.literal("All ").append(Text.literal("Off").formatted(Formatting.RED));
        MQSButtonWidget offButton = MQSButtonWidget.mqsBuilder(offButtonText, button -> viewModel.disableAllScripts())
                .dimensions(navX + 40, actionY, 40, UIConstants.BUTTON_HEIGHT).build();

        MQSButtonWidget moreButton = MQSImageButtonWidget.mqsBuilder(AssetIdentifiers.ICON_MORE_OPTIONS, button -> new MoreOptionsScreen(this).open())
                .dimensions(navX + 85, actionY, 15, UIConstants.BUTTON_HEIGHT)
                .build();

        this.addDrawableChild(this.refreshButton);
        this.addDrawableChild(consoleButton);
        this.addDrawableChild(offButton);
        this.addDrawableChild(moreButton);
    }

    private void refreshScripts() {
        if (viewModel.isRefreshing) return;

        this.refreshButton.active = false;
        this.refreshButton.setMessage(Text.literal("Refreshing..."));

        assert this.client != null;
        this.client.send(() -> {
            viewModel.refreshAndReenableScripts();
            this.refreshButton.active = true;
            this.refreshButton.setImage(null);
            this.refreshButton.setMessage(Text.literal("Refreshed!"));
            this.refreshFinishTime = System.currentTimeMillis();
        });
    }

    public void forceRefresh() {
        viewModel.forceRefresh();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        updateWidgetsFromViewModel();

        this.searchTextField.render(context, mouseX, mouseY, delta);

        if (viewModel.hasNoFilteredScripts()) {
            drawEmptyListMessage(context);
        }

        drawPageNumber(context);
    }

    private void updateWidgetsFromViewModel() {
        List<ScriptDescriptor> pageScripts = viewModel.getScriptsForCurrentPage();
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            ScriptDescriptorToggleWidget widget = this.scriptEntryWidgets.get(i);
            if (i < pageScripts.size()) {
                widget.update(pageScripts.get(i));
            } else {
                widget.update(null);
            }
        }

        prevButton.active = viewModel.isPreviousButtonActive();
        nextButton.active = viewModel.isNextButtonActive();

        if (refreshFinishTime != -1L && System.currentTimeMillis() - refreshFinishTime > 1000L) {
            this.refreshButton.setImage(AssetIdentifiers.ICON_REFRESH);
            this.refreshButton.setMessage(Text.literal("Refresh"));
            refreshFinishTime = -1L;
        }
        this.refreshButton.active = !viewModel.isRefreshing;
    }

    private void drawEmptyListMessage(DrawContext context) {
        TextRenderUtils.drawCustomCenteredText(context, "No modules found :(",
                this.getMiddlePoint().x(),
                this.getMiddlePoint().y() - 25,
                GUIColors.TEXT_DISABLED.getRGB(), true, UIConstants.TEXT_SCALE);
        TextRenderUtils.drawCustomCenteredText(context, "Maybe try refreshing.",
                this.getMiddlePoint().x(),
                this.getMiddlePoint().y() - 10,
                GUIColors.TEXT_DISABLED.getRGB(), true, UIConstants.TEXT_SCALE);
    }

    private void drawPageNumber(DrawContext context) {
        int navY = this.getMiddlePoint().y() + 86;
        int navCenterX = this.getMiddlePoint().x();
        TextRenderUtils.drawCustomCenteredText(context, viewModel.getPageNumberText(), navCenterX, navY, Color.WHITE.getRGB(), true, 1.0f);
    }
}
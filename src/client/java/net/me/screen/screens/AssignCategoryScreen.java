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
import net.me.screen.MQSScreen;
import net.me.screen.component.components.CategorySelectionEntryWidget;
import net.me.screen.component.components.MQSButtonWidget;
import net.me.screen.component.components.MQSToast;
import net.me.screen.component.components.ScrollbarWidget;
import net.me.screen.theme.UIConstants;
import net.me.scripting.ConfigManager;
import net.me.scripting.module.ScriptDescriptor;
import net.minecraft.client.gui.DrawContext;

import java.util.*;
import java.util.stream.Collectors;

public class AssignCategoryScreen extends MQSScreen {
    private static final int PADDING = 20;
    private static final int HEADER_MARGIN = 55;
    private static final int FOOTER_MARGIN = 45;
    private static final int ENTRY_HEIGHT = 30;
    private static final int WIDGET_WIDTH = 250;

    private final ScriptDescriptor scriptDescriptor;
    private final ConfigManager configManager;
    private final CategoryManager categoryManager;

    private final List<CategorySelectionEntryWidget> categoryWidgets = new ArrayList<>();

    private double scrollY = 0;
    private int totalContentHeight = 0;

    public AssignCategoryScreen(MQSScreen parent, ScriptDescriptor scriptDescriptor) {
        super("Assign Categories", 300, 250, parent);
        this.scriptDescriptor = scriptDescriptor;
        this.configManager = Main.getInstance().getConfigManager();
        this.categoryManager = Main.getInstance().getCategoryManager();
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();
        categoryWidgets.clear();

        int windowStartX = getMiddlePoint().x() - getWindowWidth() / 2;
        int windowStartY = getMiddlePoint().y() - getWindowHeight() / 2;
        int listStartY = windowStartY + HEADER_MARGIN;
        int listHeight = getWindowHeight() - HEADER_MARGIN - FOOTER_MARGIN;

        Set<String> currentCategoryIds = configManager.getScriptCategoryIds(scriptDescriptor.getId());

        List<Category> allCategories = new ArrayList<>(categoryManager.getAllCategories());
        allCategories.sort(Comparator.comparing(Category::name, String.CASE_INSENSITIVE_ORDER));

        totalContentHeight = allCategories.size() * (ENTRY_HEIGHT + 5);

        for (Category category : allCategories) {
            boolean isSelected = currentCategoryIds.contains(category.id().toString());
            CategorySelectionEntryWidget widget = new CategorySelectionEntryWidget(
                    windowStartX + PADDING, 0, WIDGET_WIDTH, ENTRY_HEIGHT, category, isSelected
            );
            categoryWidgets.add(widget);
            addSelectableChild(widget);
        }

        int scrollbarX = windowStartX + getWindowWidth() - PADDING;
        ScrollbarWidget scrollbar = new ScrollbarWidget(scrollbarX, listStartY, UIConstants.SCROLLBAR_WIDTH, listHeight, (newScroll) -> this.scrollY = newScroll);
        addDrawableChild(scrollbar);
        scrollbar.update(totalContentHeight, listHeight, scrollY);

        MQSButtonWidget confirmButton = MQSButtonWidget.mqsBuilder("Confirm", button -> saveAndClose())
                .dimensions(windowStartX + PADDING, windowStartY + getWindowHeight() - FOOTER_MARGIN + 5, WIDGET_WIDTH, UIConstants.BUTTON_HEIGHT)
                .build();
        addDrawableChild(confirmButton);
    }

    private void saveAndClose() {
        Set<String> selectedIds = categoryWidgets.stream()
                .filter(CategorySelectionEntryWidget::isToggled)
                .map(w -> w.getCategory().id().toString())
                .collect(Collectors.toSet());

        configManager.setScriptCategoryIds(scriptDescriptor.getId(), selectedIds);
        MQSToast.show("Categories Updated", "Script assignments have been saved.", 2000, MQSToast.Corner.TOP_LEFT);
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int windowStartX = getMiddlePoint().x() - getWindowWidth() / 2;
        int windowStartY = getMiddlePoint().y() - getWindowHeight() / 2;
        int listStartY = windowStartY + HEADER_MARGIN;
        int listHeight = getWindowHeight() - HEADER_MARGIN - FOOTER_MARGIN;

        context.enableScissor(windowStartX, listStartY, windowStartX + getWindowWidth(), listStartY + listHeight);

        int currentY = (int) (listStartY - scrollY);
        for (CategorySelectionEntryWidget widget : categoryWidgets) {
            widget.setPos(windowStartX + PADDING, currentY);
            widget.render(context, mouseX, mouseY, delta);
            currentY += ENTRY_HEIGHT + 5;
        }

        context.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double maxScroll = Math.max(0, totalContentHeight - (getWindowHeight() - HEADER_MARGIN - FOOTER_MARGIN));
        scrollY -= verticalAmount * 10;
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));
        return true;
    }
}
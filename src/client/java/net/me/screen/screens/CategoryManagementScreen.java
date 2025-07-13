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
import net.me.screen.component.components.CategoryEntryWidget;
import net.me.screen.component.components.MQSButtonWidget;
import net.me.screen.component.components.ScrollbarWidget;
import net.me.screen.theme.UIConstants;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CategoryManagementScreen extends MQSScreen {
    private static final int PADDING = 20;
    private static final int HEADER_MARGIN = 55;
    private static final int FOOTER_MARGIN = 45; // Changed from 25 to 45
    private static final int ENTRY_HEIGHT = 30;
    private static final int WIDGET_WIDTH = 250;

    private final CategoryManager categoryManager;
    private final List<CategoryEntryWidget> categoryWidgets = new ArrayList<>();
    private ScrollbarWidget scrollbar;

    private double scrollY = 0;
    private int totalContentHeight = 0;

    public CategoryManagementScreen(MQSScreen parent) {
        super("Manage Categories", 300, 250, parent);
        this.categoryManager = Main.getInstance().getCategoryManager();
    }

    @Override
    protected void init() {
        super.init();
        rebuildList();
    }

    public void rebuildList() {
        clearChildren();
        categoryWidgets.clear();

        int windowStartX = getMiddlePoint().x() - getWindowWidth() / 2;
        int windowStartY = getMiddlePoint().y() - getWindowHeight() / 2;
        int listStartY = windowStartY + HEADER_MARGIN;
        int listHeight = getWindowHeight() - HEADER_MARGIN - FOOTER_MARGIN;

        MQSButtonWidget createButton = MQSButtonWidget.mqsBuilder("Create New Category", button -> new EditCategoryScreen(this, null).open())
                .dimensions(windowStartX + PADDING, windowStartY + getWindowHeight() - FOOTER_MARGIN + 5, WIDGET_WIDTH, UIConstants.BUTTON_HEIGHT)
                .build();
        addDrawableChild(createButton);

        List<Category> categories = new ArrayList<>(categoryManager.getAllCategories());
        categories.sort(Comparator.comparing(Category::name, String.CASE_INSENSITIVE_ORDER));

        totalContentHeight = categories.size() * ENTRY_HEIGHT;

        for (Category category : categories) {
            CategoryEntryWidget widget = new CategoryEntryWidget(
                    windowStartX + PADDING,
                    0,
                    WIDGET_WIDTH,
                    ENTRY_HEIGHT,
                    category,
                    this::editCategory,
                    this::deleteCategory
            );
            categoryWidgets.add(widget);
            addSelectableChild(widget);
        }

        int scrollbarX = windowStartX + getWindowWidth() - PADDING;
        scrollbar = new ScrollbarWidget(scrollbarX, listStartY, UIConstants.SCROLLBAR_WIDTH, listHeight, (newScroll) -> this.scrollY = newScroll);
        addDrawableChild(scrollbar);
        scrollbar.update(totalContentHeight, listHeight, scrollY);
    }

    private void editCategory(Category category) {
        new EditCategoryScreen(this, category).open();
    }

    private void deleteCategory(Category category) {
        categoryManager.deleteCategory(category.id());
        rebuildList();
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
        for (CategoryEntryWidget widget : categoryWidgets) {
            widget.setPos(windowStartX + PADDING, currentY);
            if (widget.getY() + widget.getHeight() > listStartY && widget.getY() < listStartY + listHeight) {
                widget.render(context, mouseX, mouseY, delta);
            }
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

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (scrollbar.isMouseOver(mouseX, mouseY)) {
            return scrollbar.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollbar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
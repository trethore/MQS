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

package net.me.screen.component.components;

import net.me.category.Category;
import net.me.screen.component.AbstractToggleEntryWidget;

public class CategorySelectionEntryWidget extends AbstractToggleEntryWidget {

    private final Category category;
    private boolean selected;

    public CategorySelectionEntryWidget(int x, int y, int width, int height, Category category, boolean isSelected) {
        super(x, y, width, height);
        this.category = category;
        this.selected = isSelected;
    }

    @Override
    public boolean isToggled() {
        return this.selected;
    }

    @Override
    protected String getPrimaryText() {
        return category.name();
    }

    @Override
    protected String getSecondaryText() {
        return "";
    }

    @Override
    public void onPress() {
        this.selected = !this.selected;
    }

    public Category getCategory() {
        return category;
    }
}
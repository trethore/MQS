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

package net.me.ui;

import net.minecraft.util.math.ColorHelper;

public final class UIConstants {

    // Dimensions
    public static final int PADDING = 16;
    public static final int COMPONENT_SPACING = 8;
    public static final int TITLE_HEIGHT = 14;
    public static final int BUTTON_HEIGHT = 20;
    public static final int BUTTON_WIDTH_LARGE = 180;
    public static final int BUTTON_WIDTH_MEDIUM = 120;
    public static final int BUTTON_WIDTH_SMALL = 80;
    // Colors
    public static final int TEXT_COLOR_WHITE = 0xFFFFFFFF;
    public static final int TEXT_COLOR_GRAY = 0xFFa3a3a3;
    public static final int TEXT_COLOR_DARK_GRAY = 0xA0A0A0;
    public static final int STATUS_ON_COLOR = ColorHelper.getArgb(255, 56, 255, 70);
    public static final int STATUS_OFF_COLOR = ColorHelper.getArgb(255, 255, 75, 75);
    public static final int STATUS_BORDER_COLOR = ColorHelper.getArgb(255, 144, 144, 144);
    public static final int STATUS_BORDER_COLOR_HIGHLIGHT = ColorHelper.getArgb(255, 168, 168, 168);
    public static final int ENTRY_BORDER_COLOR = ColorHelper.getArgb(255, 168, 168, 168);
    public static final int HEADER_TEXT_COLOR = TEXT_COLOR_WHITE;
    public static final int HEADER_LINE_COLOR = TEXT_COLOR_GRAY;
    private UIConstants() {
    }
}

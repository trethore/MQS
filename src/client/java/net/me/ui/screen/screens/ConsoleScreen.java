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

package net.me.ui.screen.screens;

import net.me.ui.screen.MQSScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConsoleScreen extends MQSScreen {

    public ConsoleScreen(Screen parent) {
        super(parent, Text.translatable("screen.mqs.scripts.console"));
    }

    @Override
    protected void init() {
        super.init();
        // TODO: Implement console UI
    }
}

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

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import net.me.Main;
import tytoo.minegui.helper.constraint.constraints.Constraints;
import tytoo.minegui.helper.window.Window;
import tytoo.minegui.view.View;


public class ScriptView extends View {

    public ScriptView() {
        super(Main.MOD_ID, "script_view", true);
    }

    @Override
    protected void renderView() {
        final int POS = 100;
        final int SIZE = 200;
        Window.of(this, "My QOL Scripts")
                .initDimensions(Constraints.pixels(SIZE), Constraints.pixels(SIZE))
                .initPos(Constraints.pixels(POS), Constraints.pixels(POS))
                .flags(ImGuiWindowFlags.NoCollapse)
                .render(() -> {
                    ImGui.text("Hello from My QOL Scripts!");
                });
    }


}

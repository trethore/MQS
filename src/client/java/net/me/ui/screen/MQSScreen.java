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

package net.me.ui.screen;

import net.me.Main;
import net.me.utils.McUtils;
import net.me.utils.math.Vec2i;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class MQSScreen extends Screen {

    private Screen parent;

    protected MQSScreen(Text title) {
        super(Text.of(Main.MOD_ID + ": " + title));
    }

    protected MQSScreen(String title) {
        this(Text.of(title));
    }

    protected MQSScreen(Screen parent, Text title) {
        this(title);
        this.parent = parent;
    }

    protected MQSScreen(Screen parent, String title) {
        this(parent, Text.of(title));
    }

    public void open() {
        McUtils.getMc().ifPresentOrElse(
                mc -> mc.setScreen(this)
                , () -> {
                    throw new IllegalStateException(
                            "Minecraft instance is not available while trying to open a MQSScreen: " + this.title.getString()
                    );
                });
    }

    @Override
    public void close() {
        closeToParent();
    }

    public void closeToParent() {
        McUtils.getMc().ifPresentOrElse(
                mc -> mc.setScreen(parent)
                , () -> {
                    throw new IllegalStateException(
                            "Minecraft instance is not available while trying to close a MQSScreen with parent: " + this.title.getString()
                    );
                }
        );
    }

    protected Vec2i getMiddle() {
        return new Vec2i(this.width / 2, this.height / 2);
    }

}

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

package net.me.screen.component.builders;

import net.me.screen.component.MQSBaseWidget;

@SuppressWarnings({"unchecked", "unused"})
public abstract class BaseBuilder<T extends MQSBaseWidget, B extends BaseBuilder<T, B>> {
    protected int x = 0;
    protected int y = 0;
    protected int width = 200;
    protected int height = 20;

    public B dimensions(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return (B) this;
    }

    public B position(int x, int y) {
        this.x = x;
        this.y = y;
        return (B) this;
    }

    public B size(int width, int height) {
        this.width = width;
        this.height = height;
        return (B) this;
    }

    public abstract T build();
}
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

import net.me.screen.component.AbstractToggleEntryWidget;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class BooleanSettingEntryWidget extends AbstractToggleEntryWidget {

    private final String name;
    private final String description;
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    protected BooleanSettingEntryWidget(int x, int y, int width, int height, String name, String description, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        super(x, y, width, height);
        this.name = name;
        this.description = description;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    protected boolean isToggled() {
        return this.getter.get();
    }

    @Override
    protected String getPrimaryText() {
        return this.name;
    }

    @Override
    protected String getSecondaryText() {
        return this.description;
    }

    @Override
    public void onPress() {
        if (this.setter != null) {
            this.setter.accept(!this.getter.get());
        }
    }

    public static class Builder {
        private String name = "";
        private String description = "";
        private Supplier<Boolean> getter = () -> false;
        private Consumer<Boolean> setter = (b) -> {
        };
        private int x = 0;
        private int y = 0;
        private int width = 200;
        private int height = 40;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder getter(Supplier<Boolean> getter) {
            this.getter = getter;
            return this;
        }

        public Builder setter(Consumer<Boolean> setter) {
            this.setter = setter;
            return this;
        }

        public Builder dimensions(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public BooleanSettingEntryWidget build() {
            return new BooleanSettingEntryWidget(this.x, this.y, this.width, this.height, this.name, this.description, this.getter, this.setter);
        }
    }
}
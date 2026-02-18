/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Réthoré
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

package net.me.utils;

import net.me.Main;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;

@SuppressWarnings("unused")
public final class AssetIdentifiers {

    // --- Icons ---
    public static final Identifier ICON_CLOSE = Identifier.fromNamespaceAndPath(Main.MOD_ID, "icons/close.png");
    public static final Identifier ICON_REFRESH = Identifier.fromNamespaceAndPath(Main.MOD_ID, "icons/refresh-ccw.png");
    public static final Identifier ICON_TERMINAL = Identifier.fromNamespaceAndPath(Main.MOD_ID, "icons/square-terminal.png");
    public static final Identifier ICON_MORE_OPTIONS = Identifier.fromNamespaceAndPath(Main.MOD_ID, "icons/ellipsis-vertical.png");
    public static final Identifier ICON_SETTINGS = Identifier.fromNamespaceAndPath(Main.MOD_ID, "icons/settings.png");
    public static final Identifier ICON_KEYBOARD = Identifier.fromNamespaceAndPath(Main.MOD_ID, "icons/keyboard.png");
    public static final Identifier ICON_VSCODE = Identifier.fromNamespaceAndPath(Main.MOD_ID, "icons/app-window.png");
    public static final Identifier ICON_CREATE_SCRIPT = Identifier.fromNamespaceAndPath(Main.MOD_ID, "icons/file-code.png");
    public static final Identifier ICON_OPEN_FOLDER = Identifier.fromNamespaceAndPath(Main.MOD_ID, "icons/file-sliders.png");
    public static final Identifier ICON_WIKI = Identifier.fromNamespaceAndPath(Main.MOD_ID, "icons/cog.png");
    public static final Identifier ICON_STOP = Identifier.fromNamespaceAndPath(Main.MOD_ID, "icons/ban.png");
    public static final Identifier ICON_TAG = Identifier.fromNamespaceAndPath(Main.MOD_ID, "icons/tag.png");
    public static final Identifier ICON_EDIT = Identifier.fromNamespaceAndPath(Main.MOD_ID, "icons/pencil.png");

    // --- Fonts ---
    public static final Identifier FONT_MQS = Identifier.fromNamespaceAndPath(Main.MOD_ID, "mqsfont.ttf");


    // --- URLs ---
    public static final String URL_WIKI = "https://github.com/trethore/MQS/wiki";
    public static final String URL_GITHUB_API_BASE = "https://api.github.com/repos/";
    public static final String GITHUB_REPO = "trethore/MQS";


    private AssetIdentifiers() {
    }

    public static Optional<Identifier> getIcon(String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }
        try {
            Field field = AssetIdentifiers.class.getField(name);
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == Identifier.class) {
                return Optional.ofNullable((Identifier) field.get(null));
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
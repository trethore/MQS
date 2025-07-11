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

package net.me.utils;

import net.me.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.*;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TextRendererUtils {
    private final static Identifier FONT_ID = Identifier.of(Main.MOD_ID, "mqsfont.ttf");
    private static TextRenderer customTextRenderer;

    public static TextRenderer getCustomTextRenderer() {
        if (customTextRenderer == null) {
            customTextRenderer = McUtils.getMc().map(mc -> {
                try {
                    float fontSize = 10.0f;
                    float oversample = 8.0f;

                    TrueTypeFontLoader.Loadable loadable = new TrueTypeFontLoader(FONT_ID, fontSize, oversample, TrueTypeFontLoader.Shift.NONE, "").build().orThrow();
                    Font font = loadable.load(mc.getResourceManager());
                    List<Font.FontFilterPair> list = new ArrayList<>();
                    list.add(new Font.FontFilterPair(font, new FontFilterType.FilterMap(Collections.emptyMap())));

                    FontStorage storage = new FontStorage(mc.getTextureManager(), FONT_ID);
                    storage.setFonts(list, Collections.emptySet());

                    return new TextRenderer(id -> storage, true);

                } catch (Exception e) {
                    Main.LOGGER.error("Failed to load custom font. It may be missing or corrupt. Falling back to default.", e);
                    return mc.textRenderer;
                }
            }).orElseGet(() -> {
                Main.LOGGER.warn("Could not get MinecraftClient instance to load font. Using fallback.");
                return MinecraftClient.getInstance().textRenderer;
            });
        }
        return customTextRenderer;
    }
}

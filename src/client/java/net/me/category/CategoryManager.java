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

package net.me.category;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.me.Main;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CategoryManager {
    private static final Path CATEGORIES_FILE = Main.MOD_DIR.resolve("categories.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type CATEGORY_LIST_TYPE = new TypeToken<List<Category>>() {
    }.getType();

    private final Map<UUID, Category> categories = new ConcurrentHashMap<>();

    public void init() {
        load();
    }

    public void load() {
        if (!Files.exists(CATEGORIES_FILE)) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(CATEGORIES_FILE.toFile())) {
            List<Category> loadedCategories = GSON.fromJson(reader, CATEGORY_LIST_TYPE);
            if (loadedCategories != null) {
                categories.clear();
                for (Category category : loadedCategories) {
                    categories.put(category.id(), category);
                }
            }
        } catch (Exception e) {
            Main.LOGGER.error("Failed to load categories, using defaults.", e);
            categories.clear();
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CATEGORIES_FILE.toFile())) {
            GSON.toJson(new ArrayList<>(categories.values()), writer);
        } catch (IOException e) {
            Main.LOGGER.error("Failed to save categories.", e);
        }
    }

    public List<Category> getAllCategories() {
        return new ArrayList<>(categories.values());
    }

    public void addCategory(Category category) {
        categories.put(category.id(), category);
        save();
    }

    public void deleteCategory(UUID id) {
        if (categories.remove(id) != null) {
            save();
        }
    }
}
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

package net.me.screen.screens;

import net.me.Main;
import net.me.category.Category;
import net.me.category.CategoryManager;
import net.me.screen.MQSScreen;
import net.me.screen.component.WidgetLayoutHelper;
import net.me.screen.component.components.MQSButtonWidget;
import net.me.screen.component.components.MQSTextFieldWidget;
import net.me.screen.theme.GUIColors;
import net.me.screen.theme.UIConstants;
import net.me.utils.AssetIdentifiers;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class EditCategoryScreen extends MQSScreen {
    private final CategoryManager categoryManager;
    private final Category existingCategory;

    private MQSTextFieldWidget nameField;
    private MQSTextFieldWidget iconField;
    private MQSTextFieldWidget errorField;

    public EditCategoryScreen(MQSScreen parent, @Nullable Category existingCategory) {
        super(existingCategory == null ? "Create Category" : "Edit Category", 250, 180, parent);
        this.categoryManager = Main.getInstance().getCategoryManager();
        this.existingCategory = existingCategory;
    }

    @Override
    protected void init() {
        super.init();

        this.nameField = MQSTextFieldWidget.builder()
                .placeholder("Category Name")
                .size(UIConstants.WIDGET_WIDTH_STANDARD, UIConstants.BUTTON_HEIGHT)
                .text(existingCategory != null ? existingCategory.name() : "")
                .build();

        this.iconField = MQSTextFieldWidget.builder()
                .placeholder("Icon (e.g., ICON_SETTINGS)")
                .size(UIConstants.WIDGET_WIDTH_STANDARD, UIConstants.BUTTON_HEIGHT)
                .text(existingCategory != null ? existingCategory.iconIdentifier() : "")
                .build();

        this.errorField = MQSTextFieldWidget.builder()
                .size(UIConstants.WIDGET_WIDTH_STANDARD, UIConstants.BUTTON_HEIGHT)
                .build();
        errorField.setEditable(false);
        errorField.setVisible(false);

        MQSButtonWidget saveButton = MQSButtonWidget.mqsBuilder("Save Category", button -> saveCategory())
                .size(UIConstants.WIDGET_WIDTH_STANDARD, UIConstants.BUTTON_HEIGHT)
                .build();

        this.addSelectableChild(nameField);
        this.addSelectableChild(iconField);
        this.addDrawableChild(errorField);
        this.addDrawableChild(saveButton);

        WidgetLayoutHelper.layoutVertically(
                getMiddlePoint().x() - UIConstants.WIDGET_WIDTH_STANDARD / 2,
                getMiddlePoint().y() - 40,
                UIConstants.WIDGET_SPACING,
                nameField,
                iconField,
                saveButton
        );

        errorField.setPos(saveButton.getX(), saveButton.getY() + saveButton.getHeight() + UIConstants.WIDGET_SPACING);
    }

    private void saveCategory() {
        Optional<String> validationError = validateInput();
        if (validationError.isPresent()) {
            setError(validationError.get());
            return;
        }

        UUID id = (existingCategory != null) ? existingCategory.id() : UUID.randomUUID();
        Category newCategory = new Category(id, nameField.getText().trim(), iconField.getText().trim());
        categoryManager.addCategory(newCategory);

        if (getParent() instanceof CategoryManagementScreen mgmtScreen) {
            mgmtScreen.rebuildList();
        }
        close();
    }

    private Optional<String> validateInput() {
        if (nameField.getText().trim().isEmpty()) {
            return Optional.of("Category name cannot be empty.");
        }
        if (iconField.getText().trim().isEmpty()) {
            return Optional.of("Icon name cannot be empty.");
        }
        if (AssetIdentifiers.getIcon(iconField.getText().trim()).isEmpty()) {
            return Optional.of("Invalid icon name specified.");
        }
        return Optional.empty();
    }

    private void setError(String message) {
        this.errorField.setText(message);
        this.errorField.setEditableColor(GUIColors.ERROR.getRGB());
        this.errorField.setVisible(true);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        nameField.render(context, mouseX, mouseY, delta);
        iconField.render(context, mouseX, mouseY, delta);
        errorField.render(context, mouseX, mouseY, delta);
    }
}
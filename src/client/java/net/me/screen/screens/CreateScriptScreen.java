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
import net.me.screen.MQSScreen;
import net.me.screen.component.WidgetLayoutHelper;
import net.me.screen.component.components.MQSButtonWidget;
import net.me.screen.component.components.MQSTextFieldWidget;
import net.me.screen.component.components.MQSToast;
import net.me.utils.ChatUtils;
import net.me.utils.GUIColors;
import net.me.utils.UIConstants;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class CreateScriptScreen extends MQSScreen {

    private MQSTextFieldWidget mainClassField;
    private MQSTextFieldWidget nameField;
    private MQSTextFieldWidget versionField;
    private MQSTextFieldWidget errorField;

    public CreateScriptScreen(MQSScreen parent) {
        super("Create New Script", 250, 220, parent);
    }

    private static @NotNull String getTemplate(String mainClass, String name, String version) {
        String template = """
                // @module(main=%s, name=%s, version=%s)
                class %s {
                
                    onEnable() {
                        // triggered when the module is enabled
                        println("Hello from %s!");
                    }
                
                    onDisable() {
                        // triggered when the module is disabled
                        println("Goodbye from %s!");
                    }
                }
                
                exportModule(%s);
                """;

        return String.format(template, mainClass, name, version, mainClass, name, name, mainClass);
    }

    @Override
    protected void init() {
        super.init();

        this.mainClassField = MQSTextFieldWidget.builder()
                .placeholder("Main Class Name (e.g., MyScript)")
                .size(UIConstants.WIDGET_WIDTH_STANDARD, UIConstants.BUTTON_HEIGHT)
                .build();

        this.nameField = MQSTextFieldWidget.builder()
                .placeholder("Module Name (e.g., My Awesome Script)")
                .size(UIConstants.WIDGET_WIDTH_STANDARD, UIConstants.BUTTON_HEIGHT)
                .build();

        this.versionField = MQSTextFieldWidget.builder()
                .placeholder("Version (e.g., 1.0.0)")
                .size(UIConstants.WIDGET_WIDTH_STANDARD, UIConstants.BUTTON_HEIGHT)
                .build();

        this.errorField = MQSTextFieldWidget.builder()
                .size(UIConstants.WIDGET_WIDTH_STANDARD, UIConstants.BUTTON_HEIGHT)
                .build();
        errorField.setEditable(false);
        errorField.setVisible(false);

        // --- THIS IS THE CORRECTED LINE ---
        MQSButtonWidget createButton = MQSButtonWidget.mqsBuilder("Create Script", button -> createScript())
                .size(UIConstants.WIDGET_WIDTH_STANDARD, UIConstants.BUTTON_HEIGHT)
                .build();

        this.addSelectableChild(mainClassField);
        this.addSelectableChild(nameField);
        this.addSelectableChild(versionField);
        this.addDrawableChild(createButton);
        this.addDrawableChild(errorField);

        WidgetLayoutHelper.layoutVertically(
                getMiddlePoint().x() - UIConstants.WIDGET_WIDTH_STANDARD / 2,
                getMiddlePoint().y() - 60,
                UIConstants.WIDGET_SPACING,
                mainClassField,
                nameField,
                versionField,
                createButton
        );
        int errorY = createButton.getY() + createButton.getHeight() + UIConstants.WIDGET_SPACING;
        errorField.setPos(createButton.getX(), errorY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        mainClassField.render(context, mouseX, mouseY, delta);
        nameField.render(context, mouseX, mouseY, delta);
        versionField.render(context, mouseX, mouseY, delta);
    }

    private void createScript() {
        Optional<String> validationError = validateInput();
        if (validationError.isPresent()) {
            setError(validationError.get());
            return;
        }

        String name = nameField.getText().trim();
        Path scriptPath = generateScriptPath(name);

        if (Files.exists(scriptPath)) {
            setError("A script with this name already exists.");
            return;
        }

        try {
            generateScriptFile(scriptPath);
            handleCreationSuccess(name);
        } catch (IOException e) {
            handleCreationFailure(e);
        }
    }

    private Optional<String> validateInput() {
        if (mainClassField.getText().trim().isEmpty() || nameField.getText().trim().isEmpty() || versionField.getText().trim().isEmpty()) {
            return Optional.of("All fields are required.");
        }
        if (!mainClassField.getText().trim().matches("^[a-zA-Z_$][a-zA-Z\\d_$]*$")) {
            return Optional.of("Invalid Main Class Name.");
        }
        return Optional.empty();
    }

    private Path generateScriptPath(String name) {
        String safeFileName = name.toLowerCase().replaceAll("\\s+", "-").replaceAll("[^a-z0-9-]", "") + ".js";
        Path scriptsDir = Main.MOD_DIR.resolve("scripts");
        return scriptsDir.resolve(safeFileName);
    }

    private void generateScriptFile(Path scriptPath) throws IOException {
        String mainClass = mainClassField.getText().trim();
        String name = nameField.getText().trim();
        String version = versionField.getText().trim();
        String content = getTemplate(mainClass, name, version);

        Files.createDirectories(scriptPath.getParent());
        Files.writeString(scriptPath, content);
    }

    private void handleCreationSuccess(String scriptName) {
        MQSToast.show("Script Created!", "You can now find it in the 'scripts' folder.", 4000, MQSToast.Corner.TOP_LEFT);

        AllScriptsScreen allScriptsScreen = findAllScriptsScreen();
        if (allScriptsScreen != null) {
            assert this.client != null;
            this.client.setScreen(allScriptsScreen);
            allScriptsScreen.forceRefresh();
        } else {
            close();
            ChatUtils.addSuccessChatMessage("The script '" + scriptName + "' has been created!", true);
        }
    }

    private void handleCreationFailure(IOException e) {
        setError("Error: Failed to create script file.");
        Main.LOGGER.error("Failed to create script file", e);
    }

    @Nullable
    private AllScriptsScreen findAllScriptsScreen() {
        MQSScreen current = this.getParent();
        while (current != null) {
            if (current instanceof AllScriptsScreen allScriptsScreen) {
                return allScriptsScreen;
            }
            current = current.getParent();
        }
        return null;
    }

    private void setError(String message) {
        this.errorField.setText(message);
        this.errorField.setEditableColor(GUIColors.ERROR.getRGB());
        this.errorField.setVisible(true);
    }
}
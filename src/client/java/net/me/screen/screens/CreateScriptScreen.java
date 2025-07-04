package net.me.screen.screens;

import net.me.Main;
import net.me.screen.MQSScreen;
import net.me.screen.component.WidgetLayoutHelper;
import net.me.screen.component.components.MQSButtonWidget;
import net.me.screen.component.components.MQSTextFieldWidget;
import net.me.utils.GUIColors;
import net.me.utils.TextRenderUtils;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CreateScriptScreen extends MQSScreen {

    private static final int WIDGET_WIDTH = 200;
    private static final int WIDGET_HEIGHT = 20;
    private static final int WIDGET_SPACING = 5;

    private MQSTextFieldWidget mainClassField;
    private MQSTextFieldWidget nameField;
    private MQSTextFieldWidget versionField;

    private String statusMessage = "";
    private int statusMessageColor = GUIColors.TEXT.getRGB();

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
                .size(WIDGET_WIDTH, WIDGET_HEIGHT)
                .build();

        this.nameField = MQSTextFieldWidget.builder()
                .placeholder("Module Name (e.g., My Awesome Script)")
                .size(WIDGET_WIDTH, WIDGET_HEIGHT)
                .build();

        this.versionField = MQSTextFieldWidget.builder()
                .placeholder("Version (e.g., 1.0.0)")
                .size(WIDGET_WIDTH, WIDGET_HEIGHT)
                .build();

        MQSButtonWidget createButton = MQSButtonWidget.builder("Create Script", button -> createScript())
                .size(WIDGET_WIDTH, WIDGET_HEIGHT)
                .build();

        this.addSelectableChild(mainClassField);
        this.addSelectableChild(nameField);
        this.addSelectableChild(versionField);
        this.addDrawableChild(createButton);

        WidgetLayoutHelper.layoutVertically(
                getMiddlePoint().x() - WIDGET_WIDTH / 2,
                getMiddlePoint().y() - 60,
                WIDGET_SPACING,
                mainClassField,
                nameField,
                versionField,
                createButton
        );
        createButton.setPos(versionField.getX(), versionField.getY() + WIDGET_HEIGHT * 2);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (!statusMessage.isEmpty()) {
            TextRenderUtils.drawCustomCenteredText(
                    context,
                    statusMessage,
                    getMiddlePoint().x(),
                    getMiddlePoint().y() + 70,
                    statusMessageColor,
                    true,
                    1.0f
            );
        }
        mainClassField.render(context, mouseX, mouseY, delta);
        nameField.render(context, mouseX, mouseY, delta);
        versionField.render(context, mouseX, mouseY, delta);
    }

    private void createScript() {
        String mainClass = mainClassField.getText().trim();
        String name = nameField.getText().trim();
        String version = versionField.getText().trim();

        if (mainClass.isEmpty() || name.isEmpty() || version.isEmpty()) {
            this.statusMessage = "All fields are required.";
            this.statusMessageColor = GUIColors.ERROR.getRGB();
            return;
        }

        if (!mainClass.matches("^[a-zA-Z_$][a-zA-Z0-9_$]*$")) {
            this.statusMessage = "Invalid Main Class Name. Use standard JS class naming.";
            this.statusMessageColor = GUIColors.ERROR.getRGB();
            return;
        }

        String safeFileName = name.toLowerCase().replaceAll("\\s+", "-").replaceAll("[^a-z0-9-]", "") + ".js";
        Path scriptsDir = Main.MOD_DIR.resolve("scripts");
        Path scriptPath = scriptsDir.resolve(safeFileName);

        if (Files.exists(scriptPath)) {
            this.statusMessage = "A script with this name already exists.";
            this.statusMessageColor = GUIColors.ERROR.getRGB();
            return;
        }

        String content = getTemplate(mainClass, name, version);

        try {
            Files.createDirectories(scriptsDir);
            Files.writeString(scriptPath, content);
            this.statusMessage = "Script '" + safeFileName + "' created successfully!";
            this.statusMessageColor = GUIColors.SUCCESS.getRGB();
        } catch (IOException e) {
            this.statusMessage = "Error: Failed to create script file.";
            this.statusMessageColor = GUIColors.ERROR.getRGB();
            Main.LOGGER.error("Failed to create script file", e);
        }
    }
}
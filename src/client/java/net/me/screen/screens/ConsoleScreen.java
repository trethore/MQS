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

import net.me.console.ConsoleManager;
import net.me.console.ConsoleMessage;
import net.me.screen.MQSScreen;
import net.me.screen.component.components.MQSTextFieldWidget;
import net.me.utils.TextRenderUtils;
import net.me.utils.TextRendererUtils;
import net.me.utils.UIConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ConsoleScreen extends MQSScreen {

    private final static int HEADER_MARGIN = 55;
    private final static int FOOTER_MARGIN = 15;

    private final ConsoleManager consoleManager;
    private final List<DisplayLine> displayLines = new ArrayList<>();
    private MQSTextFieldWidget inputField;
    private double scrollY = 0;
    private boolean autoScroll = true;
    private int historyIndex;
    private String unsentInput = "";
    private boolean navigatingHistory = false;
    private int lastMessageCount = 0;

    public ConsoleScreen(AllScriptsScreen parent, ConsoleManager consoleManager) {
        super("The QOL Console", 400, 300, parent);
        this.consoleManager = consoleManager;
    }

    @Override
    protected void init() {
        super.init();
        int windowStartX = getMiddlePoint().x() - getWindowWidth() / 2;
        int windowStartY = getMiddlePoint().y() - getWindowHeight() / 2;

        int inputX = windowStartX + UIConstants.PADDING_M;
        int inputY = windowStartY + getWindowHeight() - UIConstants.INPUT_HEIGHT - UIConstants.PADDING_M;
        int inputWidth = getWindowWidth() - (UIConstants.PADDING_M * 2);

        this.inputField = MQSTextFieldWidget.builder()
                .dimensions(inputX, inputY, inputWidth, UIConstants.INPUT_HEIGHT)
                .placeholder("> Enter a command... (Type 'help')")
                .build();

        this.inputField.setChangedListener(this::onInputChanged);
        this.historyIndex = consoleManager.getCommandHistory().size();

        this.addSelectableChild(this.inputField);
        this.setFocused(this.inputField);

        rebuildDisplayLines();
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        rebuildDisplayLines();
    }

    private void appendDisplayLines(List<ConsoleMessage> messages) {
        int renderAreaWidth = getWindowWidth() - (UIConstants.PADDING_M * 2);
        if (renderAreaWidth <= 0) return;

        for (ConsoleMessage msg : messages) {
            String textToWrap = String.format("[%s] %s", msg.timestamp(), msg.text());
            List<OrderedText> wrapped = TextRendererUtils.getCustomTextRenderer().wrapLines(Text.literal(textToWrap), renderAreaWidth);

            for (OrderedText line : wrapped) {
                final StringBuilder sb = new StringBuilder();
                line.accept((index, style, codePoint) -> {
                    sb.appendCodePoint(codePoint);
                    return true;
                });
                this.displayLines.add(new DisplayLine(sb.toString(), msg.type().getColor()));
            }
        }
    }

    private void rebuildDisplayLines() {
        this.displayLines.clear();
        List<ConsoleMessage> messages = consoleManager.getMessages();
        appendDisplayLines(messages);
        this.lastMessageCount = messages.size();
    }

    private void onInputChanged(String text) {
        if (!navigatingHistory) {
            this.historyIndex = consoleManager.getCommandHistory().size();
            this.unsentInput = text;
        }
    }

    private void checkForNewMessages() {
        List<ConsoleMessage> allMessages = consoleManager.getMessages();
        if (allMessages.size() == lastMessageCount) {
            return;
        }

        if (allMessages.size() < lastMessageCount) {
            rebuildDisplayLines();
            return;
        }

        List<ConsoleMessage> newMessages = allMessages.subList(lastMessageCount, allMessages.size());
        appendDisplayLines(newMessages);
        lastMessageCount = allMessages.size();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        checkForNewMessages();

        renderMessages(context, this.displayLines);

        this.inputField.render(context, mouseX, mouseY, delta);
    }

    private int getLineHeight() {
        return TextRendererUtils.getCustomTextRenderer().fontHeight + 2;
    }

    private void renderMessages(DrawContext context, List<DisplayLine> linesToRender) {
        int fontHeight = getLineHeight();

        int windowStartX = getMiddlePoint().x() - getWindowWidth() / 2;
        int windowStartY = getMiddlePoint().y() - getWindowHeight() / 2;

        int renderAreaX = windowStartX + UIConstants.PADDING_M;
        int renderAreaY = windowStartY + HEADER_MARGIN;
        int renderAreaHeight = getWindowHeight() - HEADER_MARGIN - UIConstants.INPUT_HEIGHT - FOOTER_MARGIN;
        int maxLinesVisible = renderAreaHeight / fontHeight;

        if (autoScroll) {
            scrollY = Math.max(0, linesToRender.size() - maxLinesVisible);
        }

        int firstLineIndex = (int) Math.max(0, scrollY);

        context.enableScissor(renderAreaX, renderAreaY - 2, renderAreaX + getWindowWidth() - (UIConstants.PADDING_M * 2), renderAreaY + renderAreaHeight + 2);

        for (int i = 0; i < maxLinesVisible && (firstLineIndex + i) < linesToRender.size(); i++) {
            int currentLineIndex = firstLineIndex + i;
            DisplayLine line = linesToRender.get(currentLineIndex);

            int yPos = renderAreaY + (i * fontHeight);

            TextRenderUtils.drawCustomText(context, line.text(), renderAreaX, yPos, line.color(), true, 1f);
        }

        context.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int fontHeight = getLineHeight();
        int renderAreaHeight = getWindowHeight() - HEADER_MARGIN - UIConstants.INPUT_HEIGHT - FOOTER_MARGIN;
        if (renderAreaHeight <= 0) {
            return false;
        }

        int maxLinesVisible = renderAreaHeight / fontHeight;
        double maxScroll = Math.max(0, this.displayLines.size() - maxLinesVisible);

        double newScrollY = scrollY - verticalAmount;
        scrollY = Math.max(0, Math.min(newScrollY, maxScroll));

        autoScroll = scrollY >= maxScroll - 0.5;

        return true;
    }

    private void navigateHistory(int direction) {
        List<String> history = consoleManager.getCommandHistory();
        if (history.isEmpty()) {
            return;
        }

        int newIndex = this.historyIndex + direction;
        this.historyIndex = Math.max(0, Math.min(newIndex, history.size()));

        navigatingHistory = true;
        if (this.historyIndex >= history.size()) {
            this.inputField.setText(this.unsentInput);
        } else {
            this.inputField.setText(history.get(this.historyIndex));
        }
        this.inputField.setCursorToEnd(false);
        navigatingHistory = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.inputField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (this.inputField.isFocused()) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_UP:
                    navigateHistory(-1);
                    return true;
                case GLFW.GLFW_KEY_DOWN:
                    navigateHistory(1);
                    return true;
                case GLFW.GLFW_KEY_ENTER:
                case GLFW.GLFW_KEY_KP_ENTER:
                    handleCommandExecution();
                    return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void handleCommandExecution() {
        String command = this.inputField.getText();
        consoleManager.executeCommand(command);

        navigatingHistory = true;
        this.inputField.setText("");
        navigatingHistory = false;

        this.unsentInput = "";
        this.historyIndex = consoleManager.getCommandHistory().size();
        this.autoScroll = true;
    }

    private record DisplayLine(String text, int color) {
    }
}
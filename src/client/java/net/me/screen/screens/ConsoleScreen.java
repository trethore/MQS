package net.me.screen.screens;

import net.me.console.ConsoleManager;
import net.me.console.ConsoleMessage;
import net.me.screen.MQSScreen;
import net.me.screen.component.components.MQSTextFieldWidget;
import net.me.utils.TextRenderUtils;
import net.me.utils.TextRendererUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ConsoleScreen extends MQSScreen {

    private final ConsoleManager consoleManager;
    private final int PADDING = 10;
    private final int INPUT_HEIGHT = 20;
    private final int HEADER_MARGIN = 55;
    private final int FOOTER_MARGIN = 15;
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

        int inputX = windowStartX + PADDING;
        int inputY = windowStartY + getWindowHeight() - INPUT_HEIGHT - PADDING;
        int inputWidth = getWindowWidth() - (PADDING * 2);

        this.inputField = MQSTextFieldWidget.builder()
                .dimensions(inputX, inputY, inputWidth, INPUT_HEIGHT)
                .placeholder("> Enter a command... (Type 'help')")
                .build();

        this.inputField.setChangedListener(this::onInputChanged);
        this.historyIndex = consoleManager.getCommandHistory().size();

        this.addSelectableChild(this.inputField);
        this.setFocused(this.inputField);

        this.displayLines.clear();
        this.lastMessageCount = 0;
        updateDisplayLines();
    }

    private void onInputChanged(String text) {
        if (!navigatingHistory) {
            this.historyIndex = consoleManager.getCommandHistory().size();
            this.unsentInput = text;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (consoleManager.getMessages().size() != lastMessageCount) {
            updateDisplayLines();
        }

        renderMessages(context, this.displayLines);

        this.inputField.render(context, mouseX, mouseY, delta);
    }

    private void updateDisplayLines() {
        List<ConsoleMessage> messages = consoleManager.getMessages();
        int currentMessageCount = messages.size();

        if (currentMessageCount < lastMessageCount) {
            displayLines.clear();
            lastMessageCount = 0;
        }

        int renderAreaWidth = getWindowWidth() - (PADDING * 2);
        if (renderAreaWidth <= 0) return;

        for (int i = lastMessageCount; i < currentMessageCount; i++) {
            ConsoleMessage msg = messages.get(i);
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

        this.lastMessageCount = currentMessageCount;
    }

    private int getLineHeight() {
        return TextRendererUtils.getCustomTextRenderer().fontHeight + 2;
    }

    private void renderMessages(DrawContext context, List<DisplayLine> linesToRender) {
        int fontHeight = getLineHeight();

        int windowStartX = getMiddlePoint().x() - getWindowWidth() / 2;
        int windowStartY = getMiddlePoint().y() - getWindowHeight() / 2;

        int renderAreaX = windowStartX + PADDING;
        int renderAreaY = windowStartY + HEADER_MARGIN;
        int renderAreaHeight = getWindowHeight() - HEADER_MARGIN - INPUT_HEIGHT - FOOTER_MARGIN;
        int maxLinesVisible = renderAreaHeight / fontHeight;

        if (autoScroll) {
            scrollY = Math.max(0, linesToRender.size() - maxLinesVisible);
        }

        int firstLineIndex = (int) Math.max(0, scrollY);

        context.enableScissor(renderAreaX, renderAreaY - 2, renderAreaX + getWindowWidth() - (PADDING * 2), renderAreaY + renderAreaHeight + 2);

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
        int renderAreaHeight = getWindowHeight() - HEADER_MARGIN - INPUT_HEIGHT - FOOTER_MARGIN;
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
        this.inputField.setCursorToEnd();
        navigatingHistory = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.inputField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (this.inputField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_UP) {
                navigateHistory(-1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                navigateHistory(1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                String command = this.inputField.getText();
                consoleManager.executeCommand(command);

                navigatingHistory = true;
                this.inputField.setText("");
                navigatingHistory = false;

                autoScroll = true;
                this.historyIndex = consoleManager.getCommandHistory().size();
                this.unsentInput = "";
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private record DisplayLine(String text, int color) {
    }
}
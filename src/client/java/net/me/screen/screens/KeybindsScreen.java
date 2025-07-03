// New File: java/net/me/screen/screens/KeybindsScreen.java
package net.me.screen.screens;

import net.me.Main;
import net.me.keybinds.KeyBinding;
import net.me.keybinds.KeybindManager;
import net.me.screen.MQSScreen;
import net.me.screen.component.components.KeybindEntryWidget;
import net.me.scripting.module.RunningScript;
import net.me.utils.GUIColors;
import net.me.utils.TextRenderUtils;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class KeybindsScreen extends MQSScreen {
    private final KeybindManager keybindManager;
    private final List<KeybindEntryWidget> keybindEntryWidgets = new ArrayList<>();
    private final int PADDING = 20;
    private final int ENTRY_HEIGHT = 30;
    private final int HEADER_HEIGHT = 20;
    private double scrollY = 0;
    private KeybindEntryWidget listeningWidget = null;

    public KeybindsScreen(MQSScreen parent) {
        super("Keybinds", 400, 300, parent);
        this.keybindManager = Main.getKeybindManager();
    }

    @Override
    protected void init() {
        super.init();
        this.keybindEntryWidgets.clear();
        int windowStartX = getMiddlePoint().x() - getWindowWidth() / 2;
        int currentY = getMiddlePoint().y() - getWindowHeight() / 2 + 55;

        Map<RunningScript, List<KeyBinding>> groupedKeybinds = keybindManager.getGroupedKeybinds();
        List<RunningScript> sortedScripts = new ArrayList<>(groupedKeybinds.keySet());
        sortedScripts.sort(Comparator.comparing(RunningScript::getName, String.CASE_INSENSITIVE_ORDER));

        for (RunningScript script : sortedScripts) {
            currentY += HEADER_HEIGHT;
            List<KeyBinding> bindings = groupedKeybinds.get(script);
            if (bindings == null) continue;

            bindings.sort(Comparator.comparing(KeyBinding::getName, String.CASE_INSENSITIVE_ORDER));

            for (KeyBinding binding : bindings) {
                KeybindEntryWidget widget = new KeybindEntryWidget(
                        windowStartX + PADDING,
                        currentY,
                        getWindowWidth() - (PADDING * 2),
                        ENTRY_HEIGHT,
                        binding,
                        this::startListening
                );
                this.keybindEntryWidgets.add(widget);
                currentY += ENTRY_HEIGHT;
            }
        }

        this.keybindEntryWidgets.forEach(this::addDrawableChild);
        updateWidgetPositions();
    }

    private void startListening(KeyBinding bindingToRebind) {
        if (this.listeningWidget != null) {
            this.listeningWidget.setListening(false);
        }

        this.listeningWidget = this.keybindEntryWidgets.stream()
                .filter(w -> w.getKeyBinding().equals(bindingToRebind))
                .findFirst().orElse(null);

        if (this.listeningWidget != null) {
            this.listeningWidget.setListening(true);
        }
    }

    private void stopListening(int newKeyCode) {
        if (this.listeningWidget != null) {
            keybindManager.rebindKey(this.listeningWidget.getKeyBinding(), newKeyCode);
            this.listeningWidget.setListening(false);
            this.listeningWidget = null;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int windowStartX = getMiddlePoint().x() - getWindowWidth() / 2;
        int windowStartY = getMiddlePoint().y() - getWindowHeight() / 2;
        int listStartY = windowStartY + 55;
        int listHeight = getWindowHeight() - 70;

        context.enableScissor(windowStartX, listStartY, windowStartX + getWindowWidth(), listStartY + listHeight);

        int currentY = (int) (listStartY - scrollY);
        Map<RunningScript, List<KeyBinding>> groupedKeybinds = keybindManager.getGroupedKeybinds();
        List<RunningScript> sortedScripts = new ArrayList<>(groupedKeybinds.keySet());
        sortedScripts.sort(Comparator.comparing(RunningScript::getName, String.CASE_INSENSITIVE_ORDER));

        for (RunningScript script : sortedScripts) {
            String header = script.getName() + " v" + script.getVersion();
            TextRenderUtils.drawText(context, header, windowStartX + PADDING, currentY, GUIColors.TEXT_GREY_DISABLED.getRGBA(), true, 1f);
            currentY += HEADER_HEIGHT;

            for (KeybindEntryWidget widget : this.keybindEntryWidgets) {
                if (widget.getKeyBinding().getOwner().equals(script)) {
                    widget.setY(currentY);
                    widget.render(context, mouseX, mouseY, delta);
                    currentY += ENTRY_HEIGHT;
                }
            }
        }

        context.disableScissor();
    }

    private void updateWidgetPositions() {
        int windowStartY = getMiddlePoint().y() - getWindowHeight() / 2;
        int listStartY = windowStartY + 55;
        int currentY = (int) (listStartY - scrollY);

        for (KeybindEntryWidget widget : this.keybindEntryWidgets) {
            widget.setY(currentY);
            currentY += ENTRY_HEIGHT;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int contentHeight = 0;
        Map<RunningScript, List<KeyBinding>> groupedKeybinds = keybindManager.getGroupedKeybinds();
        for (RunningScript script : groupedKeybinds.keySet()) {
            contentHeight += HEADER_HEIGHT;
            contentHeight += groupedKeybinds.get(script).size() * ENTRY_HEIGHT;
        }

        int viewHeight = getWindowHeight() - 70;
        double maxScroll = Math.max(0, contentHeight - viewHeight);

        scrollY -= verticalAmount * 10;
        if (scrollY < 0) scrollY = 0;
        if (scrollY > maxScroll) scrollY = maxScroll;

        updateWidgetPositions();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listeningWidget != null) {
            stopListening(button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningWidget != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                stopListening(-1); // Unbind
            } else {
                stopListening(keyCode);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (listeningWidget != null) {
            listeningWidget.setListening(false);
            listeningWidget = null;
        }
        super.close();
    }
}
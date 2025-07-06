package net.me.screen.screens;

import net.me.keybinds.KeyBinding;
import net.me.keybinds.KeybindManager;
import net.me.keybinds.Keys;
import net.me.screen.MQSScreen;
import net.me.screen.component.components.KeybindEntryWidget;
import net.me.scripting.module.RunningScript;
import net.me.utils.GUIColors;
import net.me.utils.TextRenderUtils;
import net.me.utils.UIConstants;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

public class KeybindsScreen extends MQSScreen {
    private static final int PADDING = 30;
    private static final int HEADER_HEIGHT = 20;
    private static final int LIST_TOP_MARGIN = 55;
    private static final int LIST_BOTTOM_MARGIN = 70;
    private final KeybindManager keybindManager;
    private final List<KeybindEntryWidget> keybindEntryWidgets = new ArrayList<>();
    private KeybindEntryWidget listeningWidget = null;
    private double scrollY = 0;
    private int totalContentHeight = 0;
    private Map<RunningScript, List<KeyBinding>> sortedKeybindsCache;

    public KeybindsScreen(MQSScreen parent, KeybindManager keybindManager) {
        super("Keybinds", 300, 280, parent);
        this.keybindManager = keybindManager;
    }

    private Map<RunningScript, List<KeyBinding>> getSortedScriptGroups() {
        return keybindManager.getGroupedKeybinds().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(RunningScript::getName, String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    @Override
    protected void init() {
        super.init();
        this.keybindEntryWidgets.clear();
        this.totalContentHeight = 0;

        this.sortedKeybindsCache = getSortedScriptGroups();

        int windowStartX = getMiddlePoint().x() - getWindowWidth() / 2;
        int currentY = getMiddlePoint().y() - getWindowHeight() / 2 + LIST_TOP_MARGIN;

        for (Map.Entry<RunningScript, List<KeyBinding>> entry : this.sortedKeybindsCache.entrySet()) {
            currentY += HEADER_HEIGHT;
            this.totalContentHeight += HEADER_HEIGHT;

            List<KeyBinding> bindings = entry.getValue();
            bindings.sort(Comparator.comparing(KeyBinding::getName, String.CASE_INSENSITIVE_ORDER));

            for (KeyBinding binding : bindings) {
                KeybindEntryWidget widget = KeybindEntryWidget.builder(binding, this::startListening)
                        .dimensions(
                                windowStartX + PADDING,
                                currentY,
                                getWindowWidth() - (PADDING * 2),
                                UIConstants.ENTRY_HEIGHT
                        )
                        .build();

                this.keybindEntryWidgets.add(widget);
                currentY += UIConstants.ENTRY_HEIGHT;
                this.totalContentHeight += UIConstants.ENTRY_HEIGHT;
            }
        }
        this.keybindEntryWidgets.forEach(this::addSelectableChild);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (this.sortedKeybindsCache.isEmpty()) {
            TextRenderUtils.drawCustomCenteredText(context, "No custom keybinds registered :(",
                    this.getMiddlePoint().x(),
                    this.getMiddlePoint().y() - 25,
                    GUIColors.TEXT_DISABLED.getRGB(), true, UIConstants.TEXT_SCALE);
            TextRenderUtils.drawCustomCenteredText(context, "Try enabling some modules.",
                    this.getMiddlePoint().x(),
                    this.getMiddlePoint().y() - 10,
                    GUIColors.TEXT_DISABLED.getRGB(), true, UIConstants.TEXT_SCALE);
        } else {
            int windowStartX = getMiddlePoint().x() - getWindowWidth() / 2;
            int windowStartY = getMiddlePoint().y() - getWindowHeight() / 2;
            int listStartY = windowStartY + LIST_TOP_MARGIN;
            int listHeight = getWindowHeight() - LIST_BOTTOM_MARGIN;

            context.enableScissor(windowStartX, listStartY, windowStartX + getWindowWidth(), listStartY + listHeight);

            int currentY = (int) (listStartY - scrollY);
            Map<RunningScript, List<KeyBinding>> groupedKeybinds = this.sortedKeybindsCache;

            for (Map.Entry<RunningScript, List<KeyBinding>> entry : groupedKeybinds.entrySet()) {
                RunningScript script = entry.getKey();
                String header = script.getName() + " v" + script.getVersion();
                TextRenderUtils.drawCustomCenteredText(context, header, this.getMiddlePoint().x(), currentY + 8, GUIColors.TEXT.getRGB(), true, 1.1f);
                currentY += HEADER_HEIGHT;

                for (KeybindEntryWidget widget : this.keybindEntryWidgets) {
                    if (widget.getKeyBinding().getOwner().equals(script)) {
                        widget.setY(currentY);
                        widget.render(context, mouseX, mouseY, delta);
                        currentY += UIConstants.ENTRY_HEIGHT;
                    }
                }
            }
            context.disableScissor();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int viewHeight = getWindowHeight() - LIST_BOTTOM_MARGIN;
        double maxScroll = Math.max(0, this.totalContentHeight - viewHeight);

        scrollY -= verticalAmount * 10;
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));

        return true;
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listeningWidget != null) {
            stopListening(button);
            return true;
        }
        for (KeybindEntryWidget widget : this.keybindEntryWidgets) {
            if (widget.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningWidget != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                stopListening(Keys.UNBOUND.getCode());
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
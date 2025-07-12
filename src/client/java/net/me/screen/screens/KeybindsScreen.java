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

import net.me.keybinds.KeyBinding;
import net.me.keybinds.KeybindManager;
import net.me.keybinds.Keys;
import net.me.screen.MQSScreen;
import net.me.screen.component.components.KeybindEntryWidget;
import net.me.screen.component.components.ScrollbarWidget;
import net.me.screen.theme.GUIColors;
import net.me.screen.theme.UIConstants;
import net.me.scripting.module.RunningScript;
import net.me.utils.TextRenderUtils;
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
    private ScrollbarWidget scrollbar;
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
        int listStartY = getMiddlePoint().y() - getWindowHeight() / 2 + LIST_TOP_MARGIN;

        int scrollbarX = windowStartX + getWindowWidth() - PADDING + UIConstants.PADDING_S;
        int scrollbarHeight = getWindowHeight() - LIST_TOP_MARGIN - LIST_BOTTOM_MARGIN + 15;
        this.scrollbar = new ScrollbarWidget(scrollbarX, listStartY, UIConstants.SCROLLBAR_WIDTH, scrollbarHeight, (newScroll) -> this.scrollY = newScroll);
        this.addDrawableChild(this.scrollbar);


        int currentY = listStartY;
        int widgetWidth = getWindowWidth() - (PADDING * 2) - UIConstants.SCROLLBAR_WIDTH - UIConstants.PADDING_S;

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
                                widgetWidth,
                                UIConstants.ENTRY_HEIGHT
                        )
                        .build();

                this.keybindEntryWidgets.add(widget);
                currentY += UIConstants.ENTRY_HEIGHT;
                this.totalContentHeight += UIConstants.ENTRY_HEIGHT;
            }
        }
        this.keybindEntryWidgets.forEach(this::addSelectableChild);
        this.scrollbar.update(totalContentHeight, scrollbarHeight, scrollY);
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
            this.scrollbar.visible = false;
        } else {
            this.scrollbar.visible = true;
            int windowStartX = getMiddlePoint().x() - getWindowWidth() / 2;
            int windowStartY = getMiddlePoint().y() - getWindowHeight() / 2;
            int listStartY = windowStartY + LIST_TOP_MARGIN;
            int listHeight = getWindowHeight() - LIST_TOP_MARGIN - LIST_BOTTOM_MARGIN + 15;

            this.scrollbar.update(totalContentHeight, listHeight, scrollY);


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

    private boolean isMouseInScrollableArea(double mouseX, double mouseY) {
        int windowStartX = getMiddlePoint().x() - getWindowWidth() / 2;
        int windowStartY = getMiddlePoint().y() - getWindowHeight() / 2;
        int listStartY = windowStartY + LIST_TOP_MARGIN;
        int listHeight = getWindowHeight() - LIST_TOP_MARGIN - LIST_BOTTOM_MARGIN + 15;
        int listWidth = getWindowWidth() - (PADDING * 2);

        return mouseX >= windowStartX + PADDING && mouseX <= windowStartX + PADDING + listWidth &&
                mouseY >= listStartY && mouseY <= listStartY + listHeight;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isMouseInScrollableArea(mouseX, mouseY) || this.scrollbar.isMouseOver(mouseX, mouseY)) {
            int viewHeight = getWindowHeight() - LIST_TOP_MARGIN - LIST_BOTTOM_MARGIN + 15;
            double maxScroll = Math.max(0, this.totalContentHeight - viewHeight);

            scrollY -= verticalAmount * 10;
            scrollY = Math.max(0, Math.min(scrollY, maxScroll));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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

        if (this.scrollbar.mouseClicked(mouseX, mouseY, button)) {
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.scrollbar.isMouseOver(mouseX, mouseY)) {
            return this.scrollbar.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
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
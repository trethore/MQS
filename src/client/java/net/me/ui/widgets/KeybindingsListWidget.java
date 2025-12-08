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

package net.me.ui.widgets;

import net.me.keybinds.HostKeyBinding;
import net.me.keybinds.KeyBinding;
import net.me.keybinds.KeybindManager;
import net.me.scripting.module.RunningScript;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class KeybindingsListWidget extends ElementListWidget<KeybindingsListWidget.Entry> {

    private static final int PADDING = 6;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int HEADER_LINE_COLOR = 0xFFa3a3a3;

    private final KeybindManager keybindManager;

    public KeybindingsListWidget(MinecraftClient client, int width, int height, int y, int itemHeight, KeybindManager keybindManager) {
        super(client, width, height, y, itemHeight);
        this.keybindManager = keybindManager;
    }

    public void setEntries(Map<RunningScript, List<KeyBinding>> grouped, List<HostKeyBinding> hostBindings) {
        this.clearEntries();

        if (!hostBindings.isEmpty()) {
            this.addEntry(new HeaderEntry("Host"));
            hostBindings.stream()
                    .sorted(Comparator.comparing(HostKeyBinding::getName, String.CASE_INSENSITIVE_ORDER))
                    .forEach(binding -> this.addEntry(new BindingEntry(binding.getName(), binding, keybindManager)));
        }

        grouped.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getName(), String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> {
                    RunningScript script = entry.getKey();
                    List<KeyBinding> bindings = entry.getValue();
                    bindings.sort(Comparator.comparing(KeyBinding::getName, String.CASE_INSENSITIVE_ORDER));
                    this.addEntry(new HeaderEntry(script.getName()));
                    for (KeyBinding binding : bindings) {
                        this.addEntry(new BindingEntry(binding.getName(), binding, keybindManager));
                    }
                });
    }

    @Override
    public int getRowWidth() {
        return this.getWidth();
    }

    @Override
    protected int getScrollbarX() {
        return this.getRowLeft() + this.getRowWidth() - 6;
    }

    public abstract static class Entry extends ElementListWidget.Entry<Entry> {
    }

    private static final class HeaderEntry extends Entry {
        private final SeparatorWidget separator;

        private HeaderEntry(String label) {
            this.separator = new SeparatorWidget(0, 0, 0, 10, Text.literal(label), SeparatorWidget.LEFT);
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int width = entryWidth - PADDING * 2;
            int height = Math.max(10, entryHeight);
            int startX = x + PADDING;
            int startY = y + (entryHeight - height) / 2;
            separator.setDimensionsAndPosition(width, height, startX, startY);
            separator.render(context, mouseX, mouseY, tickDelta);
        }

        @Override
        public List<? extends Element> children() {
            return List.of(separator);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of(separator);
        }
    }

    private static final class BindingEntry extends Entry {
        private final String name;
        private final KeybindingButtonWidget button;

        private BindingEntry(String name, KeyBinding binding, KeybindManager manager) {
            this.name = name;
            this.button = new KeybindingButtonWidget(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, binding, manager);
        }

        private BindingEntry(String name, HostKeyBinding binding, KeybindManager manager) {
            this.name = name;
            this.button = new KeybindingButtonWidget(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, binding, manager, null);
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            int labelY = y + (entryHeight - textRenderer.fontHeight) / 2;
            context.drawTextWithShadow(textRenderer, Text.literal(name), x + PADDING, labelY, 0xFFFFFF);

            int buttonY = y + (entryHeight - BUTTON_HEIGHT) / 2;
            int buttonX = x + entryWidth - BUTTON_WIDTH - PADDING;
            button.setX(buttonX);
            button.setY(buttonY);
            button.render(context, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            boolean clicked = this.button.mouseClicked(mouseX, mouseY, button);
            if (clicked) {
                this.setFocused(this.button);
            }
            return clicked;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return this.button.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            return this.button.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            return this.button.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return this.button.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public List<? extends Element> children() {
            return List.of(button);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of(button);
        }
    }
}

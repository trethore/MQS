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

import lombok.Setter;
import net.me.scripting.ScriptingService;
import net.me.scripting.module.ScriptDescriptor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ScriptListEntry extends ElementListWidget.Entry<ScriptListEntry> {

    private static final int STATUS_ON_COLOR = ColorHelper.getArgb(255, 56, 255, 70);
    private static final int STATUS_OFF_COLOR = ColorHelper.getArgb(255, 255, 75, 75);
    private static final int STATUS_BORDER_COLOR = ColorHelper.getArgb(255, 144, 144, 144);
    private static final int STATUS_BORDER_COLOR_HIGHLIGHT = ColorHelper.getArgb(255, 168, 168, 168);
    private static final int ENTRY_BORDER_COLOR = ColorHelper.getArgb(255, 168, 168, 168);

    private final ScriptDescriptor descriptor;
    private final ScriptingService scriptingService;
    private final Runnable onToggle;
    @Setter
    private ScriptListWidget parentList;
    private final FocusAnchor focusAnchor = new FocusAnchor();

    private boolean running;
    private int lastX;
    private int lastY;
    private int lastWidth;
    private int lastHeight;

    public ScriptListEntry(ScriptDescriptor descriptor, boolean running, ScriptingService scriptingService, Runnable onToggle) {
        this.descriptor = descriptor;
        this.running = running;
        this.scriptingService = scriptingService;
        this.onToggle = onToggle;
    }

    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        this.lastX = x;
        this.lastY = y;
        this.lastWidth = entryWidth;
        this.lastHeight = entryHeight;

        var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        int lineHeight = textRenderer.fontHeight;
        int textGap = 2;
        int totalTextHeight = lineHeight * 2 + textGap;
        int textY = y + (entryHeight - totalTextHeight) / 2;
        int textX = x + 6;

        boolean focused = parentList != null && parentList.getSelectedEntry() == this;
        boolean active = focused || hovered;

        MutableText title = Text.literal(descriptor.moduleName());

        if (active) {
            context.drawBorder(x, y, entryWidth, entryHeight, ENTRY_BORDER_COLOR);
        }

        context.drawTextWithShadow(textRenderer, title, textX, textY, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, formatPath(descriptor), textX, textY + lineHeight + textGap, 0xA0A0A0);

        int statusSize = 20;
        int statusRight = x + entryWidth - 6;
        int statusLeft = statusRight - statusSize;
        int statusTop = y + (entryHeight - statusSize) / 2;
        int statusBottom = statusTop + statusSize;

        context.fill(statusLeft, statusTop, statusRight, statusBottom, running ? STATUS_ON_COLOR : STATUS_OFF_COLOR);
        int borderColor = active ? STATUS_BORDER_COLOR_HIGHLIGHT : STATUS_BORDER_COLOR;
        context.drawBorder(statusLeft, statusTop, statusSize, statusSize, borderColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInside(mouseX, mouseY)) {
            if (parentList != null) {
                parentList.setSelected(this);
            }
            toggle();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return isInside(mouseX, mouseY) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public List<? extends Element> children() {
        return Collections.singletonList(focusAnchor);
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
        return Collections.singletonList(focusAnchor);
    }

    public void focusAnchor() {
        this.setFocused(focusAnchor);
    }

    private static final class FocusAnchor implements Element, Selectable {
        @Override
        public void setFocused(boolean focused) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public SelectionType getType() {
            return SelectionType.NONE;
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {
        }
    }

    public void toggle() {
        if (running) {
            scriptingService.disable(descriptor.getId());
        } else {
            scriptingService.enable(descriptor.getId());
        }
        running = scriptingService.isRunning(descriptor.getId());
        onToggle.run();
    }

    private boolean isInside(double mouseX, double mouseY) {
        return mouseX >= lastX && mouseX <= lastX + lastWidth && mouseY >= lastY && mouseY <= lastY + lastHeight;
    }

    public ScriptDescriptor descriptor() {
        return descriptor;
    }

    private String formatPath(ScriptDescriptor descriptor) {
        Path path = descriptor.path();
        int nameCount = path.getNameCount();
        int start = Math.max(0, nameCount - 4);
        List<String> segments = new ArrayList<>();
        for (int i = start; i < nameCount; i++) {
            segments.add(path.getName(i).toString());
        }
        return String.join("/", segments).toLowerCase(Locale.ROOT);
    }
}

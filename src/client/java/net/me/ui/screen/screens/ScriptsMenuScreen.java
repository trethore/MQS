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

package net.me.ui.screen.screens;

import net.me.scripting.ScriptingService;
import net.me.scripting.module.ScriptDescriptor;
import net.me.ui.screen.MQSScreen;
import net.me.ui.widgets.ScriptListWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ScriptsMenuScreen extends MQSScreen {

    private static final int PADDING = 16;
    private static final int COMPONENT_GAP = 8;
    private static final int TITLE_GAP = 14;
    private static final int SEARCH_HEIGHT = 20;
    private static final int REFRESH_BUTTON_WIDTH = 70;
    private static final int REFRESH_BUTTON_HEIGHT = SEARCH_HEIGHT;
    private static final int LIST_ITEM_HEIGHT = 44;
    private static final Text TITLE_TEXT = Text.literal("My QOL Scripts");

    private final ScriptingService scriptingService;

    @Nullable
    private TextFieldWidget searchField;
    @Nullable
    private ButtonWidget refreshButton;
    @Nullable
    private ScriptListWidget scriptList;
    private String pendingSearch = "";

    public ScriptsMenuScreen(ScriptingService scriptingService) {
        super("My QOL Scripts");
        this.scriptingService = scriptingService;
    }

    @Override
    protected void init() {
        super.init();
        var center = getMiddle();
        int contentWidth = Math.min(420, this.width - PADDING * 2);
        int contentLeft = center.x() - (contentWidth / 2);

        int listHeight = 300;
        int totalHeight = this.textRenderer.fontHeight + TITLE_GAP + SEARCH_HEIGHT + COMPONENT_GAP + listHeight;
        int titleY = Math.max(PADDING, center.y() - totalHeight / 2);

        int searchY = titleY + this.textRenderer.fontHeight + TITLE_GAP;
        int searchWidth = contentWidth - REFRESH_BUTTON_WIDTH - COMPONENT_GAP;

        searchField = new TextFieldWidget(this.textRenderer, contentLeft, searchY, searchWidth, SEARCH_HEIGHT, Text.literal("Search"));
        searchField.setPlaceholder(Text.literal("Search").formatted(net.minecraft.util.Formatting.GRAY));
        searchField.setText(pendingSearch);
        searchField.setChangedListener(text -> {
            pendingSearch = text;
            refreshList();
        });
        this.addDrawableChild(searchField);

        refreshButton = ButtonWidget.builder(Text.literal("Refresh"), button -> handleRefresh())
                .dimensions(contentLeft + searchWidth + COMPONENT_GAP, searchY, REFRESH_BUTTON_WIDTH, REFRESH_BUTTON_HEIGHT)
                .build();
        this.addDrawableChild(refreshButton);

        int listY = searchY + SEARCH_HEIGHT + COMPONENT_GAP;
        scriptList = new ScriptListWidget(this.client, contentWidth, listHeight, listY, LIST_ITEM_HEIGHT, scriptingService, this::onEntryToggled);
        scriptList.setX(contentLeft);
        refreshList();
        this.addDrawableChild(scriptList);

        this.setInitialFocus(searchField);
    }

    private void handleRefresh() {
        scriptingService.refreshAndReenable();
        refreshList();
    }

    private void onEntryToggled() {
        refreshList();
    }

    private void refreshList() {
        if (scriptList == null) {
            return;
        }
        String query = pendingSearch.trim().toLowerCase(Locale.ROOT);

        List<ScriptDescriptor> descriptors = new ArrayList<>(scriptingService.listAvailable());
        descriptors.sort(Comparator.comparing(ScriptDescriptor::moduleName, String.CASE_INSENSITIVE_ORDER));

        if (!query.isEmpty()) {
            List<ScriptDescriptor> startsWith = descriptors.stream()
                    .filter(d -> d.moduleName().toLowerCase(Locale.ROOT).startsWith(query))
                    .toList();
            List<ScriptDescriptor> contains = descriptors.stream()
                    .filter(d -> !startsWith.contains(d))
                    .filter(d -> d.moduleName().toLowerCase(Locale.ROOT).contains(query))
                    .toList();
            List<ScriptDescriptor> ordered = new ArrayList<>(startsWith);
            ordered.addAll(contains);
            descriptors = ordered;
        }

        scriptList.setScripts(descriptors);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchField != null && searchField.mouseClicked(mouseX, mouseY, button)) {
            setFocused(searchField);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void resize(net.minecraft.client.MinecraftClient client, int width, int height) {
        this.pendingSearch = searchField != null ? searchField.getText() : "";
        super.resize(client, width, height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, TITLE_TEXT, this.width / 2, PADDING + 2, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) {
            boolean shift = (modifiers & org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT) != 0;
            cycleFocus(shift);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void cycleFocus(boolean reverse) {
        var order = new java.util.ArrayList<net.minecraft.client.gui.Element>();
        if (searchField != null) order.add(searchField);
        if (refreshButton != null) order.add(refreshButton);
        if (scriptList != null) order.add(scriptList);

        if (order.isEmpty()) return;

        net.minecraft.client.gui.Element current = this.getFocused();
        int idx = order.indexOf(current);
        if (idx == -1) {
            idx = reverse ? order.size() - 1 : 0;
        } else {
            idx = reverse ? (idx - 1 + order.size()) % order.size() : (idx + 1) % order.size();
        }
        net.minecraft.client.gui.Element target = order.get(idx);
        this.setFocused(target);
        if (target instanceof ScriptListWidget list) {
            if (list.getSelectedOrNull() == null && !list.children().isEmpty()) {
                list.setSelected(list.children().getFirst());
            }
        }
    }
}

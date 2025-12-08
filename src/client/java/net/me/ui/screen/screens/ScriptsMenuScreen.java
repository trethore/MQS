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

import net.me.Main;
import net.me.scripting.ScriptingService;
import net.me.scripting.module.ScriptDescriptor;
import net.me.ui.screen.MQSScreen;
import net.me.ui.widgets.ScriptListWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
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
    private static final int LIST_ITEM_HEIGHT = 40;
    private static final int ACTION_BUTTON_WIDTH = 80;
    private static final int ACTION_BUTTON_HEIGHT = 20;
    private static final int MAX_LIST_WIDTH = 350;
    private static final int HEADER_HEIGHT = 90;
    private static final float TITLE_SCALE = 1.4f;
    private static final Text TITLE_TEXT = Text.translatable("screen.mqs.scripts.title");

    private final ScriptingService scriptingService;

    @Nullable
    private TextFieldWidget searchField;
    @Nullable
    private ScriptListWidget scriptList;
    @Nullable
    private ButtonWidget openFolderButton;
    @Nullable
    private ButtonWidget refreshButton;
    @Nullable
    private ButtonWidget optionsButton;
    @Nullable
    private ButtonWidget doneButton;
    private String pendingSearch = "";
    @Nullable
    private String selectedScriptId;
    private ThreePartsLayoutWidget layout;
    private DirectionalLayoutWidget headerLayout;
    private DirectionalLayoutWidget footerLayout;
    private int refreshLabelTicks;

    public ScriptsMenuScreen(ScriptingService scriptingService) {
        super("My QOL Scripts");
        this.scriptingService = scriptingService;
    }

    @Override
    protected void init() {
        super.init();
        layout = new ThreePartsLayoutWidget(this, HEADER_HEIGHT, 40);
        headerLayout = layout.addHeader(DirectionalLayoutWidget.vertical().spacing(COMPONENT_GAP), positioner -> positioner.alignHorizontalCenter().marginTop(TITLE_GAP + 12));

        DirectionalLayoutWidget searchRow = headerLayout.add(DirectionalLayoutWidget.horizontal());
        searchField = new TextFieldWidget(this.textRenderer, 0, 0, computeSearchFieldWidth(), SEARCH_HEIGHT, Text.translatable("screen.mqs.scripts.search"));
        searchField.setPlaceholder(Text.translatable("screen.mqs.scripts.search").formatted(Formatting.GRAY));
        searchField.setText(pendingSearch);
        searchField.setChangedListener(text -> {
            pendingSearch = text;
            refreshList();
        });
        searchRow.add(searchField);

        scriptList = new ScriptListWidget(this.client, computeListWidth(), 0, 0, LIST_ITEM_HEIGHT, scriptingService, this::onEntryToggled);
        refreshList();
        this.addDrawableChild(scriptList);

        footerLayout = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(COMPONENT_GAP), positioner -> positioner.alignHorizontalCenter().marginBottom(COMPONENT_GAP));
        openFolderButton = footerLayout.add(ButtonWidget.builder(Text.translatable("screen.mqs.scripts.open_folder"), button -> openScriptsFolder())
                .dimensions(0, 0, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
                .build());
        refreshButton = footerLayout.add(ButtonWidget.builder(Text.translatable("screen.mqs.scripts.refresh"), button -> handleRefresh())
                .dimensions(0, 0, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
                .build());
        optionsButton = footerLayout.add(ButtonWidget.builder(Text.translatable("screen.mqs.scripts.options"), button -> openOptions())
                .dimensions(0, 0, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
                .build());
        doneButton = footerLayout.add(ButtonWidget.builder(ScreenTexts.DONE, button -> closeToParent())
                .dimensions(0, 0, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
                .build());

        layout.forEachChild(this::addDrawableChild);
        this.refreshWidgetPositions();
        this.setInitialFocus(searchField);
    }

    private void handleRefresh() {
        scriptingService.refreshAndReenable();
        refreshList();
        refreshLabelTicks = 60;
        if (refreshButton != null) {
            refreshButton.setMessage(Text.translatable("screen.mqs.scripts.refreshed"));
        }
    }

    private void onEntryToggled() {
        refreshList();
    }

    private void openScriptsFolder() {
        if (this.client == null) {
            return;
        }
        Util.getOperatingSystem().open(Main.MOD_DIR.resolve("scripts").toUri());
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
        if (selectedScriptId != null) {
            scriptList.selectById(selectedScriptId);
        }
        selectedScriptId = scriptList.getSelectedId();
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
    public void resize(MinecraftClient client, int width, int height) {
        this.pendingSearch = searchField != null ? searchField.getText() : "";
        this.selectedScriptId = scriptList != null ? scriptList.getSelectedId() : null;
        super.resize(client, width, height);
    }

    @Override
    protected void refreshWidgetPositions() {
        if (searchField != null) {
            searchField.setWidth(computeSearchFieldWidth());
        }
        if (layout != null) {
            layout.refreshPositions();
        }
        centerAndPositionList();
    }

    private void centerAndPositionList() {
        ScriptListWidget list = this.scriptList;
        if (list == null || headerLayout == null || footerLayout == null) {
            return;
        }

        int listWidth = computeListWidth();
        int minListHeight = LIST_ITEM_HEIGHT * 3;
        int maxListHeight = 260;
        int verticalBudget = this.height - PADDING * 2 - headerLayout.getHeight() - footerLayout.getHeight() - COMPONENT_GAP * 2;
        int listHeight = Math.max(minListHeight, Math.min(maxListHeight, verticalBudget));

        int blockHeight = headerLayout.getHeight() + COMPONENT_GAP + listHeight + COMPONENT_GAP + footerLayout.getHeight();
        int top = Math.max(PADDING, Math.min(getMiddle().y() - blockHeight / 2, this.height - PADDING - blockHeight));

        int listY = top + headerLayout.getHeight() + COMPONENT_GAP;
        int footerY = listY + listHeight + COMPONENT_GAP;

        headerLayout.setY(top);
        footerLayout.setY(footerY);

        int listX = getMiddle().x() - listWidth / 2;
        list.setDimensionsAndPosition(listWidth, listHeight, listX, listY);
    }

    private int computeSearchFieldWidth() {
        int maxWidth = this.width - PADDING * 2;
        return Math.max(160, Math.min(MAX_LIST_WIDTH, maxWidth));
    }

    private int computeListWidth() {
        return Math.min(MAX_LIST_WIDTH, this.width - PADDING * 2);
    }

    @Override
    public void tick() {
        super.tick();
        if (refreshLabelTicks > 0) {
            refreshLabelTicks--;
            if (refreshLabelTicks == 0 && refreshButton != null) {
                refreshButton.setMessage(Text.translatable("screen.mqs.scripts.refresh"));
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            boolean reverse = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
            cycleFocus(reverse);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void cycleFocus(boolean reverse) {
        List<Element> order = new ArrayList<>();
        if (searchField != null) {
            order.add(searchField);
        }
        if (scriptList != null) {
            order.add(scriptList);
        }
        if (openFolderButton != null) {
            order.add(openFolderButton);
        }
        if (refreshButton != null) {
            order.add(refreshButton);
        }
        if (optionsButton != null) {
            order.add(optionsButton);
        }
        if (doneButton != null) {
            order.add(doneButton);
        }

        if (order.isEmpty()) {
            return;
        }

        Element current = this.getFocused();
        int idx = order.indexOf(current);
        if (idx == -1) {
            idx = reverse ? order.size() - 1 : 0;
        } else {
            idx = reverse ? (idx - 1 + order.size()) % order.size() : (idx + 1) % order.size();
        }
        Element target = order.get(idx);
        this.setFocused(target);
        if (target instanceof ScriptListWidget list) {
            if (list.getSelectedOrNull() == null && !list.children().isEmpty()) {
                list.setSelected(list.children().getFirst());
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        renderTitle(context);
    }

    private void renderTitle(DrawContext context) {
        if (headerLayout == null) {
            return;
        }
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(getMiddle().x(), headerLayout.getY() - 35, 0);
        matrices.scale(TITLE_SCALE, TITLE_SCALE, 1.0f);
        int textWidth = this.textRenderer.getWidth(TITLE_TEXT);
        context.drawTextWithShadow(this.textRenderer, TITLE_TEXT, -textWidth / 2, 0, 0xFFFFFF);
        matrices.pop();
    }

    private void openOptions() {
        new OptionsScreen(this, Main.getInstance().getGlobalConfigManager()).open();
    }
}

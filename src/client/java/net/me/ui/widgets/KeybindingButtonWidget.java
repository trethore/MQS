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
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class KeybindingButtonWidget extends ButtonWidget {

    private final IntConsumer rebindAction;
    private final Supplier<String> labelSupplier;
    @Nullable
    private final Runnable onRebound;
    private boolean listening;

    public KeybindingButtonWidget(int x, int y, int width, int height, KeyBinding binding, KeybindManager keybindManager) {
        this(x, y, width, height, binding, keybindManager, null);
    }

    public KeybindingButtonWidget(int x, int y, int width, int height, KeyBinding binding, KeybindManager keybindManager, @Nullable Runnable onRebound) {
        this(x, y, width, height, binding.getKeyName(), binding::getKeyName, code -> keybindManager.rebindKey(binding, code), onRebound);
    }

    public KeybindingButtonWidget(int x, int y, int width, int height, HostKeyBinding binding, KeybindManager keybindManager, @Nullable Runnable onRebound) {
        this(x, y, width, height, binding.getKeyName(), binding::getKeyName, code -> keybindManager.rebindHostKey(binding, code), onRebound);
    }

    private KeybindingButtonWidget(int x, int y, int width, int height, String initialLabel, Supplier<String> labelSupplier, IntConsumer rebindAction, @Nullable Runnable onRebound) {
        super(x, y, width, height, Text.literal(initialLabel), button -> {
        }, DEFAULT_NARRATION_SUPPLIER);
        this.rebindAction = rebindAction;
        this.labelSupplier = labelSupplier;
        this.onRebound = onRebound;
    }

    @Override
    public void onPress() {
        startListening();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.active || !this.visible) {
            return false;
        }

        if (listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                stopListening();
                return true;
            }
            applyBinding(keyCode);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listening) {
            applyBinding(button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void startListening() {
        listening = true;
        this.setMessage(Text.literal("[...]"));
    }

    private void stopListening() {
        listening = false;
        this.setMessage(Text.literal(labelSupplier.get()));
    }

    private void applyBinding(int newKeyCode) {
        rebindAction.accept(newKeyCode);
        stopListening();
        if (onRebound != null) {
            onRebound.run();
        }
    }
}

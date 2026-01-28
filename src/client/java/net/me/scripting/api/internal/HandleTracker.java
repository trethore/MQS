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

package net.me.scripting.api.internal;

import net.me.scripting.module.RunningScript;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class HandleTracker<H> {
    private final Map<RunningScript, Set<H>> handlesByScript = new ConcurrentHashMap<>();

    public void track(RunningScript owner, H handle) {
        handlesByScript
                .computeIfAbsent(owner, _ -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(handle);
    }

    public void dispose(RunningScript owner, Predicate<H> matcher, Consumer<H> onDispose) {
        Set<H> owned = handlesByScript.get(owner);
        if (owned == null || owned.isEmpty()) {
            return;
        }
        List<H> snapshot = List.copyOf(owned);
        for (H handle : snapshot) {
            if (matcher.test(handle)) {
                onDispose.accept(handle);
                remove(owner, handle);
            }
        }
    }

    public void disposeAll(RunningScript owner, Consumer<H> onDispose) {
        dispose(owner, _ -> true, onDispose);
    }

    public void remove(RunningScript owner, H handle) {
        Set<H> owned = handlesByScript.get(owner);
        if (owned == null) {
            return;
        }
        owned.remove(handle);
        if (owned.isEmpty()) {
            handlesByScript.remove(owner);
        }
    }

    public boolean hasHandles(RunningScript owner) {
        Set<H> owned = handlesByScript.get(owner);
        return owned != null && !owned.isEmpty();
    }
}

/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Réthoré
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

import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ScriptContextHelper {
    private final ScriptManager scriptManager;

    public ScriptContextHelper(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    public RunningScript require(String apiName) {
        RunningScript script = scriptManager.getCurrentScript();
        if (script == null) {
            throw new IllegalStateException(apiName + " can only be used within a running script context.");
        }
        return script;
    }

    public void executeWithScript(RunningScript owner, Runnable action) {
        RunningScript previous = scriptManager.getCurrentScript();
        scriptManager.setCurrentScript(owner);
        try {
            action.run();
        } finally {
            if (previous != null) {
                scriptManager.setCurrentScript(previous);
            } else {
                scriptManager.clearCurrentScript();
            }
        }
    }

    public Value createDisposer(RunningScript owner, Runnable onDispose) {
        ProxyExecutable exec = ignored -> {
            onDispose.run();
            return null;
        };
        return owner.getContext().asValue(exec);
    }

    public Value createIdempotentDisposer(RunningScript owner, Runnable onDispose) {
        AtomicBoolean disposed = new AtomicBoolean(false);
        ProxyExecutable exec = ignored -> {
            if (disposed.compareAndSet(false, true)) {
                onDispose.run();
            }
            return null;
        };
        return owner.getContext().asValue(exec);
    }
}

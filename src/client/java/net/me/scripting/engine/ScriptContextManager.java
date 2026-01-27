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

package net.me.scripting.engine;

import lombok.Getter;
import net.me.Main;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScriptContextManager {

    private final Queue<Context> contextPool = new ConcurrentLinkedQueue<>();
    private final ScriptContextFactory contextFactory;
    @Getter
    private final ThreadLocal<Map<String, Value>> perFileExports = new ThreadLocal<>();

    private ScriptContextManager(ScriptContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    public static ScriptContextManager create(ScriptContextFactory contextFactory) {
        ScriptContextManager manager = new ScriptContextManager(contextFactory);
        manager.prewarmContextPool();
        return manager;
    }

    public Context getContext() {
        Context context = contextPool.poll();
        if (context == null) {
            Main.LOGGER.warn("Script context pool is empty. Creating a new on-demand context.");
            return contextFactory.createContext(perFileExports);
        }
        return context;
    }

    public void returnContext(Context context) {
        try {
            if (context != null) {
                contextFactory.resetContext(context);
                contextPool.offer(context);
            }
        } finally {
            perFileExports.remove();
        }
    }

    private void prewarmContextPool() {
        Main.LOGGER.info("Pre-warming script context pool...");
        Context context = contextFactory.createContext(perFileExports);
        contextPool.offer(context);
        Main.LOGGER.info("Context pool pre-warmed successfully.");
    }
}

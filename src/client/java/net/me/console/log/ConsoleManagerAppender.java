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

package net.me.console.log;

import net.me.console.ConsoleManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(name = "ConsoleManagerAppender", category = "Core", elementType = "appender", printObject = true)
public class ConsoleManagerAppender extends AbstractAppender {
    private final ConsoleManager consoleManager;

    private ConsoleManagerAppender(ConsoleManager consoleManager) {
        super("ConsoleManagerAppender", null, PatternLayout.createDefaultLayout(), true, null);
        this.consoleManager = consoleManager;
    }

    @PluginFactory
    public static ConsoleManagerAppender createAppender(ConsoleManager consoleManager) {
        return new ConsoleManagerAppender(consoleManager);
    }

    @Override
    public void append(LogEvent event) {
        String message = String.format("[%s/%s] %s",
                event.getThreadName(),
                event.getLoggerName().substring(event.getLoggerName().lastIndexOf('.') + 1),
                event.getMessage().getFormattedMessage()
        );

        Throwable thrown = event.getThrown();
        if (thrown != null) {
            message += ": " + thrown.getMessage();
        }

        if (event.getLevel().isMoreSpecificThan(Level.WARN)) {
            consoleManager.logError(message);
            if (thrown != null) {
                for (StackTraceElement ste : thrown.getStackTrace()) {
                    consoleManager.logError("  at " + ste.toString());
                }
            }
        } else {
            consoleManager.logInfo(message);
        }
    }
}
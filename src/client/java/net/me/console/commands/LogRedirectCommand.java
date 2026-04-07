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

package net.me.console.commands;

import net.me.config.GlobalConfigManager;
import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;

import java.util.Optional;

public class LogRedirectCommand extends ConsoleCommand {
    private final GlobalConfigManager globalConfigManager;

    public LogRedirectCommand(ConsoleManager consoleManager, GlobalConfigManager globalConfigManager) {
        super(consoleManager, "logredirect", "Redirects System.out, System.err, and SLF4J logs to this console.", "logredirect <true|false>");
        this.globalConfigManager = globalConfigManager;
    }

    @Override
    public void execute(String[] args) {
        ConsoleManager cm = getConsoleManager();
        Optional<Boolean> enableOpt = parseRequiredBooleanArg(args);
        if (enableOpt.isEmpty()) {
            return;
        }

        boolean enable = enableOpt.orElseThrow();

        if (globalConfigManager.isLogRedirectEnabled() == enable) {
            cm.logInfo("Log redirection is already " + (enable ? "enabled" : "disabled") + ".");
            return;
        }

        globalConfigManager.setLogRedirectEnabled(enable);
        cm.logSuccess("Log redirection " + (enable ? "enabled" : "disabled") + ".");
    }
}

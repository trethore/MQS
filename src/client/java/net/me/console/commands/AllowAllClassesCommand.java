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
import net.me.console.ConsoleUtils;

import java.util.Optional;

public class AllowAllClassesCommand extends ConsoleCommand {
    private final GlobalConfigManager globalConfigManager;

    public AllowAllClassesCommand(ConsoleManager consoleManager, GlobalConfigManager globalConfigManager) {
        super(consoleManager, "allowallclasses", "Allows scripts to access all Java classes. VERY DANGEROUS.", "allowallclasses <true|false>");
        this.globalConfigManager = globalConfigManager;
    }

    @Override
    public void execute(String[] args) {
        ConsoleManager cm = getConsoleManager();
        if (args.length != 1) {
            cm.logError("Invalid arguments. Usage: " + getUsage());
            return;
        }

        Optional<Boolean> enableOpt = ConsoleUtils.parseBooleanArg(args[0]);

        if (enableOpt.isEmpty()) {
            cm.logError("Invalid argument '" + args[0] + "'. Must be '" + ConsoleUtils.TRUE_STRING + "' or '" + ConsoleUtils.FALSE_STRING + "'.");
            return;
        }

        boolean enable = enableOpt.get();
        globalConfigManager.setAllClassesAllowed(enable);
        cm.logSuccess("Allowing all classes set to " + enable + ". A script reload is required for this to take full effect.");
        if (enable) {
            cm.logError("WARNING: This is a dangerous setting. Only use scripts from trusted sources.");
        }
    }
}
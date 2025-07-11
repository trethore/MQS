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

package net.me.console.commands;

import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;
import net.me.scripting.ScriptingService;

public class SaveAllConfigsCommand extends ConsoleCommand {
    private final ScriptingService scriptingService;

    public SaveAllConfigsCommand(ConsoleManager consoleManager, ScriptingService scriptingService) {
        super(consoleManager, "saveconfigs", "Saves the configurations for all currently running scripts.", "saveconfigs");
        this.scriptingService = scriptingService;
    }

    @Override
    public void execute(String[] args) {
        ConsoleManager cm = getConsoleManager();
        int count = scriptingService.saveAll();

        if (count > 0) {
            cm.logSuccess("Successfully saved all " + count + " loaded script configurations.");
        } else {
            cm.logInfo("No script configs loaded in memory, nothing to save.");
        }
    }
}
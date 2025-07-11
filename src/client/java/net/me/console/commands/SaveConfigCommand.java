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

public class SaveConfigCommand extends ConsoleCommand {

    private final ScriptingService scriptingService;

    public SaveConfigCommand(ConsoleManager consoleManager, ScriptingService scriptingService) {
        super(consoleManager, "saveconfig", "Saves the configuration for a specific running script.", "saveconfig <script_id>");
        this.scriptingService = scriptingService;
    }

    @Override
    public void execute(String[] args) {
        ConsoleManager cm = getConsoleManager();

        if (args.length == 0) {
            cm.logError("No script ID provided. Usage: " + getUsage());
            return;
        }

        String scriptId = String.join(" ", args);
        boolean success = scriptingService.save(scriptId);

        if (success) {
            cm.logSuccess("Successfully saved config for script '" + scriptId + "'.");
        } else {
            cm.logError("Failed to save config. Script '" + scriptId + "' is not running or does not exist.");
        }
    }
}
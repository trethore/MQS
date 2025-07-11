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

public class ScriptCommands {

    public static class ListScriptsCommand extends ConsoleCommand {
        private final ScriptingService scriptingService;

        public ListScriptsCommand(ConsoleManager consoleManager, ScriptingService scriptingService) {
            super(consoleManager, "list", "Lists all available scripts and their status.", "list");
            this.scriptingService = scriptingService;
        }

        @Override
        public void execute(String[] args) {
            ConsoleManager cm = getConsoleManager();
            cm.logInfo("--- Available Scripts ---");
            String scriptList = scriptingService.getFormattedScriptList();
            for (String line : scriptList.split("\n")) {
                cm.logInfo(line.replace("§a", "").replace("§c", "").replace("§r", "")); // Remove color codes for console
            }
        }
    }

    public static class EnableScriptCommand extends ConsoleCommand {
        private final ScriptingService scriptingService;

        public EnableScriptCommand(ConsoleManager consoleManager, ScriptingService scriptingService) {
            super(consoleManager, "enable", "Enables a script by its ID.", "enable <script_id>");
            this.scriptingService = scriptingService;
        }

        @Override
        public void execute(String[] args) {
            if (args.length == 0) {
                getConsoleManager().logError("No script ID provided. Usage: " + getUsage());
                return;
            }
            String scriptId = String.join(" ", args);
            scriptingService.enable(scriptId);
        }
    }

    public static class DisableScriptCommand extends ConsoleCommand {
        private final ScriptingService scriptingService;

        public DisableScriptCommand(ConsoleManager consoleManager, ScriptingService scriptingService) {
            super(consoleManager, "disable", "Disables a running script by its ID.", "disable <script_id>");
            this.scriptingService = scriptingService;
        }

        @Override
        public void execute(String[] args) {
            if (args.length == 0) {
                getConsoleManager().logError("No script ID provided. Usage: " + getUsage());
                return;
            }
            String scriptId = String.join(" ", args);
            scriptingService.disable(scriptId);
        }
    }

    public static class RefreshScriptsCommand extends ConsoleCommand {
        private final ScriptingService scriptingService;

        public RefreshScriptsCommand(ConsoleManager consoleManager, ScriptingService scriptingService) {
            super(consoleManager, "refresh", "Refreshes and reloads all scripts from disk, disabling all running scripts.", "refresh");
            this.scriptingService = scriptingService;
        }

        @Override
        public void execute(String[] args) {
            getConsoleManager().logInfo("Refreshing scripts...");
            scriptingService.refresh();
            getConsoleManager().logSuccess("Scripts refreshed. All scripts are now disabled.");
        }
    }

    public static class RefreshAndReenableCommand extends ConsoleCommand {
        private final ScriptingService scriptingService;

        public RefreshAndReenableCommand(ConsoleManager consoleManager, ScriptingService scriptingService) {
            super(consoleManager, "refreshandreenable", "Refreshes and reloads all scripts, then re-enables previously running scripts.", "refreshandreenable");
            this.scriptingService = scriptingService;
        }

        @Override
        public void execute(String[] args) {
            getConsoleManager().logInfo("Refreshing scripts and re-enabling...");
            scriptingService.refreshAndReenable();
            getConsoleManager().logSuccess("Scripts refreshed and previously running scripts re-enabled.");
        }
    }

    public static class DisableAllCommand extends ConsoleCommand {
        private final ScriptingService scriptingService;

        public DisableAllCommand(ConsoleManager consoleManager, ScriptingService scriptingService) {
            super(consoleManager, "disableall", "Disables all currently running scripts.", "disableall");
            this.scriptingService = scriptingService;
        }

        @Override
        public void execute(String[] args) {
            int disabledCount = scriptingService.disableAll();

            if (disabledCount == 0) {
                getConsoleManager().logInfo("No scripts are currently running.");
            } else {
                getConsoleManager().logSuccess("Disabled all " + disabledCount + " running scripts.");
            }
        }
    }
}
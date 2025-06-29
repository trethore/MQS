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
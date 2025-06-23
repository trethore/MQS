package net.me.console.commands;

import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;
import net.me.scripting.ScriptingService;

public class SaveConfigCommand implements ConsoleCommand {

    private final ScriptingService scriptingService = ScriptingService.getInstance();

    @Override
    public void execute(String[] args) {
        ConsoleManager cm = ConsoleManager.getInstance();

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

    @Override
    public String getName() {
        return "saveconfig";
    }

    @Override
    public String getDescription() {
        return "Saves the configuration for a specific running script.";
    }

    @Override
    public String getUsage() {
        return "saveconfig <script_id>";
    }
}
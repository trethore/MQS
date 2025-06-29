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
            cm.logSuccess("Successfully saved configs for all " + count + " running scripts.");
        } else {
            cm.logInfo("No running scripts found, nothing to save.");
        }
    }
}
package net.me.console.commands;

import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;
import net.me.scripting.ScriptingService;

public class SaveAllConfigsCommand implements ConsoleCommand {
    private final ScriptingService scriptingService = ScriptingService.getInstance();

    @Override
    public void execute(String[] args) {
        ConsoleManager cm = ConsoleManager.getInstance();
        int count = scriptingService.saveAll();

        if (count > 0) {
            cm.logSuccess("Successfully saved configs for all " + count + " running scripts.");
        } else {
            cm.logInfo("No running scripts found, nothing to save.");
        }
    }

    @Override
    public String getName() {
        return "saveconfigs";
    }

    @Override
    public String getDescription() {
        return "Saves the configurations for all currently running scripts.";
    }

    @Override
    public String getUsage() {
        return "saveconfigs";
    }
}
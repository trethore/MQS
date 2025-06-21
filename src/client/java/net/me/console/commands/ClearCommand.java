package net.me.console.commands;

import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;

public class ClearCommand implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        ConsoleManager.getInstance().clear();
        ConsoleManager.getInstance().logSuccess("Console cleared.");
    }

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "Clears the console screen.";
    }

    @Override
    public String getUsage() {
        return "clear";
    }
}
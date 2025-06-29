package net.me.console.commands;

import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;

public class ClearCommand extends ConsoleCommand {

    public ClearCommand(ConsoleManager consoleManager) {
        super(consoleManager, "clear", "Clears the console screen.", "clear");
    }

    @Override
    public void execute(String[] args) {
        getConsoleManager().clear();
        getConsoleManager().logSuccess("Console cleared.");
    }
}
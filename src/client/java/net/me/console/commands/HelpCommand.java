package net.me.console.commands;

import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;

import java.util.Comparator;

public class HelpCommand extends ConsoleCommand {

    public HelpCommand(ConsoleManager consoleManager) {
        super(consoleManager, "help", "Shows this help message.", "help");
    }

    @Override
    public void execute(String[] args) {
        final ConsoleManager cm = getConsoleManager();
        cm.logInfo("--- Available Commands ---");
        cm.getCommands().values().stream()
                .sorted(Comparator.comparing(ConsoleCommand::getName))
                .forEach(cmd ->
                        cm.logInfo(String.format("%-15s - %s (Usage: %s)", cmd.getName(), cmd.getDescription(), cmd.getUsage()))
                );
    }
}
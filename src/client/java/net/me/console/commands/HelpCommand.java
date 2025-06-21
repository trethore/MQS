package net.me.console.commands;

import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;

import java.util.Comparator;

public class HelpCommand implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        ConsoleManager cm = ConsoleManager.getInstance();
        cm.logInfo("--- Available Commands ---");
        cm.getCommands().values().stream()
                .sorted(Comparator.comparing(ConsoleCommand::getName))
                .forEach(cmd ->
                        cm.logInfo(String.format("%-15s - %s (Usage: %s)", cmd.getName(), cmd.getDescription(), cmd.getUsage()))
                );
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Shows this help message.";
    }

    @Override
    public String getUsage() {
        return "help";
    }
}